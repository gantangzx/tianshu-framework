package com.gantang.tianshu.exchange.orderbook.container;

import com.gantang.tianshu.exchange.common.IOrder;
import com.gantang.tianshu.exchange.common.cmd.OrderCommand;

public interface IOrderBookContainer {

    void addOrder(IOrder order);

    void remover(IOrder order);
}
