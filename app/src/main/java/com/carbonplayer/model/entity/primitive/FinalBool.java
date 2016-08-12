package com.carbonplayer.model.entity.primitive;

public class FinalBool {
    private boolean value;

    public FinalBool(){this.value=false;}

    public FinalBool(boolean value){this.value = value;}

    public boolean get() {return value;}

    public void set(boolean value){this.value=value;}

    public void flip(){this.value = !this.value;}
}

