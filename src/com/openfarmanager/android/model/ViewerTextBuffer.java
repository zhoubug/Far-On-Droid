package com.openfarmanager.android.model;

import com.openfarmanager.android.App;
import com.openfarmanager.android.filesystem.actions.RootTask;
import com.openfarmanager.android.utils.FileUtilsExt;
import org.apache.commons.io.IOCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holder for <code>Viewer</code> text. Text is represented by list of strings.
 * Text is originally initialized in 2 copies: one to be modified (after replace or in 'edit' mode)
 * and another to be immutable, so we can check is text changed.
 */
public class ViewerTextBuffer implements TextBuffer {

    private ArrayList<String> mNumberOfLines;
    private ArrayList<String> mOriginalLines;

    private StringBuilder mTempBuilder;

    public ViewerTextBuffer() {
        mNumberOfLines = new ArrayList<String>();
        mTempBuilder = new StringBuilder();
    }

    @Override
    public String getLine(int lineNumber) {
        return mNumberOfLines.get(lineNumber);
    }

    @Override
    public void setLine(int lineNumber, String text) {
        try {
            mNumberOfLines.set(lineNumber, text);
        } catch (IndexOutOfBoundsException ignore) {} // some unpredictable cases
    }

    @Override
    public void appendEmptyLine() {
        mNumberOfLines.add("");
    }

    @Override
    public int size() {
        return mNumberOfLines.size();
    }

    public boolean isTextChanged() {

        for (int i = 0; i < mNumberOfLines.size(); i++) {
            String string = mNumberOfLines.get(i);
            try {
                String originalString = mOriginalLines.get(i);
                if (!originalString.equals(string)) {
                    return true;
                }
            } catch (IndexOutOfBoundsException e) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void swapData(ArrayList<String> strings) {
        mNumberOfLines = strings;
        mOriginalLines = new ArrayList<String>(mNumberOfLines);
        Collections.copy(mOriginalLines, mNumberOfLines);
    }

    public void syncStringLists() {
        mNumberOfLines = new ArrayList<String>(mOriginalLines);
        Collections.copy(mNumberOfLines, mOriginalLines);
    }

    public Callable replaceTask(String pattern, String replaceTo,
                             IOCase caseSensitive, boolean wholeWords, boolean regularExpression) {

        return new ReplaceCallable(pattern, replaceTo, caseSensitive, wholeWords, regularExpression);
    }

    public void replace(String pattern, String replaceTo,
                               IOCase caseSensitive, boolean wholeWords, boolean regularExpression) {
        Pattern patternMatch = FileUtilsExt.createWordSearchPattern(pattern, wholeWords, caseSensitive);

        int i = 0;
        for (String line : mNumberOfLines) {
            String text = line;
            Matcher matcher = patternMatch.matcher(text);
            if (matcher.find()) {
                int firstOccurrence;
                int replacement = 0;
                int delta = pattern.length() - replaceTo.length();
                do {
                    firstOccurrence = matcher.start();
                    text = replaceText(text, pattern, replaceTo, firstOccurrence - replacement);
                    replacement += delta;

                } while (matcher.find());
            }
            mNumberOfLines.set(i++, text);
        }
    }

    public Callable saveTask(File file) {
        return new SaveCallable(file);
    }

    public void saveToFile(File file) throws IOException {
        if (!isTextChanged()) {
            return;
        }

        BufferedWriter writer = null;
        try {
            String charset = App.sInstance.getSettings().getDefaultCharset();
            String encoding = charset != null ? charset : Charset.defaultCharset().name();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
            for (String string : mNumberOfLines) {
                writer.write(string);
                writer.newLine();
            }
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (Exception ignore) { }
            }
        }
    }

    public void saveToFileRoot(File file) throws IOException {
        if (!isTextChanged()) {
            return;
        }

        try {
            StringBuilder resultString = new StringBuilder();
            for (String string : mNumberOfLines) {
                resultString.append(string).append("//\n");
            }

            RootTask.saveFile(file, resultString.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String replaceText(String text, String pattern, String replaceTo, int firstOccurrence) {
        mTempBuilder.delete(0, mTempBuilder.length());
        return mTempBuilder.append(text.substring(0, firstOccurrence)).
                append(replaceTo).
                append(text.substring(firstOccurrence + pattern.length())).toString();
    }

    private class ReplaceCallable implements Callable {

        private String mSearchPattern;
        private String mReplaceTo;
        private IOCase mCaseSensitive;
        private boolean mWholeWords;
        private boolean mRegularExpression;

        public ReplaceCallable(String pattern, String replaceTo,
                               IOCase caseSensitive, boolean wholeWords, boolean regularExpression) {
            mSearchPattern = pattern;
            mReplaceTo = replaceTo;
            mCaseSensitive = caseSensitive;
            mWholeWords = wholeWords;
            mRegularExpression = regularExpression;
        }

        @Override
        public Boolean call() {
            replace(mSearchPattern, mReplaceTo, mCaseSensitive, mWholeWords, mRegularExpression);
            return true;
        }
    }

    private class SaveCallable implements Callable {

        private File mCurrentFile;

        private SaveCallable(File currentFile) {
            mCurrentFile = currentFile;
        }

        @Override
        public Boolean call() throws IOException {
            if(mCurrentFile.canWrite()) {
                saveToFile(mCurrentFile);
            } else {
                saveToFileRoot(mCurrentFile);
            }
            syncStringLists();
            return true;
        }
    }
}
