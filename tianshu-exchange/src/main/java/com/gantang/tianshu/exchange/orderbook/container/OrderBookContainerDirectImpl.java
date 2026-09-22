package com.gantang.tianshu.exchange.orderbook.container;

import com.gantang.tianshu.exchange.common.CoreSymbolSpecification;
import com.gantang.tianshu.exchange.common.IOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

@Slf4j
public class OrderBookContainerDirectImpl implements IOrderBookContainer{

    private final Ignite ignite;
    private final CoreSymbolService coreSymbolService;
    private final String symbol;

    private IgniteCache<?,?> igniteCache;

    public OrderBookContainerDirectImpl(final Ignite ignite,final CoreSymbolService coreSymbolService,final CoreSymbolSpecification symbolSpec) {
        this.ignite = ignite;
        this.coreSymbolService = coreSymbolService;
        this.symbol = coreSymbolService.getSymbol(symbolSpec.symbolId);
        this.igniteCache = ignite.getOrCreateCache(this.symbol);
    }

    @Override
    public void addOrder(IOrder order) {

    }

    @Override
    public void remover(IOrder order) {

    }
}
