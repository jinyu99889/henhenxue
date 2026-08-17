package com.hengxue.auth.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hengxue.auth.config.SnowflakeProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 雪花 ID 生成器测试。 */
class SnowflakeIdGeneratorTest {

    private static final long EPOCH_MILLIS = 1_735_689_600_000L;

    /** 验证同一毫秒内生成的 ID 唯一且兼容既有字符串长度。 */
    @Test
    void shouldGenerateUniqueIdsWithinSameMillis() {
        SnowflakeIdGenerator generator = generator(new FixedClock(EPOCH_MILLIS + 1_000), 5);
        Set<String> ids = new HashSet<>();

        for (int index = 0; index < 1_000; index++) {
            ids.add(generator.next());
        }

        assertThat(ids).hasSize(1_000);
        assertThat(ids).allMatch(id -> id.matches("^\\d{26}$"));
    }

    /** 验证短暂时钟回拨会等待时钟恢复后继续生成。 */
    @Test
    void shouldRecoverFromShortClockRollback() {
        SnowflakeIdGenerator generator = generator(
                new SequenceClock(EPOCH_MILLIS + 100, EPOCH_MILLIS + 99, EPOCH_MILLIS + 100), 5);

        String firstId = generator.next();
        String recoveredId = generator.next();

        assertThat(recoveredId).isGreaterThan(firstId);
    }

    /** 验证超过阈值的时钟回拨会拒绝生成，避免重复 ID。 */
    @Test
    void shouldRejectExcessiveClockRollback() {
        SnowflakeIdGenerator generator = generator(
                new SequenceClock(EPOCH_MILLIS + 100, EPOCH_MILLIS + 90), 5);
        generator.next();

        assertThatThrownBy(generator::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("时钟回拨超过允许阈值");
    }

    /**
     * 创建测试用生成器。
     *
     * @param clock 测试时钟
     * @param rollbackLimitMillis 最大允许回拨毫秒数
     * @return 已注入依赖的生成器
     */
    private SnowflakeIdGenerator generator(Clock clock, long rollbackLimitMillis) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        ReflectionTestUtils.setField(generator, "clock", clock);
        ReflectionTestUtils.setField(generator, "properties", new SnowflakeProperties(1, 2, rollbackLimitMillis));
        return generator;
    }

    /** 固定返回同一毫秒的测试时钟。 */
    private static final class FixedClock extends Clock {

        private final long millis;

        private FixedClock(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }

    /** 按顺序返回毫秒时间戳的测试时钟。 */
    private static final class SequenceClock extends Clock {

        private final Queue<Long> timestamps = new ArrayDeque<>();
        private long lastTimestamp;

        private SequenceClock(long... timestamps) {
            for (long timestamp : timestamps) {
                this.timestamps.add(timestamp);
                this.lastTimestamp = timestamp;
            }
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            Long timestamp = timestamps.poll();
            if (timestamp != null) {
                lastTimestamp = timestamp;
            }
            return Instant.ofEpochMilli(lastTimestamp);
        }
    }
}
