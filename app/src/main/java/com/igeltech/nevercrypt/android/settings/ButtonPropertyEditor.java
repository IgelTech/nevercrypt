package com.igeltech.nevercrypt.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatButton;

import com.igeltech.nevercrypt.android.R;

public abstract class ButtonPropertyEditor extends PropertyEditorBase
{
    private final int _buttonTextId;
    private final String _buttonText;
    protected AppCompatButton _button;

    protected ButtonPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId, int buttonTextId)
    {
        super(host, R.layout.settings_button_editor, titleResId, descResId);
        _buttonTextId = buttonTextId;
        _buttonText = null;
    }

    protected ButtonPropertyEditor(PropertyEditor.Host host, int propertyId, String title, String desc, String buttonText)
    {
        super(host, propertyId, R.layout.settings_button_editor, title, desc);
        _buttonTextId = 0;
        _buttonText = buttonText;
    }

    @Override
    public View createView(ViewGroup parent)
    {
        View view = super.createView(parent);
        _button = view.findViewById(android.R.id.button1);
        if (_buttonTextId != 0)
            _button.setText(_buttonTextId);
        else if (_buttonText != null)
            _button.setText(_buttonText);
        _button.setOnClickListener(v -> onButtonClick());
        return view;
    }

    @Override
    public void save(Bundle b)
    {
    }

    @Override
    public void save()
    {
    }

    @Override
    public void load(Bundle b)
    {
        load();
    }

    @Override
    public void load()
    {
    }

    protected void onButtonClick()
    {
        if (_host.getPropertiesView().isInstantSave())
            save();
    }
}
