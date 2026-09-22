package com.gantang.tianshu.web.captcha.reactive;

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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Reactive 验证码端点。验证码生成是阻塞、CPU 密集型操作，下沉到 boundedElastic 调度器。
 *
 * @author gantang
 */
@RestController
@RequestMapping("${tianshu.captcha.reactive-path:/captcha/image}")
public class ReactiveCaptchaController {

    private static final String KEY_HEADER = "x-captcha-key";

    private final CaptchaService captchaService;

    public ReactiveCaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    public Mono<ResponseEntity<?>> image(@RequestParam(name = "base64", required = false,
            defaultValue = "false") boolean base64) {
        return Mono.fromCallable(this.captchaService::generate)
                .subscribeOn(Schedulers.boundedElastic())
                .map(image -> toResponse(image, base64));
    }

    private ResponseEntity<?> toResponse(CaptchaImage image, boolean base64) {
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
