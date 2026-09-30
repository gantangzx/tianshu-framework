package com.gantang.tianshu.diagnose.log;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorFingerprinterTest {

    private final ErrorFingerprinter fingerprinter = new ErrorFingerprinter();

    private List<String> frames(int line) {
        return List.of(
                "org.springframework.web.method.support.InvocableException.doInvoke(InvocableException.java:188)",
                "com.gantang.demo.service.OrderService.create(OrderService.java:" + line + ")",
                "com.gantang.demo.web.OrderController.post(OrderController.java:30)");
    }

    @Test
    void sameExceptionDifferentObjectIdAndLine_sameFingerprint() {
        String a = fingerprinter.fingerprint("java.lang.NullPointerException",
                frames(42), null, null, null);
        String b = fingerprinter.fingerprint("java.lang.NullPointerException",
                frames(99), null, null, null);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentBusinessFrame_differentFingerprint() {
        List<String> f1 = List.of("com.gantang.demo.A.a(A.java:10)");
        List<String> f2 = List.of("com.gantang.demo.B.b(B.java:10)");
        String a = fingerprinter.fingerprint("java.lang.IllegalStateException", f1, null, null, null);
        String b = fingerprinter.fingerprint("java.lang.IllegalStateException", f2, null, null, null);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentExceptionClass_differentFingerprint() {
        String a = fingerprinter.fingerprint("java.lang.NullPointerException", frames(1), null, null, null);
        String b = fingerprinter.fingerprint("java.lang.IllegalArgumentException", frames(1), null, null, null);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentErrorCode_differentFingerprint() {
        String a = fingerprinter.fingerprint("com.gantang.ServiceException", frames(1), "E1001", null, null);
        String b = fingerprinter.fingerprint("com.gantang.ServiceException", frames(1), "E1002", null, null);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void noBusinessFrame_fallsBackToFirstNonFrameworkFrame() {
        List<String> frames = List.of(
                "java.base/java.lang.String.length(String.java:123)",
                "reactor.core.publisher.MonoSubscriber.block(MonoSubscriber.java:55)",
                "com.thirdparty.Thing.call(Thing.java:8)");
        String frame = fingerprinter.firstBusinessFrame(frames);
        assertThat(frame).startsWith("com.thirdparty.Thing.call");
        assertThat(frame).doesNotContain(":8");
    }

    @Test
    void noFrames_doesNotFail() {
        String fp = fingerprinter.fingerprint("java.lang.NPE", List.of(), null, null, null);
        assertThat(fp).hasSize(32);
    }

    @Test
    void textOnly_normalizesNumbersAndUuid() {
        String a = fingerprinter.fingerprint(null, null, null, "com.gantang.Job",
                "processed 123 records for 550e8400-e29b-41d4-a716-446655440000");
        String b = fingerprinter.fingerprint(null, null, null, "com.gantang.Job",
                "processed 9 records for 6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void normalize_collapsesWhitespace() {
        assertThat(fingerprinter.normalize("a    b\tc")).isEqualTo("a b c");
    }
}
