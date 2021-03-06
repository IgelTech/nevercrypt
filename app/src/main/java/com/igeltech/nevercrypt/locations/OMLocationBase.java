package com.igeltech.nevercrypt.locations;

import android.net.Uri;

import com.igeltech.nevercrypt.android.helpers.ProgressReporter;
import com.igeltech.nevercrypt.android.providers.MainContentProvider;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.std.StdFs;
import com.igeltech.nevercrypt.settings.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class OMLocationBase extends LocationBase implements OMLocation, Cloneable
{
    protected ProgressReporter _openingProgressReporter;

    protected OMLocationBase(OMLocationBase sibling)
    {
        super(sibling);
    }

    protected OMLocationBase(Settings settings, SharedData sharedData)
    {
        super(settings, sharedData);
    }

    @Override
    public synchronized void close(boolean force) throws IOException
    {
        closeFileSystem(force);
        SecureBuffer p = getPassword();
        if (p != null)
        {
            p.close();
            getSharedData().password = null;
        }
    }

    @Override
    public boolean hasPassword()
    {
        return false;
    }

    @Override
    public boolean hasCustomKDFIterations()
    {
        return false;
    }

    @Override
    public boolean requirePassword()
    {
        return hasPassword() && !getExternalSettings().hasPassword();
    }

    @Override
    public boolean requireCustomKDFIterations()
    {
        return hasCustomKDFIterations() && getExternalSettings().getCustomKDFIterations() < 0;
    }

    @Override
    public boolean isOpenOrMounted()
    {
        return isOpen();
    }

    @Override
    public ExternalSettings getExternalSettings()
    {
        return (ExternalSettings) super.getExternalSettings();
    }

    @Override
    public void setOpeningProgressReporter(ProgressReporter pr)
    {
        _openingProgressReporter = pr;
    }

    @Override
    public boolean isReadOnly()
    {
        return getSharedData().isReadOnly;
    }

    @Override
    public void setOpenReadOnly(boolean readOnly)
    {
        getSharedData().isReadOnly = readOnly;
    }

    @Override
    protected SharedData getSharedData()
    {
        return (SharedData) super.getSharedData();
    }

    protected SecureBuffer getPassword()
    {
        return getSharedData().password;
    }

    @Override
    public synchronized void setPassword(SecureBuffer password)
    {
        SecureBuffer p = getPassword();
        if (p != null && p != password)
            p.close();
        getSharedData().password = password;
    }

    protected int getNumKDFIterations()
    {
        return getSharedData().numKDFIterations;
    }

    @Override
    public void setNumKDFIterations(int num)
    {
        getSharedData().numKDFIterations = num;
    }

    @Override
    public Uri getDeviceAccessibleUri(Path path)
    {
        return !_globalSettings.dontUseContentProvider() ? MainContentProvider.getContentUriFromLocation(this, path) : null;
    }

    @Override
    protected ArrayList<Path> loadPaths(Collection<String> paths) throws IOException
    {
        ArrayList<Path> res = new ArrayList<>();
        for (String path : paths)
            res.add(StdFs.getStdFs().getPath(path));
        return res;
    }

    protected byte[] getSelectedPassword()
    {
        SecureBuffer p = getPassword();
        if (p != null)
        {
            byte[] pb = p.getDataArray();
            if (pb != null && pb.length > 0)
                return pb;
        }
        byte[] res = getExternalSettings().getPassword();
        if (res == null)
            res = new byte[0];
        return res;
    }

    protected int getSelectedKDFIterations()
    {
        int n = getNumKDFIterations();
        return n == 0 ? getExternalSettings().getCustomKDFIterations() : n;
    }

    protected byte[] getFinalPassword() throws IOException
    {
        return getSelectedPassword();
    }

    public static class ExternalSettings extends LocationBase.ExternalSettings implements OMLocation.ExternalSettings
    {
        private static final String SETTINGS_PASS = "pass";
        private static final String SETTINGS_CUSTOM_KDF_ITERATIONS = "custom_kdf_iterations";
        private String _pass;
        private int _customKDFIterations;

        public ExternalSettings()
        {
        }

        @Override
        public byte[] getPassword()
        {
            return _pass == null ? null : decodeAndDecrypt(_pass);
        }

        @Override
        public void setPassword(byte[] password)
        {
            _pass = password == null ? null : encryptAndEncode(password);
        }

        @Override
        public boolean hasPassword()
        {
            return _pass != null && _pass.length() > 0;
        }

        @Override
        public int getCustomKDFIterations()
        {
            return _customKDFIterations;
        }

        @Override
        public void setCustomKDFIterations(int val)
        {
            _customKDFIterations = val;
        }

        @Override
        public void saveToJSONObject(JSONObject jo) throws JSONException
        {
            super.saveToJSONObject(jo);
            if (_pass != null)
                jo.put(SETTINGS_PASS, _pass);
            if (_customKDFIterations >= 0)
                storeProtectedField(jo, SETTINGS_CUSTOM_KDF_ITERATIONS, String.valueOf(_customKDFIterations));
            else
                jo.remove(SETTINGS_CUSTOM_KDF_ITERATIONS);
        }

        @Override
        public void loadFromJSONOjbect(JSONObject jo) throws JSONException
        {
            super.loadFromJSONOjbect(jo);
            _pass = jo.optString(SETTINGS_PASS, null);
            String iters = loadProtectedString(jo, SETTINGS_CUSTOM_KDF_ITERATIONS);
            if (iters != null)
                _customKDFIterations = Integer.valueOf(iters);
            else
                _customKDFIterations = -1;
        }
    }

    protected static class SharedData extends LocationBase.SharedData
    {
        SecureBuffer password;
        int numKDFIterations;
        boolean isReadOnly;

        protected SharedData(String id)
        {
            super(id);
        }
    }
}
