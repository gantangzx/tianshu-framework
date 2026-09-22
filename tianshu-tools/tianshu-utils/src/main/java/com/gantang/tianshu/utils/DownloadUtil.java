package com.gantang.tianshu.utils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @ClassName DownloadUtil
 * @Author huyaxi
 * @DATE 2020/4/10 10:08
 */
public class DownloadUtil implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }


    /**
     * 从网络加载输入流
     * @param urlStr 网络地址
     * @param isHttps 是否是Https请求
     * @return
     * @throws Exception
     */
    public static InputStream downLoadFromUrl(String urlStr, Boolean isHttps) throws Exception {
        if(isHttps){
            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = { new DownloadUtil() };
            // 初始化
            sslContext.init(null, tm, new java.security.SecureRandom());
            // 获取SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            // url对象（标准 API，不依赖 JDK 内部 Handler）
            URL url = URI.create(urlStr).toURL();
            // 打开连接
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            /**
             * 这一步的原因: 当访问HTTPS的网址。您可能已经安装了服务器证书到您的JRE的keystore
             * 但是服务器的名称与证书实际域名不相等。这通常发生在你使用的是非标准网上签发的证书。
             *
             * 解决方法：让JRE相信所有的证书和对系统的域名和证书域名。
             *
             * 如果少了这一步会报错:java.io.IOException: HTTPS hostname wrong: should be <localhost>
             */
            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
            // 设置一些参数
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            // 设置当前实例使用的SSLSoctetFactory
            conn.setSSLSocketFactory(ssf);
            conn.connect();
            return conn.getInputStream();
        } else {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置超时间为3秒
            conn.setConnectTimeout(3 * 1000);
            // 防止屏蔽程序抓取而返回403错误
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            conn.connect();
            // 得到输入流
            return conn.getInputStream();
        }
    }


    static class TrustAnyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }
}
