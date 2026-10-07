package com.gantang.tianshu.diagnose.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 错误指纹：同一类错误（异常类型 + 首个业务落点，或归一化文本）映射到稳定指纹，
 * 屏蔽对象 ID / 行号以外的动态差异，供服务端按指纹去重聚合。
 *
 * <p>纯逻辑，不依赖 Spring / Logback。
 */
public class ErrorFingerprinter {

    /** 默认业务包前缀。 */
    public static final String DEFAULT_BUSINESS_PACKAGE = "com.gantang";

    private static final String[] FRAMEWORK_PREFIXES = {
            "java.", "jdk.", "sun.", "reactor.", "org.springframework.", "org.apache."
    };

    private final List<String> businessPackages;

    public ErrorFingerprinter() {
        this(List.of(DEFAULT_BUSINESS_PACKAGE));
    }

    /**
     * @param businessPackages 业务包前缀，命中即视为业务帧；为空时退化为默认前缀
     */
    public ErrorFingerprinter(List<String> businessPackages) {
        this.businessPackages = (businessPackages == null || businessPackages.isEmpty())
                ? List.of(DEFAULT_BUSINESS_PACKAGE) : List.copyOf(businessPackages);
    }

    /**
     * 计算指纹。
     *
     * @param exceptionClass 异常全限定名，可为空
     * @param stackFrames    堆栈帧（"FQN.method(File:line)" 或 Logback 帧 toString），可为空
     * @param errorCode      业务错误码，可为空
     * @param loggerName     无异常时的 logger 名
     * @param message        无异常时的原始消息
     * @return 32 位小写 md5 hex
     */
    public String fingerprint(String exceptionClass, List<String> stackFrames, String errorCode,
                              String loggerName, String message) {
        if (exceptionClass != null && !exceptionClass.isBlank()) {
            String frame = firstBusinessFrame(stackFrames);
            return md5Hex(exceptionClass + "|" + safe(frame) + "|" + safe(errorCode));
        }
        return md5Hex(safe(loggerName) + "|" + normalize(message));
    }

    /**
     * 选首个业务帧：优先命中业务包前缀；若无，退化为首个非框架（非 JDK/Spring/Apache…）帧。
     */
    String firstBusinessFrame(List<String> stackFrames) {
        if (stackFrames == null) {
            return "";
        }
        String firstNonFramework = null;
        for (String frame : stackFrames) {
            if (frame == null || frame.isBlank()) {
                continue;
            }
            String trimmed = frame.trim();
            // 仅认可真正的堆栈帧（含“(”定位）；跳过异常头行“类名: message”与
            // “Caused by: ...”，它们描述异常层级而非代码落点。
            if (!trimmed.contains("(") || trimmed.startsWith("Caused by:")) {
                continue;
            }
            boolean business = false;
            for (String pkg : businessPackages) {
                if (pkg != null && frameStartsWith(trimmed, pkg)) {
                    business = true;
                    break;
                }
            }
            if (business) {
                return stripLineNumber(trimmed);
            }
            if (firstNonFramework == null && !isFrameworkFrame(trimmed)) {
                firstNonFramework = stripLineNumber(trimmed);
            }
        }
        return firstNonFramework == null ? "" : firstNonFramework;
    }

    private boolean frameStartsWith(String frame, String packagePrefix) {
        String dotted = packagePrefix.endsWith(".") ? packagePrefix : packagePrefix + ".";
        return frame.startsWith(dotted) || frame.startsWith(packagePrefix);
    }

    private boolean isFrameworkFrame(String frame) {
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (frame.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 去掉行号，避免行号因细微改动漂移导致同类错误指纹变化。
     * 形如 {@code com.gantang.Foo.bar(Foo.java:42)} → {@code com.gantang.Foo.bar(Foo.java)}。
     */
    private String stripLineNumber(String frame) {
        int idx = frame.lastIndexOf(':');
        int close = frame.lastIndexOf(')');
        if (idx > 0 && close > idx) {
            String maybeNum = frame.substring(idx + 1, close);
            if (!maybeNum.isBlank() && maybeNum.chars().allMatch(Character::isDigit)) {
                return frame.substring(0, idx) + frame.substring(close);
            }
        }
        return frame;
    }

    /**
     * 归一化纯文本消息：去除数字、UUID、多余空白差异，使同模板动态消息归为一类。
     */
    String normalize(String message) {
        if (message == null) {
            return "";
        }
        String s = message;
        s = s.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "<uuid>");
        s = s.replaceAll("\\d+", "<n>");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
