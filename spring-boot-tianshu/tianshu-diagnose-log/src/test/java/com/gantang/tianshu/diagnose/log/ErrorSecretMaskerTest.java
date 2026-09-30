package com.gantang.tianshu.diagnose.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorSecretMaskerTest {

    private final ErrorSecretMasker masker = new ErrorSecretMasker();

    @Test
    void masksPhone() {
        String out = masker.mask("call 13812345678 now");
        assertThat(out).doesNotContain("13812345678").contains("***");
    }

    @Test
    void masksIdCard() {
        String id = "11010119900307889X";
        String out = masker.mask("id=" + id);
        assertThat(out).doesNotContain(id);
    }

    @Test
    void masksBankCard() {
        String card = "6222021234567890123";
        String out = masker.mask("card " + card);
        assertThat(out).doesNotContain(card);
    }

    @Test
    void masksEmail() {
        String out = masker.mask("mail to user.name@example.com please");
        assertThat(out).doesNotContain("user.name@example.com");
    }

    @Test
    void masksApiKeys() {
        assertThat(masker.mask("key sk-abcdefgh12345678")).doesNotContain("sk-abcdefgh12345678");
        assertThat(masker.mask("ark-7d95abcd-1234-abcd-5678-abcdef004be")).doesNotContain("004be");
    }

    @Test
    void masksKeyValueSecrets() {
        String out = masker.mask("password=hunter2 token: abc.def-ghi");
        assertThat(out).doesNotContain("hunter2").doesNotContain("abc.def-ghi");
    }

    @Test
    void masksSqlStringLiteral() {
        String out = masker.mask("where name = 'zhangsan'");
        assertThat(out).doesNotContain("zhangsan");
    }

    @Test
    void nullSafeAndNoThrowOnWeirdInput() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask("")).isEmpty();
        assertThat(masker.mask("正常文本 no secrets")).isEqualTo("正常文本 no secrets");
    }
}
