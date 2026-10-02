package com.gantang.tianshu.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.Properties;

/**
 * 封装 Nacos 官方 {@link NamingService} 的生命周期与注册/查询操作。
 *
 * <p>仅使用官方 nacos-client，不引入任何 SCA 组件。
 *
 * @author gantang
 */
public class NacosServiceManager implements DisposableBean {

    private final NacosDiscoveryProperties properties;
    private volatile NamingService namingService;

    public NacosServiceManager(NacosDiscoveryProperties properties) {
        this.properties = properties;
    }

    /** 懒创建并缓存 NamingService。 */
    public NamingService namingService() {
        if (namingService == null) {
            synchronized (this) {
                if (namingService == null) {
                    try {
                        namingService = NamingFactory.createNamingService(buildProperties());
                    } catch (NacosException e) {
                        throw new IllegalStateException("创建 Nacos NamingService 失败: " + e.getMessage(), e);
                    }
                }
            }
        }
        return namingService;
    }

    private Properties buildProperties() {
        Properties p = new Properties();
        p.setProperty("serverAddr", properties.getServerAddr());
        if (hasText(properties.getNamespace())) {
            p.setProperty("namespace", properties.getNamespace());
        }
        if (hasText(properties.getUsername())) {
            p.setProperty("username", properties.getUsername());
            p.setProperty("password", properties.getPassword());
        }
        return p;
    }

    /** 构造并注册当前实例。 */
    public void register(NacosRegistration registration) {
        Instance instance = new Instance();
        instance.setIp(registration.getHost());
        instance.setPort(registration.getPort());
        instance.setWeight(properties.getWeight());
        instance.setEphemeral(properties.isEphemeral());
        instance.setClusterName(properties.getClusterName());
        instance.setMetadata(registration.getMetadata());
        instance.setHealthy(true);
        instance.setEnabled(true);
        try {
            namingService().registerInstance(registration.getServiceId(), properties.getGroup(), instance);
        } catch (NacosException e) {
            throw new IllegalStateException("注册到 Nacos 失败: " + e.getMessage(), e);
        }
    }

    /** 注销当前实例。 */
    public void deregister(NacosRegistration registration) {
        Instance instance = new Instance();
        instance.setIp(registration.getHost());
        instance.setPort(registration.getPort());
        instance.setClusterName(properties.getClusterName());
        instance.setEphemeral(properties.isEphemeral());
        try {
            namingService().deregisterInstance(registration.getServiceId(), properties.getGroup(), instance);
        } catch (NacosException e) {
            throw new IllegalStateException("从 Nacos 注销失败: " + e.getMessage(), e);
        }
    }

    /** 查询某服务的全部实例。 */
    public List<Instance> getAllInstances(String serviceId) {
        try {
            return namingService().getAllInstances(serviceId, properties.getGroup());
        } catch (NacosException e) {
            throw new IllegalStateException("查询 Nacos 实例失败: " + e.getMessage(), e);
        }
    }

    /** 分页查询所有服务名。 */
    public List<String> getServices() {
        try {
            return namingService().getServicesOfServer(1, Integer.MAX_VALUE).getData();
        } catch (NacosException e) {
            throw new IllegalStateException("查询 Nacos 服务列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (namingService != null) {
            try {
                namingService.shutDown();
            } catch (NacosException ignored) {
                // 关闭时忽略
            }
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
