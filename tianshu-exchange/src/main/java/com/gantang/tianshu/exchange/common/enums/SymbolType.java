/*
 * Copyright 2019 Maksim Zheravin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gantang.tianshu.exchange.common.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SymbolType {
    /**
     * 货币交易对
     */
    CURRENCY_EXCHANGE_PAIR(0),
    /**
     * 期货合约
     */
    FUTURES_CONTRACT(1),
    /**
     * 期权
     */
    OPTION(2);

    private byte code;

    SymbolType(int code) {
        this.code = (byte) code;
    }

    public static SymbolType of(int code) {
        return Arrays.stream(values())
                .filter(c -> c.code == (byte) code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown SymbolType code: " + code));
    }
}
