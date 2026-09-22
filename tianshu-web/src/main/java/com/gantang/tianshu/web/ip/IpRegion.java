package com.gantang.tianshu.web.ip;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * IP 属地解析的不可变结果。ip2region 数据以 {@code |} 分隔为
 * {@code 国家|区域|省份|城市|运营商}。
 *
 * @author gantang
 */
public record IpRegion(String country, String region, String province, String city, String isp) {

    static final IpRegion EMPTY = new IpRegion("", "", "", "", "");

    static IpRegion parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return EMPTY;
        }
        List<String> parts = List.of(StringUtils.delimitedListToStringArray(raw, "|"));
        return new IpRegion(get(parts, 0), get(parts, 1), get(parts, 2), get(parts, 3), get(parts, 4));
    }

    private static String get(List<String> parts, int index) {
        return index < parts.size() ? parts.get(index) : "";
    }
}
