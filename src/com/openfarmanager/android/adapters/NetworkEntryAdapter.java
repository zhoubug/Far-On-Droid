package com.openfarmanager.android.adapters;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.filesystem.FakeFile;
import com.openfarmanager.android.filesystem.FileProxy;
import com.openfarmanager.android.utils.Extensions;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Namashko
 */
public class NetworkEntryAdapter extends FlatFileSystemAdapter {

    private List<FileProxy> mEntries;
    //private String mParentPath;
    private FileProxy mUpNavigator;

    public NetworkEntryAdapter(List<FileProxy> entries, FileProxy upNavigator) {
        setItems(entries, upNavigator);
    }

    public void setItems(List<FileProxy> entries, FileProxy upNavigator) {
        mEntries = entries;
        mUpNavigator = upNavigator;
        mIsRoot = upNavigator.isRoot();
        notifyDataSetChanged();
    }

    public List<FileProxy> getFiles() {
        return mEntries;
    }

    @Override
    public int getCount() {
        return mEntries.size() + 1;
    }

    @Override
    public Object getItem(int i) {
        if (i == 0) {
            return mUpNavigator;
        }
        return mEntries.get(i - 1);
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

        FileProxy item = (FileProxy) getItem(i);

        TextView name = (TextView) view.findViewById(R.id.item_name);
        name.setText(item.getName());
        TextView size = (TextView) view.findViewById(R.id.item_info);

        int fontSize = App.sInstance.getSettings().getMainPanelFontSize();
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        size.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        if (mSelectedFiles.contains(item)) {
            name.setTextColor(Color.YELLOW);
            size.setTextColor(Color.YELLOW);
        } else if (item.isDirectory()) {
            name.setTextColor(Color.WHITE);
            size.setTextColor(Color.WHITE);
        } else {
            name.setTextColor(Color.CYAN);
            size.setTextColor(Color.CYAN);
        }

        FakeFile fakeFile = null;
        if (item instanceof FakeFile) {
            fakeFile = (FakeFile) item;
        }

        if (item.isRoot() || (fakeFile != null && fakeFile.isRoot())) {
            size.setText(R.string.folder_root);
        } else if (item.isUpNavigator() || (fakeFile != null && fakeFile.isUpNavigator())) {
            size.setText(R.string.folder_up);
        } else if (item.isDirectory()) {
            size.setText(R.string.folder);
        } else {
            int type = Extensions.tryParse(App.sInstance.getSettings().getFileInfoType(), 0);
            String value = "";
            switch (type) {
                case 0: default:
                    value = formatSize(item.getSize());
                    break;
                case 1:
                    value = sDateFormat.format(new Date(item.lastModifiedDate()));
                    break;
                case 2:
                    value += "rw";
                    break;
            }

            size.setText(value);
        }
        return view;
    }
}
