package com.carbonplayer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.util.Util;

public class GlidePaletteUtils {

    /*public class PaletteBitmap {
        public final Palette palette;
        public final Bitmap bitmap;

        public PaletteBitmap(@NonNull Bitmap bitmap, @NonNull Palette palette) {
            this.bitmap = bitmap;
            this.palette = palette;
        }
    }
    public class PaletteBitmapResource implements Resource<PaletteBitmap> {
        private final PaletteBitmap paletteBitmap;
        private final BitmapPool bitmapPool;

        public PaletteBitmapResource(@NonNull PaletteBitmap paletteBitmap, @NonNull BitmapPool bitmapPool) {
            this.paletteBitmap = paletteBitmap;
            this.bitmapPool = bitmapPool;
        }

        @Override public PaletteBitmap get() {
            return paletteBitmap;
        }

        @Override public int getSize() {
            return Util.getBitmapByteSize(paletteBitmap.bitmap);
        }

        @Override public void recycle() {
            if (!bitmapPool.put(paletteBitmap.bitmap)) {
                paletteBitmap.bitmap.recycle();
            }
        }
    }
    public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, PaletteBitmap> {
        private final BitmapPool bitmapPool;

        public PaletteBitmapTranscoder(@NonNull Context context) {
            this.bitmapPool = Glide.get(context).getBitmapPool();
        }

        @Override public Resource<PaletteBitmap> transcode(Resource<Bitmap> toTranscode) {
            Bitmap bitmap = toTranscode.get();
            Palette palette = new Palette.Builder(bitmap).generate();
            PaletteBitmap result = new PaletteBitmap(bitmap, palette);
            return new PaletteBitmapResource(result, bitmapPool);
        }

        @Override public String getId() {
            return PaletteBitmapTranscoder.class.getName();
        }
    }*/
}
