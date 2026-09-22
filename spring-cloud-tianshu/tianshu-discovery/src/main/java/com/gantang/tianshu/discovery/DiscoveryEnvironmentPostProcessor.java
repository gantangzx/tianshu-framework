package com.gantang.tianshu.discovery;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

/**
 * 以最低优先级加载 {@code discovery-defaults.yml}，用户配置可覆盖。
 *
 * @author gantang
 */
public class DiscoveryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULTS_RESOURCE = "discovery-defaults.yml";
    private static final String SOURCE_NAME = "tianshuDiscoveryDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ClassPathResource resource = new ClassPathResource(DEFAULTS_RESOURCE);
        if (!resource.exists()) {
            return;
        }
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(SOURCE_NAME, resource);
            sources.forEach(source -> environment.getPropertySources().addLast(source));
        } catch (IOException ex) {
            throw new IllegalStateException("加载 " + DEFAULTS_RESOURCE + " 失败", ex);
        }
    }
}
