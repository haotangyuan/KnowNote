package dev.haotangyuan.knownote.research.framework;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceResponseTest {

    @Test
    void ok_hasStatusOkAndContent() {
        ServiceResponse<String> r = ServiceResponse.ok("result");
        assertThat(r.status()).isEqualTo(ServiceResponse.Status.OK);
        assertThat(r.content()).isEqualTo("result");
        assertThat(r.errorMessage()).isNull();
        assertThat(r.isOk()).isTrue();
        assertThat(r.isError()).isFalse();
        assertThat(r.isQuotaExceeded()).isFalse();
    }

    @Test
    void error_hasStatusErrorAndMessage() {
        ServiceResponse<String> r = ServiceResponse.error("something failed");
        assertThat(r.status()).isEqualTo(ServiceResponse.Status.ERROR);
        assertThat(r.content()).isNull();
        assertThat(r.errorMessage()).isEqualTo("something failed");
        assertThat(r.isOk()).isFalse();
        assertThat(r.isError()).isTrue();
        assertThat(r.isQuotaExceeded()).isFalse();
    }

    @Test
    void quotaExceeded_hasStatusQuotaExceeded() {
        ServiceResponse<Integer> r = ServiceResponse.quotaExceeded("limit reached");
        assertThat(r.status()).isEqualTo(ServiceResponse.Status.QUOTA_EXCEEDED);
        assertThat(r.isQuotaExceeded()).isTrue();
        assertThat(r.errorMessage()).isEqualTo("limit reached");
        assertThat(r.isOk()).isFalse();
        assertThat(r.isError()).isFalse();
    }
}
