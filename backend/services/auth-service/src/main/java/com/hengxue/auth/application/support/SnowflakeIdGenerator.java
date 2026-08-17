package com.hengxue.auth.application.support;

import com.hengxue.auth.config.SnowflakeProperties;
import java.time.Clock;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 使用雪花算法生成分布式唯一标识。
 *
 * <p>生成结果保留为 26 位十进制字符串，以兼容现有 {@code CHAR(26)} 数据库列与 HTTP 字符串 ID 契约。
 * 数值本体仍是标准的 64 位雪花 ID。</p>
 */
@Component
public class SnowflakeIdGenerator {

    /** 自定义纪元：2025-01-01T00:00:00Z。 */
    private static final long CUSTOM_EPOCH_MILLIS = 1_735_689_600_000L;

    private static final long MAX_DATACENTER_ID = 31L;
    private static final long MAX_WORKER_ID = 31L;
    private static final long SEQUENCE_MASK = 4_095L;
    private static final int WORKER_ID_SHIFT = 12;
    private static final int DATACENTER_ID_SHIFT = 17;
    private static final int TIMESTAMP_SHIFT = 22;

    @Autowired
    private Clock clock;

    @Autowired
    private SnowflakeProperties properties;

    private long sequence;
    private long lastTimestamp = -1L;

    /**
     * 生成一个新的雪花 ID。
     *
     * @return 左侧补零后的 26 位十进制雪花 ID
     */
    public synchronized String next() {
        long timestamp = currentTimestamp();
        validateTimestamp(timestamp);

        if (timestamp < lastTimestamp) {
            timestamp = recoverFromClockRollback(timestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        long id = ((timestamp - CUSTOM_EPOCH_MILLIS) << TIMESTAMP_SHIFT)
                | (properties.datacenterId() << DATACENTER_ID_SHIFT)
                | (properties.workerId() << WORKER_ID_SHIFT)
                | sequence;
        return String.format(Locale.ROOT, "%026d", id);
    }

    /**
     * 处理系统时钟回拨。
     *
     * @param currentTimestamp 当前读取到的毫秒时间戳
     * @return 恢复到不早于上次时间戳的毫秒时间戳
     */
    private long recoverFromClockRollback(long currentTimestamp) {
        long rollbackMillis = lastTimestamp - currentTimestamp;
        if (rollbackMillis > properties.maxClockRollbackMillis()) {
            throw new IllegalStateException("系统时钟回拨超过允许阈值，拒绝生成雪花 ID");
        }

        // 小范围回拨时暂停至上次时间戳，防止复用相同的时间位、节点位与序列位组合。
        try {
            Thread.sleep(rollbackMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待系统时钟恢复时被中断", exception);
        }

        long recoveredTimestamp = currentTimestamp();
        if (recoveredTimestamp < lastTimestamp) {
            throw new IllegalStateException("系统时钟未在允许时间内恢复，拒绝生成雪花 ID");
        }
        return recoveredTimestamp;
    }

    /**
     * 等待进入下一毫秒，避免同一节点在单毫秒内生成超过 4096 个 ID 后发生碰撞。
     *
     * @param timestamp 已耗尽序列号的毫秒时间戳
     * @return 大于传入时间戳的当前毫秒时间戳
     */
    private long waitForNextMillis(long timestamp) {
        long nextTimestamp = currentTimestamp();
        while (nextTimestamp <= timestamp) {
            Thread.onSpinWait();
            nextTimestamp = currentTimestamp();
        }
        return nextTimestamp;
    }

    /**
     * 验证当前时间与节点配置是否处于可编码范围。
     *
     * @param timestamp 当前毫秒时间戳
     */
    private void validateTimestamp(long timestamp) {
        if (timestamp < CUSTOM_EPOCH_MILLIS) {
            throw new IllegalStateException("当前时间早于雪花算法纪元，无法生成 ID");
        }
        if (properties.datacenterId() > MAX_DATACENTER_ID || properties.workerId() > MAX_WORKER_ID) {
            throw new IllegalStateException("雪花算法数据中心或工作节点编号超出范围");
        }
    }

    /**
     * 读取当前毫秒时间戳。
     *
     * @return 当前毫秒时间戳
     */
    private long currentTimestamp() {
        return clock.millis();
    }
}
