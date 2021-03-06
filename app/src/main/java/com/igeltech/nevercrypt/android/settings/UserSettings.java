package com.igeltech.nevercrypt.android.settings;

import android.content.Context;

import com.igeltech.nevercrypt.settings.DefaultSettings;
import com.igeltech.nevercrypt.settings.Settings;

public class UserSettings extends UserSettingsCommon implements Settings
{
    private static final Settings _defaultSettings = new DefaultSettings();
    private static UserSettings _instance;

    public UserSettings(Context context)
    {
        super(context, _defaultSettings);
    }

    public static synchronized UserSettings getSettings(Context context)
    {
        if (_instance == null)
            _instance = new UserSettings(context);
        return _instance;
    }

    public synchronized static void closeSettings()
    {
        if (_instance != null)
            _instance.clearSettingsProtectionKey();
        _instance = null;
    }
}
