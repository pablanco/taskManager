package com.genexus.live_editing.ui.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.genexus.live_editing.R;
import com.squareup.okhttp.HttpUrl;

public class EndpointPreference extends DialogPreference implements View.OnClickListener {
    private EditText mIpEdit;
    private EditText mPortEdit;
    private HttpUrl mCurrentValue;

    public EndpointPreference(Context context) {
        this(context, null);
    }

    public EndpointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.endpoint_pref_dialog);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog) getDialog();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (validateInputs()) {
            getDialog().dismiss();
            onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        mIpEdit = (EditText) view.findViewById(R.id.ip_edit);
        mPortEdit = (EditText) view.findViewById(R.id.port_edit);
        if (mCurrentValue != null) {
            mIpEdit.setText(mCurrentValue.host());
            mPortEdit.setText(String.valueOf(mCurrentValue.port()));
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedString("") : (String) defaultValue);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String url = mCurrentValue != null ?
                    mCurrentValue.newBuilder().
                            host(getIP()).
                            port(getPort()).
                            build().
                            toString()
                    : null;
            if (url != null) {
                setValue(url);
            }
        }
    }

    public void setValue(String value) {
        HttpUrl url = HttpUrl.parse(value);
        if (url == null) {
            return;
        }
        if (callChangeListener(value)) {
            mCurrentValue = url;
            persistString(value);
            notifyChanged();
            setSummary(String.format("\t%1$s: %3$s\n\t%2$s: %4$d",
                            getContext().getString(R.string.pref_ip_address),
                            getContext().getString(R.string.pref_port_number),
                            url.host(),
                            url.port())
            );
        }
    }

    public String getValue() {
        return mCurrentValue.toString();
    }

    private int getPort() {
        Editable portText = mPortEdit != null ? mPortEdit.getText() : null;
        return !TextUtils.isEmpty(portText) ? Integer.valueOf(portText.toString()) : -1;
    }

    private String getIP() {
        Editable ipText = mIpEdit != null ? mIpEdit.getText() : null;
        return ipText != null ? ipText.toString() : "";
    }

    private boolean validateInputs() {
        String ip = getIP();
        int port = getPort();
        boolean validIP = Patterns.IP_ADDRESS.matcher(ip).matches();
        boolean validPort = port > 0 && port <= 65535;
        if (!validIP) {
            String errorMessage = getContext().getString(R.string.invalid_ip_address);
            mIpEdit.setError(errorMessage);
        }
        if (!validPort) {
            String errorMessage = getContext().getString(R.string.invalid_port_number);
            mPortEdit.setError(errorMessage);
        }
        return validIP && validPort;
    }
}
