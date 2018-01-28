package com.carbonplayer.model.network.utils;

import android.content.Context;

import com.carbonplayer.model.network.entity.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class TailInputStream extends InputStream {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Context context;
    private RandomAccessFile input;
    private volatile long startReadPoint;
    private final Stream stream;
    private long totalRead = 0;

    public TailInputStream(Context context, Stream stream, long startRangeByte) {
        Timber.i("New TailInputStream for: %s", stream);
        this.context = context;
        this.stream = stream;
        this.startReadPoint = startRangeByte;
    }

    public String toString() {
        return stream.toString();
    }

    public int read() {
        throw new UnsupportedOperationException();
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        if (this.isClosed.get()) {
            Timber.i("read(%s) ret=-1 since we were closed", stream);

            return -1;
        }
        try {
            stream.waitForData((this.startReadPoint + this.totalRead) + 1);
            if (this.isClosed.get()) {
                Timber.i("read(%s) ret=-1 since we were closed", stream);
                return -1;
            }
            int read = readFromFile(b, offset, length);
            if (read >= 0) {
                return read;
            }
            read = readFromFile(b, offset, length);
            if (read >= 0 || !stream.isFinished()) {
                return read;
            }
            Timber.i("read(%s): %s; ret=-1", stream, totalRead);

            return -1;
        } catch (InterruptedException e) {
            Timber.w("TailInputStream for: %s interrupted", stream);

            return -1;
        }
    }

    private int readFromFile(byte[] b, int offset, int length) throws IOException {
        if (this.input == null) {
            this.input = this.stream.getStreamFile(startReadPoint + totalRead);
            if (this.input == null) {
                Timber.i("read(%s) ret=-1 since the file location doesn't exist", stream);
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
