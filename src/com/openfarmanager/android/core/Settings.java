package com.openfarmanager.android.core;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.openfarmanager.android.App;
import com.openfarmanager.android.filesystem.FileSystemScanner;
import com.openfarmanager.android.utils.SystemUtils;

import java.io.File;

public class Settings {

    public static final String PANEL_STATE_SETTINGS = "panelState";
    public static final String LEFT_PANEL_PATH = "left";
    public static final String RIGHT_PANEL_PATH = "right";
    public static final String ACTIVE_PANEL = "active_panel";

    public static final String SDCARD_ROOT = "sdcard_root";
    public static final String HIDE_SYSTEM_FILES = "hide_system_files";
    public static final String FILES_SORT = "files_sort";
    public static final String FILE_INFO = "file_info";
    public static final String FOLDERS_FIRST = "folders_first";
    public static final String MULTI_PANELS = "multi_panels";
    public static final String MULTI_PANELS_CHANGED = "multi_panels_changed";
    public static final String FLEXIBLE_PANELS = "flexible_panels";
    public static final String FORCE_EN_LANG = "force_en_lang";
    public static final String SHOW_TIPS = "show_tips_full_screen";
    public static final String ENABLE_HOME_FOLDER = "enable_home_folder";
    public static final String HOME_FOLDER = "home_folder";
    public static final String MAIN_PANEL_FONT_SIZE = "main_panel_font_size";
    public static final String BOTTOM_PANEL_FONT_SIZE = "bottom_panel_font_size";
    public static final String VIEWER_FONT_SIZE = "viewer_panel_font_size";
    public static final String VIEWER_DEFAULT_CHARSET_NAME = "viewer_charset_name";
    public static final String ROOT_ENABLED = "root_enabled";

    private static File sSdCard;
    public static String sSdPath;

    private int mMainPanelFontSize = 0;
    private int mBottomPanelFontSize = 0;
    private int mViewerFontSize = 0;

    static {
        sSdCard = Environment.getExternalStorageDirectory();
        sSdPath = sSdCard != null ? sSdCard.getPath() : FileSystemScanner.ROOT;
    }

    public void savePanelsState(String leftPanelPath, String rightPanelPath, boolean isLeftPanelActive) {
        SharedPreferences.Editor edit = getPanelSettings().edit();
        edit.putString(LEFT_PANEL_PATH, leftPanelPath);
        edit.putString(RIGHT_PANEL_PATH, rightPanelPath);
        edit.putBoolean(ACTIVE_PANEL, isLeftPanelActive);
        edit.commit();
    }

    public SharedPreferences getPanelSettings() {
        return App.sInstance.getSharedPreferences(PANEL_STATE_SETTINGS, 0);
    }

    public String getLeftPanelPath() {
        return getPanelSettings().getString(LEFT_PANEL_PATH, sSdPath);
    }

    public String getRightPanelPath() {
        return getPanelSettings().getString(RIGHT_PANEL_PATH, sSdPath);
    }

    public boolean isLeftPanelActive() {
        return getPanelSettings().getBoolean(ACTIVE_PANEL, true);
    }

    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(App.sInstance);
    }

    public boolean isSDCardRoot() {
        return getSharedPreferences().getBoolean(SDCARD_ROOT, false);
    }

    public boolean isHideSystemFiles() {
        return getSharedPreferences().getBoolean(HIDE_SYSTEM_FILES, false);
    }

    public boolean isFoldersFirst() {
        return getSharedPreferences().getBoolean(FOLDERS_FIRST, false);
    }

    public String getFileSortValue() {
        return getSharedPreferences().getString(FILES_SORT, "0");
    }

    /**
     * Get file info requested type.
     *
     *  0 - File Size
     *  1 - Modification Date
     *  2 - Permissions
     *
     * @return file type info string index.
     */
    public String getFileInfoType() {
        return getSharedPreferences().getString(FILE_INFO, "0");
    }

    public void setFileSortValue(String value) {
        getSharedPreferences().edit().putString(Settings.FILES_SORT, value).commit();
        FileSystemScanner.sInstance.initFilters();
    }

    public void setFileInfoTypeValue(String value) {
        getSharedPreferences().edit().putString(Settings.FILE_INFO, value).commit();
    }

    public boolean isMultiPanelMode() {
        if (!getSharedPreferences().getBoolean(Settings.MULTI_PANELS_CHANGED, false)) {
            return SystemUtils.isTablet();
        }

        return getSharedPreferences().getBoolean(MULTI_PANELS, SystemUtils.isTablet());
    }

    public boolean isFlexiblePanelsMode() {
        return getSharedPreferences().getBoolean(FLEXIBLE_PANELS, false);
    }

    public boolean isForceUseEn() {
        return getSharedPreferences().getBoolean(FORCE_EN_LANG, false);
    }

    public boolean isEnableHomeFolder() {
        return getSharedPreferences().getBoolean(ENABLE_HOME_FOLDER, false);
    }

    public String getHomeFolder() {
        return getSharedPreferences().getString(HOME_FOLDER, sSdPath);
    }

    public void setHomeFolder(String path) {
        getSharedPreferences().edit().putString(HOME_FOLDER, path).commit();
    }

    public int getMainPanelFontSize() {
        if (mMainPanelFontSize == 0) {
            mMainPanelFontSize = getSharedPreferences().getInt(MAIN_PANEL_FONT_SIZE, 18);
        }

        return mMainPanelFontSize;
    }

    public void setMainPanelFontSize(int size) {
        getSharedPreferences().edit().putInt(MAIN_PANEL_FONT_SIZE, size).commit();
        mMainPanelFontSize = size;
    }

    public int getBottomPanelFontSize() {
        if (mBottomPanelFontSize == 0) {
            mBottomPanelFontSize = getSharedPreferences().getInt(BOTTOM_PANEL_FONT_SIZE, 14);
        }

        return mBottomPanelFontSize;
    }

    public void setBottomPanelFontSize(int size) {
        getSharedPreferences().edit().putInt(BOTTOM_PANEL_FONT_SIZE, size).commit();
        mBottomPanelFontSize = size;
    }

    public int getViewerFontSize() {
        if (mViewerFontSize == 0) {
            mViewerFontSize = getSharedPreferences().getInt(VIEWER_FONT_SIZE, 14);
        }

        return mViewerFontSize;
    }

    public void setViewerFontSize(int size) {
        getSharedPreferences().edit().putInt(VIEWER_FONT_SIZE, size).commit();
        mViewerFontSize = size;
    }

    public boolean isShowTips() {
        return getSharedPreferences().getBoolean(SHOW_TIPS, true);
    }

    public void setDefaultCharset(String key) {
        getSharedPreferences().edit().putString(VIEWER_DEFAULT_CHARSET_NAME, key).commit();
    }

    public String getDefaultCharset() {
        return getSharedPreferences().getString(VIEWER_DEFAULT_CHARSET_NAME, "UTF-8");
    }

    public boolean getRootEnabled(){
        return getSharedPreferences().getBoolean(ROOT_ENABLED, false);
    }

}
