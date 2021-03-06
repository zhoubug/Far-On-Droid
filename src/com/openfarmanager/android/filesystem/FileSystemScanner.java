package com.openfarmanager.android.filesystem;

import android.text.TextUtils;
import com.openfarmanager.android.App;
import com.openfarmanager.android.core.Settings;
import com.openfarmanager.android.filesystem.actions.RootTask;
import com.openfarmanager.android.filesystem.filter.Filter;
import com.openfarmanager.android.filesystem.filter.FilterFactory;
import com.openfarmanager.android.model.exeptions.FileIsNotDirectoryException;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.PrefixFileFilter;

import java.io.File;
import java.util.*;

/**
 * Vlad Namashko
 */
public class FileSystemScanner {

    public static String ROOT = "/";

    public static final FileSystemScanner sInstance;

    private LinkedList<Filter> mFilters;

    static {
        sInstance = new FileSystemScanner();
    }

    private Comparator<FileProxy> mComparator = new Comparator<FileProxy>() {
        public int compare(FileProxy f1, FileProxy f2) {
            int result = 0;
            for (Filter filter : mFilters) {
                if ((result = filter.doFilter(f1, f2)) != 0) {
                    return result;
                }
            }
            return result;
        }
    };

    private FileSystemScanner() {
        initFilters();
    }

    public void initFilters() {
        ROOT = App.sInstance.getSettings().isSDCardRoot() ? Settings.sSdPath : "/";

        mFilters = new LinkedList<Filter>();
        if (App.sInstance.getSettings().isFoldersFirst()) {
            mFilters.add(FilterFactory.createDirectoryUpFilter());
        }
        mFilters.add(FilterFactory.createPreferredFilter());
    }

    public static Collection<File> getTree(File... root) {
        ArrayList<File> tree = new ArrayList<File>();
        for (File f : root) {
            addFilesRecursively(f, tree);
        }
        return tree;
    }

    private static void addFilesRecursively(File file, Collection<File> all) {
        final File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                all.add(child);
                addFilesRecursively(child, all);
            }
        }
    }

    public File getRoot() {
        return new File(ROOT);
    }

    public boolean isRoot(File node) {
        return node.getAbsolutePath().equals(ROOT);
    }

    public List<FileProxy> fallingDown(File currentNode, String mFilter) throws FileIsNotDirectoryException {
        if (!currentNode.isFile()) {
            String[] files = null;
            List<FileProxy> result = new LinkedList<FileProxy>();

            if (currentNode.canRead()) {
                if (!TextUtils.isEmpty(mFilter)) {
                    try {
                        files = currentNode.list(new PrefixFileFilter(mFilter, IOCase.INSENSITIVE));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (files == null) {
                    files = currentNode.list();
                }
            } else {
                files = RootTask.ls(currentNode);
            }

            if (files == null) {
                return result;
            } else {
                for (String f : files) {
                    FileSystemFile file = new FileSystemFile(currentNode, f);
                    if (App.sInstance.getSettings().isHideSystemFiles() && file.isHidden()) {
                        continue;
                    }
                    result.add(file);
                }
                sort(result);
                return result;
            }
        } else {
            throw new FileIsNotDirectoryException(currentNode.getAbsolutePath());
        }
    }

    public void sort(List<FileProxy> filesToSort) {
        Collections.sort(filesToSort, mComparator);
    }

}
