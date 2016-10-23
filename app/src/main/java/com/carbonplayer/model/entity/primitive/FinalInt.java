package com.carbonplayer.model.entity.primitive;

public final class FinalInt {
    private int value;

    public FinalInt(){this.value=0;}

    @SuppressWarnings("unused")
    public FinalInt(int value){this.value = value;}

    public int value() {return value;}

    public void set(int value){this.value=value;}

    public void increment(){this.value++;}
}