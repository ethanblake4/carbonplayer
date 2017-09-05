package com.carbonplayer.model.network.utils;


import com.google.common.io.Closeables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class IOUtils {
    public static byte[] readSmallStream(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = null;
        try {
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            int copied = 0;
            boolean reachedEos = false;
            while (copied <= max && !reachedEos) {
                try {
                    int read = in.read(buffer, 0, Math.min(buffer.length, (max - copied) + 1));
                    if (read < 0) {
                        reachedEos = true;
                    } else {
                        out2.write(buffer, 0, read);
                        copied += read;
                    }
                } catch (Throwable th2) {
                    out = out2;
                }
            }
            if (reachedEos) {
                byte[] toByteArray = out2.toByteArray();
                Closeables.close(in, false);
                Closeables.close(out2, false);
                return toByteArray;
            }
            throw new IOException("Stream is too large to cache. Exceeds " + max);
        } catch (IOException e) {
            Closeables.close(in, false);
            Closeables.close(out, false);
            throw e;
        }
    }
}
