package com.fanyu.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanyu.example.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper接口
 *
 * @author fanyu
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
