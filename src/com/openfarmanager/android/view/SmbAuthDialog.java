package com.openfarmanager.android.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.controllers.FileSystemController;
import com.openfarmanager.android.model.exeptions.InAppAuthException;
import com.openfarmanager.android.utils.Extensions;

import static com.openfarmanager.android.utils.Extensions.isNullOrEmpty;
import static com.openfarmanager.android.utils.Extensions.runAsynk;
import static com.openfarmanager.android.utils.Extensions.tryParse;

/**
 * author: Vlad Namashko
 */
public class SmbAuthDialog extends Dialog {

    private Handler mHandler;
    private View mDialogView;
    private TextView mError;
    private EditText mDomain;
    private EditText mUserName;
    private EditText mPassword;
    private String mSelectedIp;

    public SmbAuthDialog(Context context, Handler handler, String selectedIp) {
        super(context, R.style.Action_Dialog);
        mHandler = handler;
        mSelectedIp = selectedIp;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogView = View.inflate(App.sInstance.getApplicationContext(), R.layout.dialog_smb_authentication, null);

        mDomain = (EditText) mDialogView.findViewById(R.id.smb_domain);
        mUserName = (EditText) mDialogView.findViewById(R.id.smb_username);
        mPassword = (EditText) mDialogView.findViewById(R.id.smb_password);

        mError = (TextView) mDialogView.findViewById(R.id.error);

        mDialogView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mDialogView.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearError();
                if (!validate()) {
                    return;
                }

                connect();
            }
        });

        mDialogView.findViewById(R.id.smb_scan_network).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismiss();
                mHandler.sendEmptyMessage(FileSystemController.SMB_SCAN_NETWORK_REQUESTED);
            }
        });

        if (mSelectedIp != null) {
            mDomain.setText(mSelectedIp);
        }

        setContentView(mDialogView);
    }

    private boolean validate() {

        if (isNullOrEmpty(mDomain.getText().toString())) {
            setErrorMessage(App.sInstance.getString(R.string.error_empty_domain));
            return false;
        }

        return true;
    }

    private void clearError() {
        updateErrorState("", View.GONE);
    }

    private void setErrorMessage(final String errorMessage) {
        updateErrorState(errorMessage, View.VISIBLE);
    }

    private void setLoading(final boolean isLoading) {
        Message.obtain(mHandler, new Runnable() {
            @Override
            public void run() {
                mDialogView.findViewById(R.id.auth_form).setVisibility(isLoading ? View.GONE : View.VISIBLE);
                mDialogView.findViewById(R.id.progress_form).setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        }).sendToTarget();

    }

    private void updateErrorState(final String errorMessage, final int visibility) {
        Message.obtain(mHandler, new Runnable() {
            @Override
            public void run() {
                mError.setVisibility(visibility);
                mError.setText(errorMessage);
            }
        }).sendToTarget();
    }

    private void connect() {
        setLoading(true);
        runAsynk(mConnectRunnable);
    }

    Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                App.sInstance.getSmbAPI().connectAndSave(mDomain.getText().toString(),
                        mUserName.getText().toString(), mPassword.getText().toString());
            } catch (InAppAuthException e) {
                setErrorMessage(e.getErrorMessage());
                setLoading(false);
                return;
            }

            dismiss();

            mHandler.sendEmptyMessage(FileSystemController.SMB_CONNECTED);
        }
    };
}
