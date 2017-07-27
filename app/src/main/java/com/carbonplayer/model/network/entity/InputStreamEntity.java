package com.carbonplayer.model.network.entity;

import com.google.common.io.Closeables;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class InputStreamEntity extends AbstractHttpEntity {
    private boolean consumed = false;
    private final String debugPrefix;
    private InputStream readStream;

    public InputStreamEntity(String debugprefix, InputStream is) {
        this.debugPrefix = debugprefix;
        this.readStream = is;
    }

    public void writeTo(OutputStream outstream) throws IOException {
        boolean z = true;
        long totalRead = 0;
        boolean success = false;
        try {
            outstream.flush();
            byte[] buff = new byte[2048];
            while (true) {
                int len = this.readStream.read(buff);
                if (len < 0) {
                    break;
                }
                outstream.write(buff, 0, len);
                totalRead += (long) len;
            }
            outstream.flush();
            Timber.i("Finished writeTo(" + this.debugPrefix + ") " + totalRead);
            this.consumed = true;
            success = true;
        } finally {
            Closeable closeable = this.readStream;
            if (success) {
                z = false;
            }
            Closeables.close(closeable, z);
        }
    }

    public void consumeContent() {
        throw new UnsupportedOperationException();
    }

    public InputStream getContent() {
        return readStream;
    }

    public boolean isRepeatable() {
        return false;
    }

    public boolean isStreaming() {
        return !consumed;
    }

    public long getContentLength() {
        return -1;
    }
}
