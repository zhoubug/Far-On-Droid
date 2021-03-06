package com.openfarmanager.android.fragments;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.adapters.LinesAdapter;
import com.openfarmanager.android.core.Settings;
import com.openfarmanager.android.filesystem.actions.RootTask;
import com.openfarmanager.android.model.ViewerBigFileTextViewer;
import com.openfarmanager.android.model.ViewerTextBuffer;
import com.openfarmanager.android.utils.SystemUtils;
import com.openfarmanager.android.view.SelectEncodingDialog;
import com.openfarmanager.android.view.ToastNotification;
import org.apache.commons.io.IOCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;

import static com.openfarmanager.android.controllers.EditViewController.MSG_BIG_FILE;
import static com.openfarmanager.android.controllers.EditViewController.MSG_TEXT_CHANGED;
import static com.openfarmanager.android.utils.Extensions.runAsynk;

/**
 * File viewer
 */
public class Viewer extends Fragment {

    private static final String TAG = "::: Viewer :::";

    private static final int MB = 1048576;
    public static final int MAX_FILE_SIZE = 3 * MB;
    public static final int LINES_COUNT_FRAGMENT = 2000;

    private File mFile;
    private ListView mList;
    private LinesAdapter mAdapter;
    private ProgressBar mProgress;
    private Handler mHandler;

    private ViewerTextBuffer mText;
    private ViewerBigFileTextViewer mBigText;

    private boolean mStopLoading;
    private boolean mBigFile;

    private LoadFileTask mLoadFileTask;
    private LoadTextFragmentTask mLoadTextFragmentTask;

    private Charset mSelectedCharset = Charset.forName("UTF-8");
    private Dialog mCharsetSelectDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        EasyTracker.getInstance(App.sInstance).send(MapBuilder.createAppView().set(Fields.SCREEN_NAME,"Viewer").build());

        View view = inflater.inflate(R.layout.viewer, container);
        mList = (ListView) view.findViewById(android.R.id.list);
        mProgress = (ProgressBar) view.findViewById(android.R.id.progress);

