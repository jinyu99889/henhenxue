package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

/** 统一幂等记录表的 MyBatis Mapper。 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {
}
