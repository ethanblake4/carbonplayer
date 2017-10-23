package com.carbonplayer.utils.ui

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.support.v7.graphics.Palette
import android.view.View
import android.widget.TextView
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.enums.PaletteMode


object PaletteUtil {

    data class SwatchPair (
            val primary: Palette.Swatch,
            val secondary: Palette.Swatch
    )

    @JvmStatic fun crossfadeBackground(target: View, swatch: Palette.Swatch) {
        val firstDrawable = target.background
        val newDrawable = ColorDrawable(swatch.rgb)
        val transition = TransitionDrawable(arrayOf(firstDrawable, newDrawable))
        target.background = transition
        transition.startTransition(500)
    }

    @JvmStatic fun crossfadeTitle(target: TextView, swatch: Palette.Swatch) {
        val colorAnim = ObjectAnimator.ofInt(target, "textColor",
                target.currentTextColor, swatch.titleTextColor)
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.start()
    }

    @JvmStatic fun crossfadeSubtitle(target: TextView, swatch: Palette.Swatch) {
        val colorAnim = ObjectAnimator.ofInt(target, "textColor",
                target.currentTextColor, swatch.bodyTextColor)
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.start()
    }

    @JvmStatic fun getSwatches(context: Context, palette: Palette): SwatchPair {
        val prefs = (context.applicationContext as CarbonPlayerApplication)
                .preferences
        var primary: Palette.Swatch? = null //Palette.Swatch(Color.DKGRAY, 10)
        var secondary: Palette.Swatch? = null //Palette.Swatch(Color.WHITE, 10)

        when(prefs.primaryPaletteMode) {
            PaletteMode.POPULOUS -> {
                primary = ColorUtils.getMostPopulousSwatch(palette)
                primary = primary ?: palette.dominantSwatch
                primary = primary ?: Palette.Swatch(Color.DKGRAY, 10)
            }
            PaletteMode.MUTED -> {
                primary = palette.mutedSwatch
                primary = primary ?: palette.lightMutedSwatch
                primary = primary ?: palette.darkMutedSwatch
                primary = primary ?: Palette.Swatch(Color.GRAY, 10)
            }
            PaletteMode.MUTED_DARK -> {
                primary = palette.darkMutedSwatch
                primary = primary ?: palette.mutedSwatch
                primary = primary ?: palette.darkVibrantSwatch
                primary = primary ?: palette.lightMutedSwatch
                primary = primary ?: Palette.Swatch(Color.DKGRAY, 10)
            }
            PaletteMode.MUTED_LIGHT -> {
                primary = palette.lightMutedSwatch
                primary = primary ?: palette.mutedSwatch
                primary = primary ?: palette.lightVibrantSwatch
                primary = primary ?: palette.darkMutedSwatch
                primary = primary ?: Palette.Swatch(Color.LTGRAY, 10)
            }
            PaletteMode.VIBRANT -> {
                primary = palette.vibrantSwatch
                primary = primary ?: palette.lightVibrantSwatch
                primary = primary ?: palette.darkVibrantSwatch
                primary = primary ?: Palette.Swatch(Color.GRAY, 10)
            }
            PaletteMode.VIBRANT_DARK -> {
                primary = palette.darkVibrantSwatch
                primary = primary ?: palette.vibrantSwatch
                primary = primary ?: palette.darkMutedSwatch
                primary = primary ?: palette.lightVibrantSwatch
                primary = primary ?: Palette.Swatch(Color.DKGRAY, 10)
            }
            PaletteMode.VIBRANT_LIGHT -> {
                primary = palette.lightVibrantSwatch
                primary = primary ?: palette.vibrantSwatch
                primary = primary ?: palette.lightMutedSwatch
                primary = primary ?: palette.darkVibrantSwatch
                primary = primary ?: Palette.Swatch(Color.LTGRAY, 10)
            }
            PaletteMode.UNKNOWN -> {
                primary = Palette.Swatch(Color.GRAY, 10)
            }
        }
        val primaryDark = ColorUtils.isDark(primary.hsl)

        val getSecondary = { secondaryMode: PaletteMode ->
            var secondary: Palette.Swatch? = null
            when (secondaryMode) {
                PaletteMode.POPULOUS -> {
                    secondary = ColorUtils.getMostPopulousSwatch(palette)
                    secondary = secondary ?: palette.dominantSwatch
                    secondary = secondary ?: Palette.Swatch(Color.DKGRAY, 10)
                }
                PaletteMode.MUTED -> {

                    secondary = palette.mutedSwatch
                    secondary = secondary ?: palette.lightMutedSwatch
                    secondary = secondary ?: palette.darkMutedSwatch
                    secondary = secondary ?: Palette.Swatch(Color.GRAY, 10)
                }
                PaletteMode.MUTED_DARK -> {
                    secondary = palette.darkMutedSwatch
                    secondary = secondary ?: palette.mutedSwatch
                    secondary = secondary ?: palette.darkVibrantSwatch
                    secondary = secondary ?: palette.lightMutedSwatch
                    secondary = secondary ?: Palette.Swatch(Color.DKGRAY, 10)
                }
                PaletteMode.MUTED_LIGHT -> {
                    secondary = palette.lightMutedSwatch
                    secondary = secondary ?: palette.mutedSwatch
                    secondary = secondary ?: palette.lightVibrantSwatch
                    secondary = secondary ?: palette.darkMutedSwatch
                    secondary = secondary ?: Palette.Swatch(Color.LTGRAY, 10)
                }
                PaletteMode.VIBRANT -> {
                    secondary = palette.vibrantSwatch
                    secondary = secondary ?: palette.lightVibrantSwatch
                    secondary = secondary ?: palette.darkVibrantSwatch
                    secondary = secondary ?: Palette.Swatch(Color.GRAY, 10)
                }
                PaletteMode.VIBRANT_DARK -> {
                    secondary = palette.darkVibrantSwatch
                    secondary = secondary ?: palette.vibrantSwatch
                    secondary = secondary ?: palette.darkMutedSwatch
                    secondary = secondary ?: palette.lightVibrantSwatch
                    secondary = secondary ?: Palette.Swatch(Color.DKGRAY, 10)
                }
                PaletteMode.VIBRANT_LIGHT -> {
                    secondary = palette.lightVibrantSwatch
                    secondary = secondary ?: palette.vibrantSwatch
                    secondary = secondary ?: palette.lightMutedSwatch
                    secondary = secondary ?: palette.darkVibrantSwatch
                    secondary = secondary ?: Palette.Swatch(Color.LTGRAY, 10)
                }
                PaletteMode.UNKNOWN -> {
                    secondary = Palette.Swatch(Color.GRAY, 10)
                }
            }
            secondary
        }

        secondary = getSecondary(prefs.secondaryPaletteMode)

        if(ColorUtils.colorsAreSimilar(primary.rgb, secondary!!.rgb)) {
            secondary = getSecondary(
                    if(primaryDark) {
                        val mode = prefs.secondaryPaletteMode
                        if(mode == PaletteMode.VIBRANT_LIGHT ||
                                mode == PaletteMode.VIBRANT || mode == PaletteMode.VIBRANT_DARK) {
                            PaletteMode.VIBRANT_LIGHT
                        } else PaletteMode.MUTED_LIGHT
                    } else {
                        val mode = prefs.secondaryPaletteMode
                        if(mode == PaletteMode.VIBRANT_LIGHT ||
                                mode == PaletteMode.VIBRANT || mode == PaletteMode.VIBRANT_DARK) {
                            PaletteMode.VIBRANT_DARK
                        } else PaletteMode.MUTED_DARK
                    })

            /*if (ColorUtils.perceptiveDifference(primary.rgb, newSecondary!!.rgb) >
                    ColorUtils.perceptiveDifference(primary.rgb, secondary!!.rgb)) {
                        secondary = newSecondary
            }*/
        }

        return SwatchPair(primary, secondary!!)

    }



}