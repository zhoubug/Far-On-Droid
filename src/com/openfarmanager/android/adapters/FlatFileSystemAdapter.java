package com.openfarmanager.android.adapters;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.core.archive.ArchiveUtils;
import com.openfarmanager.android.core.archive.MimeTypes;
import com.openfarmanager.android.core.bookmark.BookmarkManager;
import com.openfarmanager.android.filesystem.FileProxy;
import com.openfarmanager.android.filesystem.FileSystemFile;
import com.openfarmanager.android.filesystem.FileSystemScanner;
import com.openfarmanager.android.model.Bookmark;
import com.openfarmanager.android.utils.CustomFormatter;
import com.openfarmanager.android.utils.Extensions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlatFileSystemAdapter extends BaseAdapter {

    protected File mBaseDir;
    protected List<FileProxy> mSelectedFiles = new ArrayList<FileProxy>();
    protected List<FileProxy> mFiles = new ArrayList<FileProxy>();
    boolean mIsRoot;
    private String mFilter;

    public static SimpleDateFormat sDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm");

    public FlatFileSystemAdapter(File baseDir) {
        setBaseDir(baseDir);
    }

    protected FlatFileSystemAdapter() {

    }

    @Override
    public int getCount() {
        return mFiles.size() + (mIsRoot ? 0 : 1);
    }

    @Override
    public Object getItem(int i) {
        if (mIsRoot) {
            return mFiles.get(i);
        }
        if (i == 0) {
            return new FileSystemFile(mBaseDir, "..");
        }
        return mFiles.get(i - 1);
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
        TextView info = (TextView) view.findViewById(R.id.item_info);

        int size = App.sInstance.getSettings().getMainPanelFontSize();
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        File fileItem = (File) item;

        if (mSelectedFiles.contains(item)) {
            setColor(name, info, Color.YELLOW);
        } else if ((!fileItem.canRead() || fileItem.isHidden()) && !item.isVirtualDirectory()) {
            setColor(name, info, Color.GRAY);
        } else if (item.isDirectory()) {
            setColor(name, info, Color.WHITE);
        } else if (ArchiveUtils.getMimeType(fileItem).equals(MimeTypes.MIME_APPLICATION_ANDROID_PACKAGE)) {
            setColor(name, info, Color.GREEN);
        } else if (ArchiveUtils.isArchiveFile(fileItem)) {
            setColor(name, info, Color.MAGENTA);
        } else {
            setColor(name, info, Color.CYAN);
        }

        if (item.isDirectory()) {
            if (item.isRoot()) {
                info.setText(R.string.folder_root);
            } else if (item.isUpNavigator()) {
                info.setText(R.string.folder_up);
            } else if (item.isVirtualDirectory()) {
                info.setText(R.string.virtual_folder);
            } else {
                info.setText(R.string.folder);
            }
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
                    File file = (File) item;
                    value += file.canRead() ? "r" : "-";
                    value += file.canWrite() ? "w" : "-";

                    break;
            }

            info.setText(value);
        }
        return view;
    }

    private void setColor(TextView name, TextView size, int color) {
        name.setTextColor(color);
        size.setTextColor(color);
    }

    public void setSelectedFiles(List<FileProxy> selectedFiles) {
        mSelectedFiles.clear();
        mSelectedFiles.addAll(selectedFiles);
    }

    public void clearSelectedFiles() {
        mSelectedFiles.clear();
    }

    public void setBaseDir(File baseDir) {
        if (baseDir == null) {
            return;
        }

        mBaseDir = baseDir;
        mIsRoot = FileSystemScanner.sInstance.isRoot(baseDir);
        clearSelectedFiles();

        final BookmarkManager bookmarkManager = App.sInstance.getBookmarkManager();
        final String path = mBaseDir.getAbsolutePath();

        if (path.equals(bookmarkManager.getBookmarksFolder())) {
            mFiles.clear();
            List<Bookmark> bookmarks = bookmarkManager.getBookmarks();
            for (Bookmark bookmark : bookmarks) {
                mFiles.add(new FileSystemFile(mBaseDir, bookmark.getBookmarkLabel(), bookmark));
            }
            notifyDataSetChanged();
        } else {
            new AsyncTask<Void, Void, List<FileProxy>>() {
                @Override
                protected List<FileProxy> doInBackground(Void... params) {
                    List<FileProxy> files = FileSystemScanner.sInstance.fallingDown(mBaseDir, mFilter);
                    if (bookmarkManager.isBookmarksEnabled() && path.equals(bookmarkManager.getBookmarksPath())) {
                        files.add(new FileSystemFile(mBaseDir, BookmarkManager.BOOKMARKS_FOLDER, true));
                        FileSystemScanner.sInstance.sort(files);
                    }

                    return files;
                }

                @Override
                protected void onPostExecute(List<FileProxy> aVoid) {
                    mFiles = aVoid;
                    notifyDataSetChanged();
                }
            }.execute();

        }


    }

    protected String formatSize(long length) {
        return CustomFormatter.formatBytes(length);
    }

    public void filter(String obj) {
        mFilter = obj;
        setBaseDir(mBaseDir);
        notifyDataSetChanged();
    }

    public void resetFilter() {
        mFilter = null;
    }

    public int getItemPosition(File oldDir) {
        if (mFiles == null) {
            return 0;
        }

        String old = oldDir.getName();
        for (int i = 0; i < mFiles.size(); i++) {
            if (old.equals(mFiles.get(i).getName())) {
                return i + (mIsRoot ? 0 : 1);
            }
        }
        return 0;
    }
}
