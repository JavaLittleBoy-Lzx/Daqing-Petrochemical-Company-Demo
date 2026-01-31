package com.parkingmanage.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import cn.hutool.core.codec.Base64Encoder;
import org.apache.poi.xddf.usermodel.SystemColor;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;
import java.nio.charset.StandardCharsets;
/**
 @Description:
 @PROJECT_NAME: parkingmanage
 @PACKAGE_NAME: com.parkingmanage.utils
 @NAME: AesUtil
 @author:yuli
 @Version: 1.0
 @DATE: 2021/11/2 15:28
*/
public class AesUtil {
    //Aes加密算法
    private static final SecretKey secretKey = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), "1234567890abcdef".getBytes(StandardCharsets.UTF_8));
    //密码加密
    private static final SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, secretKey);

    //密码解密
    public static String encrypt(String s) {
        return aes.encryptHex(s);
    }
    //Base64EnCode编码
    public static String Base64EnCode(String s) {
        return Base64Encoder.encode (s);
    }

    //MD5密码加密
    public static String encryptMd5(String s) {
        return SecureUtil.md5(s);
    }
    //aes算法
    public static String decrypt(String s) {
        return aes.decryptStr(s, CharsetUtil.CHARSET_UTF_8);
    }
}
