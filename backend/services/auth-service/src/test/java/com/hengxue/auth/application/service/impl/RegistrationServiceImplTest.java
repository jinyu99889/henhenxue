package com.hengxue.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

/** 注册用例 Sentinel 保护测试。 */
class RegistrationServiceImplTest {

    /** 验证注册方法声明了独立的 Sentinel 资源。 */
    @Test
    void shouldDeclareSentinelResourceForRegistration() throws NoSuchMethodException {
        SentinelResource annotation = RegistrationServiceImpl.class
                .getMethod("register", RegisterCommand.class, String.class)
                .getAnnotation(SentinelResource.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("auth.registration.submit");
        assertThat(annotation.blockHandler()).isEqualTo("handleRegistrationBlocked");
    }

    /** 验证注册资源被流控时返回稳定的限流错误。 */
    @Test
    void shouldMapRegistrationBlockToRateLimitedError() {
        RegistrationServiceImpl service = new RegistrationServiceImpl();

        assertThatThrownBy(() -> service.handleRegistrationBlocked(
                null, "idempotency-key", new FlowException("auth.registration.submit")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RATE_LIMITED));
    }
}
