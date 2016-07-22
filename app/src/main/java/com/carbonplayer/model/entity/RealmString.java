package com.carbonplayer.model.entity;

import io.realm.RealmObject;

/**
 * RealmObject wrapper of String
 */
public class RealmString extends RealmObject {
    private String val;

    public RealmString(){}

    public RealmString(String val){
        this.val = val;
    }

    public String getValue() {
        return val;
    }

    public void setValue(String value) {
        this.val = value;
    }

    @Override
    public String toString(){
        return val;
    }
}
