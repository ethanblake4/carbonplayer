package com.carbonplayer.utils.ui;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

/**
 * Created by ethanelshyeb on 4/25/18.
 */

public class UiUtils {


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
