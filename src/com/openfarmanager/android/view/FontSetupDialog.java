package com.openfarmanager.android.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;

import com.openfarmanager.android.App;
import com.openfarmanager.android.R;

/**
 * @author Vlad Namashko
 */
public class FontSetupDialog extends Dialog {

    protected final static int MIN_VALUE = 6;
    protected final static int MAX_VALUE = 26 - MIN_VALUE;

    protected int mNewFontSize;
    private View mDialogView;
    private SaveAction mSaveAction;

    public FontSetupDialog(Context context, SaveAction runnable) {
        super(context, R.style.Action_Dialog);
        mSaveAction = runnable;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogView = View.inflate(App.sInstance.getApplicationContext(), R.layout.font_setup_dialog, null);

        int fontSize = mSaveAction.getDefaultValue();

        final TextView fontSizeSample = (TextView) mDialogView.findViewById(R.id.font_size_sample);
        fontSizeSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        mNewFontSize = fontSize - MIN_VALUE;
        SeekBar seekbar = (SeekBar) mDialogView.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(MAX_VALUE);
        seekbar.setProgress(mNewFontSize);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mNewFontSize = MIN_VALUE + progress;
                    fontSizeSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, mNewFontSize);
                }
            }
        });

        mDialogView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        setContentView(mDialogView);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mSaveAction != null) {
            mSaveAction.execute(mNewFontSize);
        }
    }

    public static interface SaveAction {

        void execute(int newValue);

        int getDefaultValue();
    }
}
