package com.igeltech.nevercrypt.android.filemanager.tasks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.filemanager.fragments.FileListViewFragment;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.providers.MainContentProvider;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.settings.GlobalConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CopyToClipboardTask extends TaskFragment
{
    public static final String TAG = "CopyToClipboardTask";
    protected Context _context;

    public static CopyToClipboardTask newInstance(Location loc, Collection<? extends Path> paths)
    {
        Bundle args = new Bundle();
        LocationsManager.storePathsInBundle(args, loc, paths);
        CopyToClipboardTask f = new CopyToClipboardTask();
        f.setArguments(args);
        return f;
    }

    public static ClipData makeClipData(Context context, Location location, Iterable<Path> paths)
    {
        Iterator<Path> pi = paths.iterator();
        if (!pi.hasNext())
            return null;
        Path path = pi.next();
        ContentResolver cr = context.getContentResolver();
        ClipData clip = ClipData.newUri(cr, path.getPathString(), MainContentProvider.getContentUriFromLocation(location, path));
        while (pi.hasNext())
        {
            path = pi.next();
            clip.addItem(new ClipData.Item(MainContentProvider.getContentUriFromLocation(location, path)));
        }
        return clip;
    }

    @Override
    public void initTask(FragmentActivity activity)
    {
        _context = activity.getApplicationContext();
    }

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        if (GlobalConfig.isDebug())
            Logger.debug("CopyToClipboardTask args: " + getArguments());
        ArrayList<Path> paths = new ArrayList<>();
        Location location = LocationsManager.
                getLocationsManager(_context).
                getFromBundle(getArguments(), paths);
        ClipData clip = makeClipData(_context, location, paths);
        if (clip == null)
        {
            Logger.debug("CopyToClipboardTask: no paths");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null)
            clipboard.setPrimaryClip(clip);
        if (GlobalConfig.isDebug())
            Logger.debug("CopyToClipboardTask: clip has been set: " + clip);
    }

    @Override
    protected TaskCallbacks getTaskCallbacks(final FragmentActivity activity)
    {
        return new TaskCallbacks()
        {
            @Override
            public void onUpdateUI(Object state)
            {
            }

            @Override
            public void onPrepare(Bundle args)
            {
            }

            @Override
            public void onResumeUI(Bundle args)
            {
            }

            @Override
            public void onSuspendUI(Bundle args)
            {
            }

            @Override
            public void onCompleted(Bundle args, Result result)
            {
                try
                {
                    result.getResult();
                    FileListViewFragment f = (FileListViewFragment) getParentFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
                    if (f != null)
                    {
                        Logger.debug("CopyToClipboard task onCompleted: updating options menu");
                        f.updateOptionsMenu();
                    }
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(activity, e);
                }
            }
        };
    }
}