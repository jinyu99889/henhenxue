package com.hengxue.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 雪花 ID 的节点与时钟回拨保护配置。 */
@Validated
@ConfigurationProperties(prefix = "hengxue.auth.snowflake")
public record SnowflakeProperties(
        @Min(0) @Max(31) long datacenterId,
        @Min(0) @Max(31) long workerId,
        @Min(0) @Max(5_000) long maxClockRollbackMillis
) {
}
