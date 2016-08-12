package com.carbonplayer.model.entity.exception;

public class ResponseCodeException extends Exception {

    @SuppressWarnings("unused")
    public ResponseCodeException(){
        super();
    }

    @SuppressWarnings("unused")
    public ResponseCodeException(String message){
        super(message);
    }
}
