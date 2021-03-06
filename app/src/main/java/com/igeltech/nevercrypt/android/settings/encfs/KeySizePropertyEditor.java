package com.igeltech.nevercrypt.android.settings.encfs;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateLocationFragment;
import com.igeltech.nevercrypt.android.locations.tasks.CreateEncFsTaskFragment;
import com.igeltech.nevercrypt.android.settings.ChoiceDialogPropertyEditor;

import java.util.ArrayList;
import java.util.List;

public class KeySizePropertyEditor extends ChoiceDialogPropertyEditor
{
    public KeySizePropertyEditor(CreateLocationFragment hostFragment)
    {
        super(hostFragment, R.string.key_size, R.string.key_size_descr, hostFragment.getTag());
    }

    @Override
    protected int loadValue()
    {
        return (getHostFragment().getState().getInt(CreateEncFsTaskFragment.ARG_KEY_SIZE, 16) * 8 - 128) / 64;
    }

    @Override
    protected void saveValue(int value)
    {
        getHostFragment().getState().putInt(CreateEncFsTaskFragment.ARG_KEY_SIZE, (128 + value * 64) / 8);
    }

    @Override
    protected List<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        for (int i = 128; i <= 256; i += 64)
            res.add(String.valueOf(i));
        return res;
    }

    protected CreateLocationFragment getHostFragment()
    {
        return (CreateLocationFragment) getHost();
    }
}
