package com.carbonplayer.utils.protocol;

import android.util.Base64;

import org.apache.http.util.EncodingUtils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

/**
 * Signs streaming URLs.
 */

public class URLSigning {

    public static String sign(String song_id, String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {

        byte[] _s1 = Base64.decode("VzeC4H4h+T2f0VI180nVX8x+Mb5HiTtGnKgH52Otj8ZCGDz9jRW" +
                "yHb6QXK0JskSiOgzQfwTY5xgLLSdUSreaLVMsVVWfxfa8Rw==", Base64.DEFAULT);
        byte[] _s2 = Base64.decode("ZAPnhUkYwQ6y5DdQxWThbvhJHN8msQ1rqJw0ggKdufQjelrKuiG" +
                "GJI30aswkgCWTDyHkTGK9ynlqTkJ5L4CiGGUabGeo8M6JTQ==", Base64.DEFAULT);

        byte[] _key = new byte[_s1.length];
        for (int i = 0; i < _s1.length; i++) {
            _key[i] = (byte) (_s1[i] ^ _s2[i]);
        }

        Timber.d("salt: %s", salt);

        String digest;
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(_key, mac.getAlgorithm()));
        mac.update(EncodingUtils.getAsciiBytes(song_id));
        digest = new String(Base64.encode(mac.doFinal(EncodingUtils.getAsciiBytes(salt)), 11));

        Timber.d("digest: %s", digest);
        return digest;
    }
}
