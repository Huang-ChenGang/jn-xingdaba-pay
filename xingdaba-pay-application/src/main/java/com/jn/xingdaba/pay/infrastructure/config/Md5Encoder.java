package com.jn.xingdaba.pay.infrastructure.config;

import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.PAY_FAILED;

@Slf4j
public class Md5Encoder {
    public static String md5Encode(String origin) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return byteToHex(md.digest(origin.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            log.error("md5 encode error.", e);
            throw new PayException(PAY_FAILED);
        }
    }

    public static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
