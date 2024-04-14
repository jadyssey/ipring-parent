package org.ipring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author lgj
 * @date 8/2/2023
 */
public class MD5Utils {

    private final static Logger logger = LoggerFactory.getLogger(MD5Utils.class);
    public static String md5(String clearText)  {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("{}", e.getLocalizedMessage());
            return null;
        }
        md.update(clearText.getBytes());
        byte[] digest = md.digest();
        return DatatypeConverter
                .printHexBinary(digest).toLowerCase();
    }

    public static String md5(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("{}", e.getLocalizedMessage());
            return null;
        }
        md.update(bytes);
        byte[] digest = md.digest();
        return DatatypeConverter
                .printHexBinary(digest).toLowerCase();
    }

    public static void main(String[] args) {

        String md51 = MD5Utils.md5((4 + "_" + "latest" + "_" + "2e1dbd38a73c47fc8d5aca50fb1ecc61" + "_" + "9845cf0960986641b36b41b661cb4b9d").toLowerCase());
        long time = System.currentTimeMillis() - 1676449342792L;
        String md5 = md5(26 + "_" + "1676448715055" + "_" + 562808);
        System.out.println("md5 = " + md5);
    }
}
