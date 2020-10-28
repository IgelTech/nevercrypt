package com.igeltech.nevercrypt.container;


import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.android.helpers.ContainerOpeningProgressReporter;
import com.igeltech.nevercrypt.crypto.EncryptedFileWithCache;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.crypto.LocalEncryptedFileXTS;
import com.igeltech.nevercrypt.crypto.modes.XTS;
import com.igeltech.nevercrypt.exceptions.ApplicationException;
import com.igeltech.nevercrypt.exceptions.WrongFileFormatException;
import com.igeltech.nevercrypt.fs.File.AccessMode;
import com.igeltech.nevercrypt.fs.FileSystem;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.RandomAccessIO;
import com.igeltech.nevercrypt.fs.exfat.ExFat;
import com.igeltech.nevercrypt.fs.fat.FatFS;
import com.igeltech.nevercrypt.fs.std.StdFs;
import com.igeltech.nevercrypt.fs.std.StdFsPath;

import java.io.Closeable;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ContainerBase implements Closeable
{

	public static ContainerFormatInfo findFormatByName(List<ContainerFormatInfo> supportedFormats, String name)
	{
		if(name != null)
			for(ContainerFormatInfo cfi: supportedFormats)
			{
				if(cfi.getFormatName().equalsIgnoreCase(name))
					return cfi;
			}
		return null;
	}

	public static byte[] cutPassword(byte[] pass, int maxLength)
	{
		if(pass!=null)
		{
			if (maxLength > 0 && pass.length > maxLength)
			{
				byte[] tmp = pass;
				pass = new byte[maxLength];
				System.arraycopy(tmp, 0, pass, 0, maxLength);
			}
			else
				pass = pass.clone();
		}
		return pass;
	}

	public static FileSystem loadFileSystem(RandomAccessIO io, boolean isReadOnly) throws IOException, UserException
	{
		if(ExFat.isExFATImage(io))
		{
			return new ExFat(io, isReadOnly);
		}

		FatFS fs = FatFS.getFat(io);
		if (isReadOnly)
			fs.setReadOnlyMode(true);
		return fs;
	}


	public ContainerBase(Path path, ContainerFormatInfo containerFormat, VolumeLayout layout)
	{			
		_pathToContainer = path;
		_layout = layout;
		_containerFormat = containerFormat;
	}
	
	public static final short COMPATIBLE_TC_VERSION = 0x700;
	
	public synchronized void open(byte[] password) throws IOException, ApplicationException
	{
		Logger.debug("Opening container at " + _pathToContainer.getPathString());
		try (RandomAccessIO t = openFile())
		{
			if (_containerFormat == null)
			{
				if (tryLayout(t, password, false) || tryLayout(t, password, true))
					return;
			} else
			{
				if (tryLayout(_containerFormat, t, password, false) || tryLayout(_containerFormat, t, password, true))
					return;
			}
		}

		throw new WrongFileFormatException();
	}
	
	public FileSystem getEncryptedFS() throws IOException, UserException
	{
		return getEncryptedFS(false);
	}

	public RandomAccessIO initEncryptedFile(boolean isReadOnly) throws IOException
	{
		if(_layout == null)
			throw new IOException("The container is closed");
		EncryptionEngine enc = _layout.getEngine();
		return allowLocalXTS() ?
				new LocalEncryptedFileXTS(_pathToContainer.getPathString(), isReadOnly, _layout.getEncryptedDataOffset(), (XTS)enc)
				:
				new EncryptedFileWithCache(_pathToContainer,isReadOnly ? AccessMode.Read : AccessMode.ReadWrite,_layout);
	}

	public synchronized FileSystem getEncryptedFS(boolean isReadOnly) throws IOException, UserException
	{
		if(_fileSystem == null)
		{
			RandomAccessIO io = getEncryptedFile(isReadOnly);
			_fileSystem = loadFileSystem(io, isReadOnly);
		}
		return _fileSystem;
	}
		
	public synchronized void close() throws IOException
	{
		if(_fileSystem!=null)
		{
			_fileSystem.close(true);
			_fileSystem = null;
		}

		if(_encryptedFile!=null)
		{
			_encryptedFile.close();
			_encryptedFile = null;
		}
		
		if(_layout!=null)		
		{
			_layout.close();
			_layout = null;
		}
	}
	
	public Path getPathToContainer()
	{
		return _pathToContainer;
	}
	
	public VolumeLayout getVolumeLayout()
	{
		return _layout;
	}
	
	public ContainerFormatInfo getContainerFormat()
	{
		return _containerFormat;
	}

	public void setContainerFormat(ContainerFormatInfo containerFormat)
	{
		_containerFormat = containerFormat;
	}

	public void setEncryptionEngineHint(FileEncryptionEngine eng)
	{
		_encryptionEngine = eng;
	}

	public void setHashFuncHint(MessageDigest hf)
	{
		_messageDigest = hf;
	}

	public void setNumKDFIterations(int num)
	{
		_numKDFIterations = num;
	}

	public void setProgressReporter(ContainerOpeningProgressReporter r)
	{
		_progressReporter = r;
	}

	public RandomAccessIO getEncryptedFile(boolean isReadOnly) throws IOException
	{
		if(_encryptedFile == null)
			_encryptedFile = initEncryptedFile(isReadOnly);
		return  _encryptedFile;
	}

	protected FileSystem _fileSystem;
	protected RandomAccessIO _encryptedFile;
	protected int _numKDFIterations;
	protected VolumeLayout _layout;
	protected ContainerFormatInfo _containerFormat;
	protected final Path _pathToContainer;
	protected ContainerOpeningProgressReporter _progressReporter;
	protected FileEncryptionEngine _encryptionEngine;

	protected MessageDigest _messageDigest;

	protected abstract List<ContainerFormatInfo> getFormats();

	protected RandomAccessIO openFile() throws IOException
	{
		return _pathToContainer.getFile().getRandomAccessIO(AccessMode.Read);
	}

	protected boolean tryLayout(RandomAccessIO containerFile, byte[] password, boolean isHidden) throws IOException, ApplicationException
	{
		List<ContainerFormatInfo> cfs = getFormats();
		if(cfs.size()>1)
			Collections.sort(cfs, (lhs, rhs) -> Integer.valueOf(lhs.getOpeningPriority()).compareTo(rhs.getOpeningPriority()));
		
		for(ContainerFormatInfo cf: cfs)
		{
			//Don't try too slow container formats
			if(cf.getOpeningPriority() < 0)			
				continue;
			if(tryLayout(cf, containerFile, password, isHidden))
				return true;
		}
		return false;
	}
	
	protected boolean tryLayout(ContainerFormatInfo cf, RandomAccessIO containerFile, byte[] password, boolean isHidden) throws IOException, ApplicationException
	{
		if(isHidden && !cf.hasHiddenContainerSupport())
			return false;
		Logger.debug(String.format("Trying %s container format%s", cf.getFormatName(), isHidden ? " (hidden)" : ""));
		if(_progressReporter!=null)
		{
			_progressReporter.setContainerFormatName(cf.getFormatName());
			_progressReporter.setIsHidden(isHidden);
		}
		VolumeLayout vl = isHidden ? cf.getHiddenVolumeLayout() : cf.getVolumeLayout();
		vl.setOpeningProgressReporter(_progressReporter);
		if(_encryptionEngine!=null)
			vl.setEngine(_encryptionEngine);
		if(_messageDigest!=null)
			vl.setHashFunc(_messageDigest);
		
		vl.setPassword(cutPassword(password, cf.getMaxPasswordLength()));
		if(cf.hasCustomKDFIterationsSupport() && _numKDFIterations > 0)
			vl.setNumKDFIterations(_numKDFIterations);
		if(vl.readHeader(containerFile))
		{			
			_containerFormat = cf;
			_layout = vl;			
			return true;
		}
		else if(isHidden && (_encryptionEngine!=null || _messageDigest!=null))
		{
			vl.setEngine(null);
			vl.setHashFunc(null);
			if(vl.readHeader(containerFile))
			{
				_containerFormat = cf;
				_layout = vl;
				return true;
			}
		}
		vl.close();		
		return false;
	}
	
	protected Iterable<VolumeLayout> getLayouts(boolean isHidden)
	{
		List<VolumeLayout> vll = new ArrayList<>();
		for(ContainerFormatInfo cf: getFormats())
		{
			VolumeLayout vl = isHidden ? cf.getHiddenVolumeLayout() : cf.getVolumeLayout();
			if(vl!=null)
				vll.add(vl);
		}
		return vll;
	}
	
	protected boolean allowLocalXTS()
	{
		return _pathToContainer instanceof StdFsPath
				&& _layout.getEngine() instanceof XTS 
				&& _pathToContainer.getFileSystem() instanceof StdFs
				&& ((StdFs)_pathToContainer.getFileSystem()).getRootDir().isEmpty();
	}

}



