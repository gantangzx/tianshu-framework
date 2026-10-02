package com.gantang.tianshu.nacos;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

/**
 * 将内置 {@code nacos-defaults.yml} 以最低优先级加入环境，
 * 用户自身配置优先级更高，可任意覆盖。
 *
 * @author gantang
 */
public class NacosEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULTS_RESOURCE = "nacos-defaults.yml";
    private static final String SOURCE_NAME = "tianshuNacosDefaults";

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
