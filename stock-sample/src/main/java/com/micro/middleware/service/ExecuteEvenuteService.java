package com.micro.middleware.service;


import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock ExecuteEvenuteService Interface
 */
public interface ExecuteEvenuteService {

    /**
     * 对TradeRequest进行业务处理,接收一个TradeRequest请求输出,得到一个返回输出TradeResponse.
     * @param request
     * @return
     */
    TradeResponse executeTradeRequest(TradeRequest request);
}
