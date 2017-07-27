package com.carbonplayer.model.entity.primitive;

public final class RealmInteger {
    private int value;

    public RealmInteger(){this.value=0;}

    @SuppressWarnings("unused")
    public RealmInteger(int value){this.value = value;}

    public int value() {return value;}

    public void set(int value){this.value=value;}

    public void increment(){this.value++;}
}