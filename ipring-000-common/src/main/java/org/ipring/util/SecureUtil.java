package org.ipring.util;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author: Rainful
 * @date: 2023/03/13 10:20
 * @description:
 */
@Slf4j
public class SecureUtil {

    private static final String AES_IV = "0000000000000000";

    private static final String FIX_KEY = "CloudTrader4 - pre by Stl Co.Ltd";

    public static String md5(String content) {
        return cn.hutool.crypto.SecureUtil.md5(content);
    }

    public static String hMacSha256(String content, String key) {
        final HMac hmacSha256 = cn.hutool.crypto.SecureUtil.hmacSha256(key);
        return hmacSha256.digestBase64(content, false);
    }

    public static String aesEn(String content) {
        return getAes(FIX_KEY).encryptBase64(content, StandardCharsets.UTF_8);
    }

    public static String aesEn(String content, String key) {
        return getAes(key).encryptBase64(content, StandardCharsets.UTF_8);
    }

    public static String aesDe(String content) {
        if (!StringUtils.hasText(content)) return null;
        return getAes(FIX_KEY).decryptStr(content, StandardCharsets.UTF_8);
    }

    public static String aesDe(String content, String key) {
        return getAes(key).decryptStr(content, StandardCharsets.UTF_8);
    }

    public static String aesEn(byte[] content, String key) {
        return getAes(key).encryptBase64(content);
    }

    public static String aesDe(byte[] content, String key) {
        return getAes(key).decryptStr(content, StandardCharsets.UTF_8);
    }

    private static AES getAes(String key) {
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, key.getBytes());
        aes.setIv(AES_IV.getBytes());
        return aes;
    }

    public static String digestPassword(String password, String salt) {
        return DigestUtils.md5DigestAsHex((password + salt).getBytes());
    }

    public static void main(String[] args) {
        // String key = "751abf10ddbe4d5aab2eed88df881a70";
        // String ret = aesEn("@pG0@Y..q_", key);
        // System.out.println(ret);
        // System.out.println(aesDe(ret, key));


        System.out.println(SecureUtil.aesDe("aAxI+eaWb/oRndBUhdChTkbcVTMYhG12ynX8dQPaD6U="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykgaSZUfvc0DuTf6yFiiGl44="));
        System.out.println(SecureUtil.aesDe("aAxI+eaWb/oRndBUhdChTkbcVTMYhG12ynX8dQPaD6U="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykkvlAabDRcLlnz/MTh0M+C4="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykqdVUKd6l6fCcDlURscKhWk="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykupacesWbQac2/7gGRCmlI0="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykvBSod2O9vdPB38TMbbazCY="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykum4henViWG6M+J/Pb0bsh8="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpstuJPh9f1keAjO1Sa2XSU="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykmQCpiqGljl+bk1QjYus2Yo="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklya/N3RvgjZ4E6a9b2+Bo0="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyku2BftycBX03SkvEHD6jatM="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykrcO+dePjs6unqNcibIcdiw="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykvBSod2O9vdPB38TMbbazCY="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyksS0fNNpfs8bAtxFWZ8vUDY="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpstuJPh9f1keAjO1Sa2XSU="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpstuJPh9f1keAjO1Sa2XSU="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc="));
        System.out.println(SecureUtil.aesDe("4IQ9NtjlbuZu2btdSk4MyfySW5LIJQx9lnJJHV+3zds="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykpMv+5VHtZ1556V4PLxD6Xc="));
        System.out.println(SecureUtil.aesDe("udGkbiHACR4Ms+2daB9RKZdsFekZ/VE8qwE3qJC5Yv4="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykn5uwFbpjPrVa4/nxry+/7g="));
        System.out.println(SecureUtil.aesDe("kXOAvQa1pRiXvil5Yh/KA0a8s1ALs7z5VNpFG+Q7gqw="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykkvlAabDRcLlnz/MTh0M+C4="));
        System.out.println(SecureUtil.aesDe("S3Js2MzilqRxmDEuHGsykkvlAabDRcLlnz/MTh0M+C4="));
    }
}
