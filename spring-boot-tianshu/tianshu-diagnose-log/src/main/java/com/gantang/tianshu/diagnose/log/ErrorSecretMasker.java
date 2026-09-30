package com.gantang.tianshu.diagnose.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 错误信息脱敏：在出进程前对消息 / 堆栈 / MDC 中的常见敏感片段做掩码。
 *
 * <p>纯逻辑，不依赖 Spring / Logback。任何正则处理异常都回退原文，保证不会因畸形输入打挂采集。
 */
public class ErrorSecretMasker {

    private static final String MASK = "***";

    // 中国大陆手机号（避免误伤：要求非数字边界）
    private static final Pattern PHONE = Pattern.compile("(?<![0-9])1[3-9]\\d{9}(?![0-9])");
    // 身份证（18 位，末位 X）
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<![0-9A-Za-z])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?![0-9A-Za-z])");
    // 银行卡：13–19 位连续数字
    private static final Pattern BANK_CARD = Pattern.compile("(?<![0-9])[1-9]\\d{12,18}(?![0-9])");
    // 邮箱
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    // API 密钥前缀
    private static final Pattern API_KEY = Pattern.compile(
            "(?<![A-Za-z0-9])(?:sk|ark|pk|rk)-[A-Za-z0-9._-]{8,}");
    // key=value 型凭据（值至少 1 位，到空白/&/引号结束）
    private static final Pattern KV_SECRET = Pattern.compile(
            "((?:password|passwd|pwd|token|secret|access[_-]?key|secret[_-]?key)\\s*[=:]\\s*)"
                    + "[^\\s&\"']+", Pattern.CASE_INSENSITIVE);
    // SQL 字符串字面量（保留首尾引号，仅掩码内容）
    private static final Pattern SQL_STRING = Pattern.compile("'[^']*'");

    /**
     * 对文本脱敏；入参为 null 返回 null，任何异常回退原文。
     */
    public String mask(String input) {
        if (input == null) {
            return null;
        }
        try {
            String s = input;
            s = replace(EMAIL, s, MASK);
            s = replace(ID_CARD, s, MASK);
            s = replace(PHONE, s, MASK);
            s = replace(BANK_CARD, s, MASK);
            s = replace(API_KEY, s, MASK);
            s = KV_SECRET.matcher(s).replaceAll("$1" + MASK);
            s = replace(SQL_STRING, s, "'" + MASK + "'");
            return s;
        } catch (RuntimeException e) {
            return input;
        }
    }

    private String replace(Pattern p, String input, String replacement) {
        Matcher m = p.matcher(input);
        return m.replaceAll(Matcher.quoteReplacement(replacement));
    }
}
