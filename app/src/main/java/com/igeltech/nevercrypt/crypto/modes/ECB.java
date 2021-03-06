package com.igeltech.nevercrypt.crypto.modes;

import com.igeltech.nevercrypt.crypto.BlockCipher;
import com.igeltech.nevercrypt.crypto.EncryptionEngineException;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;

import java.util.Arrays;

public abstract class ECB implements FileEncryptionEngine
{
    protected final BlockCipher _cipher;
    protected byte[] _key;

    protected ECB(BlockCipher cipher)
    {
        _cipher = cipher;
    }

    @Override
    public synchronized void init() throws EncryptionEngineException
    {
        closeCipher();
        if (_key == null)
            throw new EncryptionEngineException("Encryption key is not set");
        _cipher.init(_key);
    }

    @Override
    public int getFileBlockSize()
    {
        return _cipher.getBlockSize();
    }

    @Override
    public byte[] getIV()
    {
        return null;
    }

    @Override
    public void setIV(byte[] iv)
    {
    }

    @Override
    public void setIncrementIV(boolean val)
    {
    }

    @Override
    public int getIVSize()
    {
        return 0;
    }

    @Override
    public int getKeySize()
    {
        return _cipher.getKeySize();
    }

    public void close()
    {
        closeCipher();
        clearAll();
    }

    @Override
    public void encrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        if (len == 0)
            return;
        int blockSize = _cipher.getBlockSize();
        if (len % blockSize != 0 || (offset + len) > data.length)
            throw new EncryptionEngineException("Wrong buffer length");
        int numBlocks = len / blockSize;
        byte[] block = new byte[blockSize];
        for (int i = 0; i < numBlocks; i++)
        {
            int sp = offset + blockSize * i;
            System.arraycopy(data, sp, block, 0, blockSize);
            _cipher.encryptBlock(block);
            System.arraycopy(block, 0, data, sp, blockSize);
        }
    }

    @Override
    public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        if (len == 0)
            return;
        int blockSize = _cipher.getBlockSize();
        if (len % blockSize != 0 || (offset + len) > data.length)
            throw new EncryptionEngineException("Wrong buffer length");
        int numBlocks = len / blockSize;
        byte[] block = new byte[blockSize];
        for (int i = 0; i < numBlocks; i++)
        {
            int sp = offset + blockSize * i;
            System.arraycopy(data, sp, block, 0, blockSize);
            _cipher.decryptBlock(block);
            System.arraycopy(block, 0, data, sp, blockSize);
        }
    }

    @Override
    public byte[] getKey()
    {
        return _key;
    }

    @Override
    public void setKey(byte[] key)
    {
        clearKey();
        _key = key == null ? null : Arrays.copyOf(key, getKeySize());
    }

    @Override
    public String getCipherModeName()
    {
        return "ecb";
    }

    @Override
    public int getEncryptionBlockSize()
    {
        return _cipher.getBlockSize();
    }

    protected void closeCipher()
    {
        _cipher.close();
    }

    private void clearAll()
    {
        clearKey();
    }

    private void clearKey()
    {
        if (_key != null)
        {
            Arrays.fill(_key, (byte) 0);
            _key = null;
        }
    }
}

    