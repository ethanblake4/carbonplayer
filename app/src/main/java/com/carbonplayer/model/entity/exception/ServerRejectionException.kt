package com.carbonplayer.model.entity.exception

import android.content.Context

import com.carbonplayer.R

import java.io.IOException

class ServerRejectionException(val rejectionReason: RejectionReason) : IOException() {

    enum class RejectionReason {
        ANOTHER_STREAM_BEING_PLAYED,
        STREAM_RATE_LIMIT_REACHED,
        DEVICE_NOT_AUTHORIZED,
        TRACK_NOT_IN_SUBSCRIPTION,
        WOODSTOCK_SESSION_TOKEN_INVALID,
        WOODSTOCK_ENTRY_ID_INVALID,
        WOODSTOCK_ENTRY_ID_EXPIRED,
        WOODSTOCK_ENTRY_ID_TOO_EARLY,
        DEVICE_VERSION_BLACKLISTED
    }

    fun getDescription(context: Context): String {
        return when (rejectionReason) {
            RejectionReason.ANOTHER_STREAM_BEING_PLAYED ->
                context.getString(R.string.serverrejectionexception_another_stream_being_played)
            RejectionReason.STREAM_RATE_LIMIT_REACHED ->
                context.getString(R.string.serverrejectionexception_stream_rate_limit_reached)
            RejectionReason.DEVICE_NOT_AUTHORIZED ->
                context.getString(R.string.serverrejectionexception_device_not_authorized)
            RejectionReason.TRACK_NOT_IN_SUBSCRIPTION ->
                context.getString(R.string.serverrejectionexception_track_not_in_subscription)
            RejectionReason.WOODSTOCK_SESSION_TOKEN_INVALID,
            RejectionReason.WOODSTOCK_ENTRY_ID_INVALID,
            RejectionReason.WOODSTOCK_ENTRY_ID_EXPIRED,
            RejectionReason.WOODSTOCK_ENTRY_ID_TOO_EARLY ->
                context.getString(R.string.error_internal_error)
            RejectionReason.DEVICE_VERSION_BLACKLISTED ->
                context.getString(R.string.serverrejectionexception_device_version_blacklisted)
            else -> context.getString(R.string.error_unknown_error)
        }
    }
}