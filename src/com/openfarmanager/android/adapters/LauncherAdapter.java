package com.openfarmanager.android.adapters;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.filesystem.FileProxy;
import com.openfarmanager.android.fragments.GenericPanel;
import com.openfarmanager.android.fragments.MainPanel;
import com.openfarmanager.android.model.Bookmark;
import com.openfarmanager.android.model.FileActionEnum;
import com.openfarmanager.android.view.ToastNotification;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class LauncherAdapter extends FlatFileSystemAdapter {

    private Handler mHandler;

    public LauncherAdapter(Handler handler) {
        mHandler = handler;
        refresh();
    }

    public void refresh() {
        new AsyncTask<Void, Void, List<FileProxy>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mHandler.sendEmptyMessage(GenericPanel.START_LOADING);
            }

            @Override
            protected List<FileProxy> doInBackground(Void... voids) {
                List<FileProxy> result = new LinkedList<FileProxy>();

                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                final PackageManager manager = App.sInstance.getPackageManager();
                final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

                if (apps != null) {
                    for (ResolveInfo info : apps) {
                        ComponentName componentName = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                        ComponentProxy applicationInfo = new ComponentProxy();
                        applicationInfo.mComponentName = componentName;
                        applicationInfo.mName = String.valueOf(info.loadLabel(manager));
                        if (TextUtils.isEmpty(applicationInfo.mName)) {
                            applicationInfo.mName = info.activityInfo.name;
                        }
                        result.add(applicationInfo);
                    }
                }

                Collections.sort(result, new Comparator<FileProxy>() {
                    @Override
                    public int compare(FileProxy applicationInfo, FileProxy applicationInfo2) {
                        return applicationInfo.getName().compareTo(applicationInfo2.getName());
                    }
                });

                return result;
            }

            @Override
            protected void onPostExecute(List<FileProxy> result) {
                mFiles = result;
                notifyDataSetChanged();
                mHandler.sendEmptyMessage(GenericPanel.STOP_LOADING);
            }
        }.execute();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public FileProxy getItem(int i) {
        return mFiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(App.sInstance.getApplicationContext()).inflate(R.layout.panel_item, null);
        }

        FileProxy info = getItem(i);

        TextView name = (TextView) view.findViewById(R.id.item_name);
        name.setText(info.getName());
        name.setTextColor(Color.GREEN);
        TextView infoItem = (TextView) view.findViewById(R.id.item_info);

        int size = App.sInstance.getSettings().getMainPanelFontSize();
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        infoItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, size); // to adjust item size

        return view;
    }

    public void onItemClick(int i) {
        final PackageManager manager = App.sInstance.getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(getItem(i).getFullPath());
        App.sInstance.startActivity(intent);
    }

    public FileActionEnum[] getAvailableActions() {
        return new FileActionEnum[] {
                FileActionEnum.OPEN, FileActionEnum.INFO, FileActionEnum.DELETE
        };
    }

    public void executeAction(final FileActionEnum action, MainPanel inactivePanel) {
        if (mSelectedFiles.isEmpty()) {
            return;
        }

        Intent intent;
        String fullPath = mSelectedFiles.get(0).getFullPath();
        switch (action) {
            case OPEN:
                final PackageManager manager = App.sInstance.getPackageManager();
                App.sInstance.startActivity(manager.getLaunchIntentForPackage(fullPath));
                break;

            case INFO:
                try {
                    intent = new Intent()
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", fullPath, null));
                    App.sInstance.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    ToastNotification.makeText(App.sInstance, App.sInstance.getString(R.string.error_not_supported), Toast.LENGTH_LONG).show();
                }
                break;

            case DELETE:
                try {
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", fullPath, null)).
                            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    App.sInstance.startActivity(uninstallIntent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    ToastNotification.makeText(App.sInstance, App.sInstance.getString(R.string.error_not_supported), Toast.LENGTH_LONG).show();
                }
                break;

        }
    }

    public class ComponentProxy implements FileProxy<ComponentProxy> {
        private ComponentName mComponentName;
        private String mName;

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public long lastModifiedDate() {
            return 0;
        }

        @Override
        public List<ComponentProxy> getChildren() {
            return null;
        }

        @Override
        public String getFullPath() {
            return mComponentName.getPackageName();
        }

        @Override
        public String getParentPath() {
            return null;
        }

        @Override
        public boolean isUpNavigator() {
            return false;
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        public boolean isVirtualDirectory() {
            return false;
        }

        @Override
        public boolean isBookmark() {
            return false;
        }

        @Override
        public Bookmark getBookmark() {
            return null;
        }
    }

}
