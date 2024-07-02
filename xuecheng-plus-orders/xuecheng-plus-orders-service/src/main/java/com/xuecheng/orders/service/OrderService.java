package com.xuecheng.orders.service;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;

public interface OrderService {

    /**
     * @description 创建商品订单
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录(包括二维码)
     * @author Mr.M
     * @date 2022/10/4 11:02
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);
}
