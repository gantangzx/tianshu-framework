package com.gantang.tianshu.utils;

import cn.hutool.core.util.RandomUtil;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GoogleAuthenticator {


    /**
     * 生成随机的密钥
     * @return
     */
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static String getRandomSecretKey() {
        StringBuilder secret = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            secret.append(BASE32_ALPHABET.charAt(RandomUtil.randomInt(BASE32_ALPHABET.length())));
        }
        return secret.toString();
    }


    /**
     * 根据密钥，计算出当前时间的动态口令 （30s会变化一次）
     * @param secretKey
     * @return
     */
    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        long time = (System.currentTimeMillis() / 1000) / 30;
        String hexTime = Long.toHexString(time);
        return TOTP.generateTOTP(hexKey, hexTime, "6");
    }

    /**
     * 根据密钥，生成 TOPT 密钥的 URI 字符串
     * @param secretKey
     * @param account
     * @param issuer
     * @return
     */
    public static String getGoogleAuthenticatorBarCode(String secretKey, String account, String issuer) {
        try {
            return "otpauth://totp/"
                    + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * 根据 TOPT 密钥的 URI 字符串 生成二维码
     * @param barCode
     */

    @SneakyThrows
    public static void createQRCode(String barCode) {
        QRCodeUtil.encode(barCode,null,"qrcode","barCode",false);
    }


    public static void main(String[] args) {
        // 生成随机的密钥
//        String secretKey = GoogleAuthenticator.getRandomSecretKey();
//        System.out.println("随机密钥：" + secretKey);
//
//        // 根据验证码，账户，服务商生成 TOPT 密钥的 URI
//        String uri = GoogleAuthenticator.getGoogleAuthenticatorBarCode(secretKey, "747692844@qq.com", "springboot");
//        System.out.println("TOPT密钥URI：" + uri);
//
//        // 根据 TOPT 密钥的 URI生成二维码，存储在本地
//        GoogleAuthenticator.createQRCode(uri);
//
//        String lastCode = null;
//        while (true) {
//            // 根据密钥获取此刻的动态口令
//            String code = GoogleAuthenticator.getTOTPCode(secretKey);
//            if (!code.equals(lastCode)) {
//                System.out.println("刷新了验证码：" + code + " " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
//            }
//            lastCode = code;
//            try {
//                Thread.sleep(1000);  // 线程暂停1秒
//            } catch (InterruptedException e) {};
//        }
        System.out.println(RandomUtil.randomString(32));
    }
}
