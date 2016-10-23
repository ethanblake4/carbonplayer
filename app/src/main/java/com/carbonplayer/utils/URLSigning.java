package com.carbonplayer.utils;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

/**
 * Signs streaming URLs. 2nd most complicated piece of code in the app
 */

public class URLSigning {

    public static String sign(String song_id, String salt) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] _s1 = Base64.decode("VzeC4H4h+T2f0VI180nVX8x+Mb5HiTtGnKgH52Otj8ZCGDz9jRW" +
                "yHb6QXK0JskSiOgzQfwTY5xgLLSdUSreaLVMsVVWfxfa8Rw==", Base64.DEFAULT);
        byte[] _s2 = Base64.decode("ZAPnhUkYwQ6y5DdQxWThbvhJHN8msQ1rqJw0ggKdufQjelrKuiG" +
                "GJI30aswkgCWTDyHkTGK9ynlqTkJ5L4CiGGUabGeo8M6JTQ==", Base64.DEFAULT);

        byte[] _key = new byte[_s1.length];
        for (int i = 0; i < _s1.length; i++) {
            _key[i] = (byte) (_s1[i] ^ _s2[i]);
        }

        Timber.d("key: byte[] -> str(ASCII): %s", new String(_key, "ASCII"));

        //String salt = String.valueOf(new Date().getTime());

        Timber.d("salt: %s", salt);

        String digest = "";
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(new String(_key).getBytes("ASCII"), "HmacSHA1"));
        mac.update(song_id.getBytes("UTF-8"));
        mac.update(salt.getBytes("UTF-8"));
        digest = new String(Base64.encode(mac.doFinal(), Base64.URL_SAFE | Base64.NO_PADDING));

        Timber.d("digest: %s", digest);
        return digest;
    }
}
