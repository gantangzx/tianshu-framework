package com.gantang.tianshu.web.captcha.servlet;

import com.gantang.tianshu.web.captcha.CaptchaImage;
import com.gantang.tianshu.web.captcha.CaptchaService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Servlet 验证码端点。验证码标识通过 {@code x-captcha-key} 响应头返回。
 *
 * @author gantang
 */
@RestController
@RequestMapping("${tianshu.captcha.servlet-path:/captcha/image}")
public class ServletCaptchaController {

    private static final String KEY_HEADER = "x-captcha-key";

    private final CaptchaService captchaService;

    public ServletCaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    public ResponseEntity<?> image(@RequestParam(name = "base64", required = false,
            defaultValue = "false") boolean base64) {
        CaptchaImage image = this.captchaService.generate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(KEY_HEADER, image.key());
        headers.setCacheControl(CacheControl.noStore());
        headers.setPragma("no-cache");

        if (base64) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.ok().headers(headers)
                    .body(Map.of("key", image.key(), "image", image.base64()));
        }
        headers.setContentType(image.gif() ? MediaType.IMAGE_GIF : MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(headers).body(image.image());
    }
}
