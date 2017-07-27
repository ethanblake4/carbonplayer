package com.carbonplayer.utils;

import com.google.common.collect.ImmutableMap;

import java.io.File;

public class DownloadUtils {
    public static final ImmutableMap<String, String> ExtensionToMimeMap = new ImmutableMap.Builder()
            .put("mp3", "audio/mpeg")
            .put("aac", "audio/aac")
            .put("ogg", "audio/ogg")
            .put("m4a", "audio/mp4")
            .put("wav", "audio/x-wav")
            .put("wma", "audio/x-ms-wma")
            .put("flac", "audio/flac")
            .put("mka", "audio/x-matroska")
            .put("jpg", "image/jpeg")
            .put("png", "image/png")
            .put("webp", "image/webp")
            .build();

    public static String getFileExtension(File file) {
        String filename = file.getName().toLowerCase();
        int pos = filename.lastIndexOf(46);
        if (pos != -1) {
            return filename.substring(pos + 1, filename.length());
        }
        return null;
    }
}

