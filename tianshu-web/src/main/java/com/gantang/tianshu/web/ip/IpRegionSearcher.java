package com.gantang.tianshu.web.ip;

import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 线程安全的 IP 属地查询器。基于字节缓冲区创建的 {@link Searcher} 可并发调用，
 * 每次返回不可变的 {@link IpRegion}，双栈通用。
 *
 * @author gantang
 */
public class IpRegionSearcher implements AutoCloseable {

    private final Searcher searcher;

    public IpRegionSearcher(IpRegionProperties properties) {
        try {
            ClassPathResource resource = new ClassPathResource(properties.getDbFile());
            byte[] bytes;
            try (var in = resource.getInputStream()) {
                bytes = in.readAllBytes();
            }
            this.searcher = Searcher.newWithBuffer(bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException("加载 ip2region 数据文件失败: " + properties.getDbFile(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("初始化 ip2region Searcher 失败", ex);
        }
    }

    /**
     * 查询指定 IP 的属地。
     *
     * @param ip IPv4 地址
     * @return 解析结果，不为 {@code null}
     */
    public IpRegion search(String ip) {
        try {
            return IpRegion.parse(this.searcher.search(ip));
        } catch (Exception ex) {
            throw new IllegalStateException("查询 IP 属地失败: " + ip, ex);
        }
    }

    @Override
    public void close() {
        try {
            this.searcher.close();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
