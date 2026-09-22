package com.gantang.tianshu.exchange;

public interface IEventsMatchCache {


    void putMatchEvent(IEventsHandler.TradeEvent tradeEvent);


    void removeMatchEvent(IEventsHandler.TradeEvent tradeEvent);
}
