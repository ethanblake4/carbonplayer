package com.carbonplayer.utils.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

import java.util.LinkedList;
import java.util.concurrent.Callable;

import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class UiUtils {

    private static LinkedList<Pair<Callable<Bitmap>, Function1<Bitmap, Unit>>> runnables =
            new LinkedList<>();

    private static Runnable r = () -> {
        while(true) {
            if (!runnables.isEmpty()) {
                try {
                    Bitmap b = runnables.getFirst().getFirst().call();
                    runnables.getFirst().getSecond().invoke(b);
                    runnables.removeFirst();
                } catch (Exception e) {}
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                // shouldnt happen
            }
        }
    };

    private static Thread t = new Thread(r);

    public static void combineBitmaps(Bitmap b1, Bitmap b2, Bitmap b3, Bitmap b4,
                                      Function1<Bitmap, Unit> callback) {
        runnables.add(new Pair<>(() -> {
            Bitmap big = Bitmap.createBitmap(b1.getWidth(), b1.getWidth(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(big);

            float half = b1.getWidth()/2f;

            RectF rect1 = new RectF(0, 0, half, half);
            RectF rect2 = new RectF(half, 0, half*2, half);
            RectF rect3 = new RectF(0, half, half, half*2);
            RectF rect4 = new RectF(half, half, half*2, half*2);

            canvas.drawBitmap(b1, null, rect1, null);
            Thread.sleep(4);
            canvas.drawBitmap(b2, null, rect2, null);
            Thread.sleep(4);
            canvas.drawBitmap(b3, null, rect3, null);
            Thread.sleep(4);
            canvas.drawBitmap(b4, null, rect4, null);

            return big;
        }, callback));

        if(!t.isAlive()) t.start();
    }


    /**
     * Produce a formatted SpannableString object from a given String
     * input, with all lowercase characters converted to smallcap
     * characters. Uses only standard A-Z characters, so works with
     * any font.
     *
     * @param input  The input string, e.g. "Small Caps"
     * @return       A formatted SpannableString, e.g. "Sᴍᴀʟʟ Cᴀᴘs"
     */
    public static SpannableString getSmallCapsString(String input) {
        // values needed to record start/end points of blocks of lowercase letters
        char[] chars = input.toCharArray();
        int currentBlock = 0;
        int[] blockStarts = new int[chars.length];
        int[] blockEnds = new int[chars.length];
        boolean blockOpen = false;

        // record where blocks of lowercase letters start/end
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (c >= 'a' && c <= 'z') {
                if (!blockOpen) {
                    blockOpen = true;
                    blockStarts[currentBlock] = i;
                }
                // replace with uppercase letters
                chars[i] = (char) (c - 'a' + '\u0041');
            } else {
                if (blockOpen) {
                    blockOpen = false;
                    blockEnds[currentBlock] = i;
                    ++currentBlock;
                }
            }
        }

        // add the string end, in case the last character is a lowercase letter
        blockEnds[currentBlock] = chars.length;

        // shrink the blocks found above
        SpannableString output = new SpannableString(String.valueOf(chars));
        for (int i = 0; i < Math.min(blockStarts.length, blockEnds.length); ++i) {
            output.setSpan(new RelativeSizeSpan(0.8f), blockStarts[i], blockEnds[i], Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }

        return output;
    }
}
