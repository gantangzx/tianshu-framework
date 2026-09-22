package com.gantang.tianshu.exchange.common.enums;

import lombok.Getter;

@Getter
public enum PositionAction {
    /**
     * 开仓
     */
    OPEN(1),
    /**
     * 平仓
     */
    CLOSE(0);
    private final byte code;


    PositionAction(int code) {
        this.code = (byte) code;
    }

    public static PositionAction of(byte code) {
        switch (code) {
            case 0:
                return CLOSE;
            case 1:
                return OPEN;
            default:
                throw new IllegalArgumentException("unknown OrderAction:" + code);
        }
    }


    public PositionAction opposite() {
        return this == OPEN ? OPEN : CLOSE;
    }
}
