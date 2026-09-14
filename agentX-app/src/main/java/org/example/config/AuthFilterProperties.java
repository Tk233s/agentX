package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器配置
 */
@Data
@ConfigurationProperties(prefix = "auth.filter")
public class AuthFilterProperties {

    /** 是否启用认证过滤器 */
    private boolean enabled = true;

    /** 不校验Token的路径 */
    private List<String> excludePaths = new ArrayList<>(Arrays.asList("/auth/login"));
}
