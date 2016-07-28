package com.carbonplayer.model.entity;

import timber.log.Timber;

/**
 * Typical callback function
 */
public abstract class StdCallback{
    public abstract void onSuccess();
    public void onError(Throwable error){
        Timber.e(error, error.getMessage());
    }
}
