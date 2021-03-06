package com.openfarmanager.android.filesystem.actions;

import android.support.v4.app.FragmentManager;
import com.openfarmanager.android.model.TaskStatusEnum;
import com.openfarmanager.android.utils.FileUtilsExt;

import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.List;

import static com.openfarmanager.android.model.TaskStatusEnum.*;

/**
 * User: sokhotnyi
 */
public class CopyTask extends FileActionTask {

    private final static byte[] BUFFER = new byte[256 * 1024];

    protected File mDestinationFolder;

    public CopyTask(FragmentManager fragmentManager, OnActionListener listener, List<File> items, File destination) {
        super(fragmentManager, listener, items);
        mDestinationFolder = destination;
    }

    @Override
    protected TaskStatusEnum doInBackground(Void... voids) {
        if (FileUtilsExt.isTheSameFolders(mItems, mDestinationFolder)) { // no need to copy.
            return ERROR_COPY_TO_THE_SAME_FOLDER;
        }

        for (File file : mItems) {
            if (isCancelled()) {
                return CANCELED;
            }
            try {
                copy(file, new File(mDestinationFolder, file.getName()));
            } catch (NullPointerException e) {
                return ERROR_FILE_NOT_EXISTS;
            } catch (InterruptedIOException e) {
                return CANCELED;
            } catch (IOException e) {
                return ERROR_COPY;
            } catch (IllegalArgumentException e) {
                return ERROR_COPY;
            } catch (ConcurrentModificationException e) {
                return ERROR_COPY;
            } catch (Exception e) {
                return ERROR_COPY;
            }
        }
        return TaskStatusEnum.OK;
    }

    private void copy(File source, File destination) throws IOException {
        if (isCancelled()) {
            throw new InterruptedIOException();
        }
        if (source.isDirectory()) {
            String[] files = source.list();
            for (int i = 0; i < files.length; i++) {
                copy(new File(source, files[i]), new File(destination, files[i]));
            }
        } else {
            copyFileRoutine(source, destination);
        }
    }

    private void copyFileRoutine(File file, File destination) throws IOException {
        mCurrentFile = file.getName();
        File parentFile = destination.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Cannot create directory " + parentFile.getAbsolutePath());
        }
        if (!parentFile.canWrite() || !file.canRead()) {
            if (!RootTask.copy(file, destination)) {
                throw new IOException("Cannot copy file to " + parentFile.getAbsolutePath());
            }
        } else {
            InputStream in = new FileInputStream(file);
            OutputStream out = new FileOutputStream(destination);
            int len;
            while ((len = in.read(BUFFER)) > 0) {
                out.write(BUFFER, 0, len);
                doneSize += len;
                updateProgress();
            }
            in.close();
            out.close();
        }
    }

}
