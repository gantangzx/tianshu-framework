package com.gantang.tianshu.gateway.doc;

/**
 * 单个服务的文档信息。
 *
 * @param name 服务名
 * @param url  该服务在网关下的 OpenAPI 地址（相对路径）
 * @author gantang
 */
public record ServiceDoc(String name, String url) {
}
