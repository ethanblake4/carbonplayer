package com.carbonplayer.utils.ui

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette

/**
 * Processes a Palette and gives consistent colors.
 *
 * This class is mostly stolen from Android's MediaNotificationProcessor.java
 */
object PaletteProcessor {

    class ColorAndFilter(@ColorInt color: Int, filter: FloatArray?)

    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private val POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f
    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two
     * colors
     */
    private val MIN_SATURATION_WHEN_DECIDING = 0.19f
    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private val MINIMUM_IMAGE_FRACTION = 0.002
    /**
     * The population fraction to select the dominant color as the text color over a the colored
     * ones.
     */
    private val POPULATION_FRACTION_FOR_DOMINANT = 0.01f

    /**
     * The population fraction to select a white or black color as the background over a color.
     */
    private val POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f
    private val BLACK_MAX_LIGHTNESS = 0.08f
    private val WHITE_MIN_LIGHTNESS = 0.90f
    private const val RESIZE_BITMAP_AREA = 150 * 150

    private fun isWhiteOrBlack(hsl: FloatArray): Boolean {
        return isBlack(hsl) || isWhite(hsl)
    }

    private fun selectMutedCandidate(first: Palette.Swatch,
                                     second: Palette.Swatch): Palette.Swatch? {
        val firstValid = hasEnoughPopulation(first)
        val secondValid = hasEnoughPopulation(second)
        if (firstValid && secondValid) {
            val firstSaturation = first.getHsl()[1]
            val secondSaturation = second.getHsl()[1]
            val populationFraction = first.getPopulation() / second.getPopulation() as Float
            return if (firstSaturation * populationFraction > secondSaturation) {
                first
            } else {
                second
            }
        } else if (firstValid) {
            return first
        } else if (secondValid) {
            return second
        }
        return null
    }

    private fun selectVibrantCandidate(first: Palette.Swatch, second: Palette.Swatch): Palette.Swatch? {
        val firstValid = hasEnoughPopulation(first)
        val secondValid = hasEnoughPopulation(second)
        if (firstValid && secondValid) {
            val firstPopulation = first.getPopulation()
            val secondPopulation = second.getPopulation()
            return if (firstPopulation / secondPopulation.toFloat() < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
                second
            } else {
                first
            }
        } else if (firstValid) {
            return first
        } else if (secondValid) {
            return second
        }
        return null
    }

    private fun findBackgroundColorAndFilter(palette: Palette): ColorAndFilter {
        // by default we use the dominant palette
        val dominantSwatch = palette.dominantSwatch
                ?: // We're not filtering on white or black
                return ColorAndFilter(Color.WHITE, null)

        if (!isWhiteOrBlack(dominantSwatch.hsl)) {
            return ColorAndFilter(dominantSwatch.rgb, dominantSwatch.hsl)
        }
        // Oh well, we selected black or white. Lets look at the second color!
        val swatches = palette.swatches
        var highestNonWhitePopulation = -1f
        var second: Palette.Swatch? = null
        for (swatch in swatches) {
            if (swatch !== dominantSwatch
                    && swatch.population > highestNonWhitePopulation
                    && !isWhiteOrBlack(swatch.hsl)) {
                second = swatch
                highestNonWhitePopulation = swatch.population.toFloat()
            }
        }
        if (second == null) {
            // We're not filtering on white or black
            return ColorAndFilter(dominantSwatch.rgb, null)
        }
        if (dominantSwatch.population / highestNonWhitePopulation > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            // The dominant swatch is very dominant, lets take it!
            // We're not filtering on white or black
            return ColorAndFilter(dominantSwatch.rgb, null)
        } else {
            return ColorAndFilter(second.rgb, second.hsl)
        }
    }

    private fun hasEnoughPopulation(swatch: Palette.Swatch?): Boolean {
        // We want a fraction that is at least 1% of the image
        return swatch != null &&
                swatch.population / RESIZE_BITMAP_AREA.toFloat() > MINIMUM_IMAGE_FRACTION
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private fun isBlack(hslColor: FloatArray): Boolean {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private fun isWhite(hslColor: FloatArray): Boolean {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS
    }

}