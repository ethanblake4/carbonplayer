package com.carbonplayer.model.entity.primitive;

import io.realm.RealmObject;

public class RealmLong extends RealmObject {
    private long val;

    public RealmLong() {
    }

    public RealmLong(long value) {
        set(value);
    }

    public void set(long value) {
        this.val = value;
    }

    public long get() {
        return val;
    }
}