        mText = new ViewerTextBuffer();
        mBigText = new ViewerBigFileTextViewer();

        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mCharsetSelectDialog != null && mCharsetSelectDialog.isShowing()) {
            adjustDialogSize(mCharsetSelectDialog);
        }
    }

    @Override
    public void onDestroy() {
        if (mLoadFileTask != null) {
            mStopLoading = true;
            mLoadFileTask.cancel(true);
            mLoadFileTask = null;
        }
        super.onDestroy();
    }

    public void changeMode() {
        mAdapter.setMode(mAdapter.getMode() == LinesAdapter.MODE_VIEW ? LinesAdapter.MODE_EDIT : LinesAdapter.MODE_VIEW);
        EasyTracker.getInstance(App.sInstance).send(MapBuilder.createAppView().set(Fields.SCREEN_NAME,"Viewer").set("MODE",mAdapter.getMode()==LinesAdapter.MODE_VIEW?"view":"edit").build());
        updateAdapter();
    }

    public int getMode() {
        return mAdapter.getMode();
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setEncoding(Charset charset) {
        mSelectedCharset = charset;
        openSelectedFile();
    }

    public void gotoLine(final int lineNumber, int type) {
        int totalLinesNumber = mAdapter.getCount();
        int position = type == EditViewGotoDialog.GOTO_LINE_POSITION ?
                lineNumber : (int) (totalLinesNumber / 100.0 * lineNumber);

        if (position > totalLinesNumber) {
            position = totalLinesNumber;
        }

        final int endPosition = position;

        mList.post(new Runnable() {
            public void run() {
                mList.setSelection(endPosition);
            }
        });
    }

    public void openFile(final File file) {
        mFile = file;

        String charset = App.sInstance.getSettings().getDefaultCharset();

        if (charset == null) {
            showSelectEncodingDialog();
        } else {
            setEncoding(Charset.forName(charset));
        }
    }

    private void openSelectedFile() {
        mProgress.setVisibility(View.VISIBLE);

        if (mFile.length() > MAX_FILE_SIZE) {
            mBigFile = true;
            mHandler.sendMessage(Message.obtain(mHandler, MSG_BIG_FILE));
            Log.d(TAG, "file to large to be opened in edit mode");
            ToastNotification.makeText(Viewer.this.getActivity(),
                    App.sInstance.getString(R.string.error_file_is_too_big), Toast.LENGTH_SHORT).show();

            mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i == mAdapter.getCount() - 1 && mBigText.hasBottomFragment()) {
                        loadBifFileFragment(mBigText.nextFragment());
                    } else if (i == 0 && mBigText.hasUpperFragment()) {
                        loadBifFileFragment(mBigText.previousFragment());
                    }
                }
            });
        }

        mAdapter = new LinesAdapter(mBigFile ? mBigText : mText);
        mList.setAdapter(mAdapter);

        mStopLoading = false;
        mLoadFileTask = new LoadFileTask();
        //noinspection unchecked
        mLoadFileTask.execute();
    }

    private void loadBifFileFragment(int fragmentNumber) {
        if (mLoadTextFragmentTask != null) {
            mLoadTextFragmentTask.cancel(true);
        }

        mLoadTextFragmentTask = new LoadTextFragmentTask(fragmentNumber);
        //noinspection unchecked
        mLoadTextFragmentTask.execute();
    }

    public void search(String pattern, boolean caseSensitive, boolean wholeWords, boolean regularExpression) {
        mAdapter.search(pattern, caseSensitive, wholeWords, regularExpression);
    }

    public void save() {
        mAdapter.saveCurrentEditLine(getActivity().getCurrentFocus());
        runAsynk(new Runnable() {
            @Override
            public void run() {
                @SuppressWarnings("unchecked")
                FutureTask<Boolean> futureTask = new FutureTask<Boolean>(mText.saveTask(mFile));
                futureTask.run();

                try {
                    if (futureTask.get()) {
                        mHandler.sendMessage(Message.obtain(mHandler, MSG_TEXT_CHANGED, mText.isTextChanged()));
                    }
                } catch (Exception ignore) {}

            }
        });
    }

    public void replace(final String pattern, final String replaceTo, final boolean caseSensitive,
                        final boolean wholeWords, final boolean regularExpression) {
        runAsynk(new Runnable() {
            @Override
            public void run() {

                @SuppressWarnings("unchecked")
                FutureTask<Boolean> futureTask = new FutureTask<Boolean>(
                        mText.replaceTask(pattern, replaceTo, caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE,
                                wholeWords, regularExpression));
                futureTask.run();

                try {
                    if (futureTask.get()) {
                        updateAdapter();
                        mHandler.sendMessage(Message.obtain(mHandler, MSG_TEXT_CHANGED, mText.isTextChanged()));
                    }
                } catch (Exception ignore) {}
            }
        });
    }

    // TODO: we need to avoid this говнокод!!!
    private class LoadTextFragmentTask extends AsyncTask<Void, Void, ArrayList<String>> {

        private int mFragment;
        private int mStartPosition;

        private LoadTextFragmentTask(int fragment) {
            mFragment = fragment;

            mStartPosition = fragment * LINES_COUNT_FRAGMENT;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            BufferedReader is = null;
            ArrayList<String> lines = new ArrayList<String>();
            try {
                is = new BufferedReader(new InputStreamReader(new FileInputStream(mFile), mSelectedCharset.name()));
                String line;

                int count = 0;
                while (count < mStartPosition && (is.readLine()) != null) {
                    count++;
                }

                count = 0;
                while (count < LINES_COUNT_FRAGMENT && (line = is.readLine()) != null) {
                    lines.add(line);
                    count++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return lines;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);
            mBigText.setLines(result);
            mProgress.setVisibility(View.GONE);
            mList.post(new Runnable() {
                public void run() {
                    mList.setSelection(0);
                    mList.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            mAdapter.notifyDataSetChanged();
                            mList.getViewTreeObserver().removeOnScrollChangedListener(this);
                        }
                    });
                }
            });

        }

    }

    private class LoadFileTask extends AsyncTask<Void, Void, Void> {

        private ArrayList<String> mLines;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mLines = new ArrayList<String>();

            if (mBigFile) {
                loadFragmentsFileStrings();
            } else {
                loadFileStrings();
            }

            return null;
        }

        private void loadFragmentsFileStrings() {
            BufferedReader is = null;
            try {
                is = new BufferedReader(new InputStreamReader(new FileInputStream(mFile), mSelectedCharset.name()));
                String line;

                int count = 0;
                while (!mStopLoading && count < LINES_COUNT_FRAGMENT && (line = is.readLine()) != null) {
                    mLines.add(line);
                    count++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void loadFileStrings() {
            BufferedReader is = null;
            try {
                if(mFile.canRead()){
                    is = new BufferedReader(new InputStreamReader(new FileInputStream(mFile), mSelectedCharset.name()));
                }else{
                    is = new BufferedReader(RootTask.readFile(mFile));
                }
                String line;
                while (!mStopLoading && (line = is.readLine()) != null) {
                    mLines.add(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgress.setVisibility(View.GONE);
            mAdapter.swapData(mLines);
        }
    }

    private void updateAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public void showSelectEncodingDialog() {
        mCharsetSelectDialog = new SelectEncodingDialog(getActivity(), mHandler, mFile);
        mCharsetSelectDialog.show();
        adjustDialogSize(mCharsetSelectDialog);
    }

    /**
     * Adjust dialog size. Actuall for old android version only (due to absence of Holo themes).
     *
     * @param dialog dialog whose size should be adjusted.
     */
    private void adjustDialogSize(Dialog dialog) {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(dialog.getWindow().getAttributes());
        params.width = (int) (metrics.widthPixels * 0.65);
        params.height = (int) (metrics.heightPixels * 0.55);

        dialog.getWindow().setAttributes(params);
    }

}
