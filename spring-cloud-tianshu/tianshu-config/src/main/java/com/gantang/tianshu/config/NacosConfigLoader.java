package com.gantang.tianshu.config;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 按 dataId 后缀把 Nacos 配置文本解析为 {@link PropertySource}。
 *
 * <p>支持 {@code .yml/.yaml} 与 {@code .properties}。
 *
 * @author gantang
 */
final class NacosConfigLoader {

    private NacosConfigLoader() {
    }

    /**
     * 解析配置内容。
     *
     * @param dataId  配置 ID（决定解析格式）
     * @param content 配置文本
     * @return 属性源，内容为空时返回 {@code null}
     */
    static PropertySource<?> load(String dataId, String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String lower = dataId.toLowerCase();
        try {
            if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
                ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return dataId;
                    }
                };
                List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(dataId, resource);
                return sources.isEmpty() ? null : sources.get(0);
            }
            if (lower.endsWith(".properties")) {
                return parseProperties(dataId, content);
            }
            // 未知后缀：按默认扩展名约定用 yaml 解析
            ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(dataId, resource);
            return sources.isEmpty() ? null : sources.getFirst();
        } catch (IOException e) {
            throw new IllegalStateException("解析 Nacos 配置失败 dataId=" + dataId, e);
        }
    }

    private static PropertySource<?> parseProperties(String dataId, String content) throws IOException {
        java.util.Properties props = new java.util.Properties();
        props.load(new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return new org.springframework.core.env.PropertiesPropertySource(dataId, props);
    }
}
