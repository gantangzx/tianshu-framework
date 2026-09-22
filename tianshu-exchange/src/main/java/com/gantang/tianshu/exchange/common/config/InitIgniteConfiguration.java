package com.gantang.tianshu.exchange.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class InitIgniteConfiguration {

    public static InitIgniteConfiguration DEFAULT = InitIgniteConfiguration.builder()
            .igniteEnabled(false)
            .clientModeEnabled(true)
            .peerClassLoadingEnabled(true)
            .addresses(Collections.singletonList("127.0.0.1:47500..47509"))
            .build();

    private final boolean igniteEnabled;

    private final boolean clientModeEnabled;

    private final boolean peerClassLoadingEnabled;

    private final List<String> addresses;
}
