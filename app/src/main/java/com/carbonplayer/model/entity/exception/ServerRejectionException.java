package com.carbonplayer.model.entity.exception;

import android.content.Context;

import com.carbonplayer.R;

import java.io.IOException;
public class ServerRejectionException extends IOException {
    private final RejectionReason mRejectionReason;

    public enum RejectionReason {
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

    public ServerRejectionException(RejectionReason reason) {
        this.mRejectionReason = reason;
    }

    public String getDescription(Context context){
        switch (mRejectionReason){
            case ANOTHER_STREAM_BEING_PLAYED:
                return context.getString(R.string.serverrejectionexception_another_stream_being_played);
            case STREAM_RATE_LIMIT_REACHED:
                return context.getString(R.string.serverrejectionexception_stream_rate_limit_reached);
            case DEVICE_NOT_AUTHORIZED:
                return context.getString(R.string.serverrejectionexception_device_not_authorized);
            case TRACK_NOT_IN_SUBSCRIPTION:
                return context.getString(R.string.serverrejectionexception_track_not_in_subscription);
            case WOODSTOCK_SESSION_TOKEN_INVALID:
            case WOODSTOCK_ENTRY_ID_INVALID:
            case WOODSTOCK_ENTRY_ID_EXPIRED:
            case WOODSTOCK_ENTRY_ID_TOO_EARLY:
                return context.getString(R.string.error_internal_error);
            case DEVICE_VERSION_BLACKLISTED:
                return context.getString(R.string.serverrejectionexception_device_version_blacklisted);
            default:
                return context.getString(R.string.error_unknown_error);
        }
    }

    public RejectionReason getRejectionReason() {
        return this.mRejectionReason;
    }
}