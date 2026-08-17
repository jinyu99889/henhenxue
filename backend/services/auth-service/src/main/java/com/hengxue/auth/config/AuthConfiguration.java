package com.hengxue.auth.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 身份服务的安全与邮件配置。 */
@Configuration
@EnableConfigurationProperties({
        AuthMailProperties.class,
        EmailCodeProperties.class,
        SnowflakeProperties.class,
        TrustedProxyProperties.class,
        AuthLoginProperties.class
})
public class AuthConfiguration {

    /**
     * 创建密码哈希器。
     *
     * @return BCrypt 密码哈希器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 创建供时间敏感组件使用的 UTC 时钟。
     *
     * @return 系统 UTC 时钟
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 创建注册验证码邮件投递线程池。
     *
     * @return 使用有界队列的邮件投递执行器
     */
    @Bean("registrationEmailTaskExecutor")
    public ThreadPoolTaskExecutor registrationEmailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("registration-email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
