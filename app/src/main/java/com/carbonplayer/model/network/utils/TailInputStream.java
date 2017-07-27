package com.carbonplayer.model.network.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import com.carbonplayer.model.network.entity.StreamingContent;

import timber.log.Timber;

public class TailInputStream extends InputStream {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Context context;
    private RandomAccessFile input;
    private volatile long startReadPoint;
    private final StreamingContent streamingContent;
    private long totalRead = 0;

    public TailInputStream(Context context, StreamingContent streamingContent, long startRangeByte) {
        Timber.i("New TailInputStream for: %s", streamingContent);
        this.context = context;
        this.streamingContent = streamingContent;
        this.startReadPoint = startRangeByte;
    }

    public String toString() {
        return streamingContent.toString();
    }

    public int read() {
        throw new UnsupportedOperationException();
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        if (this.isClosed.get()) {
            Timber.i("read(%s) ret=-1 since we were closed", streamingContent);

            return -1;
        }
        try {
            streamingContent.waitForData((this.startReadPoint + this.totalRead) + 1);
            if (this.isClosed.get()) {
                Timber.i("read(%s) ret=-1 since we were closed", streamingContent);
                return -1;
            }
            int read = readFromFile(b, offset, length);
            if (read >= 0) {
                return read;
            }
            read = readFromFile(b, offset, length);
            if (read >= 0 || !streamingContent.isFinished()) {
                return read;
            }
                Timber.i("read(%s): %s; ret=-1", streamingContent, totalRead);

            return -1;
        } catch (InterruptedException e) {
            Timber.w("TailInputStream for: %s interrupted", streamingContent);

            return -1;
        }
    }

    private int readFromFile(byte[] b, int offset, int length) throws IOException {
        if (this.input == null) {
            this.input = this.streamingContent.getStreamFile(startReadPoint + totalRead);
            if (this.input == null) {
                Timber.i("read(%s) ret=-1 since the file location doesn't exist", streamingContent);
                return -1;
            }
        }
        int read = this.input.read(b, offset, length);
        if (read > 0) {
            totalRead += (long) read;
            return read;
        }
        input.close();
        input = null;
        return read;
    }

    public void close() throws IOException {
        if (input != null) {
            input.close();
            input = null;
        }
        synchronized (this.isClosed) {
            this.isClosed.set(true);
            this.isClosed.notifyAll();
        }
    }
}
