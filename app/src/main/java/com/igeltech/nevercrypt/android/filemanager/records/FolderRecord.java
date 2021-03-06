package com.igeltech.nevercrypt.android.filemanager.records;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;

import java.io.IOException;

public class FolderRecord extends FsBrowserRecord
{
    private static Drawable _folderIcon;

    public FolderRecord(Context context) throws IOException
    {
        super(context);
    }

    private static synchronized Drawable getFolderIcon(Context context)
    {
        if (_folderIcon == null && context != null)
        {
            _folderIcon = context.getDrawable(R.drawable.ic_folder);
        }
        return _folderIcon;
    }

    @Override
    public int getViewType()
    {
        return 1;
    }

    @Override
    public View createView(int position, ViewGroup parent)
    {
        if (!_host.isAdded())
            Logger.debug("FolderRecord createView - host not attached");
        if (_host == null || !_host.isAdded())
            return null;
        LayoutInflater inflater = _host.getLayoutInflater();
        View v = inflater.inflate(R.layout.fs_browser_folder_row, parent, false);
        ((ViewGroup) v).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        updateView(v, position);
        return v;
    }

    @Override
    public boolean allowSelect()
    {
        return _host.allowFolderSelect();
    }

    @Override
    public boolean open() throws Exception
    {
        if (_path != null)
            _host.goTo(_path);
        return true;
    }

    @Override
    public boolean openInplace() throws Exception
    {
        _host.showProperties(null, true);
        return open();
    }

    @Override
    protected Drawable getDefaultIcon()
    {
        return getFolderIcon(_host.getContext());
    }
}