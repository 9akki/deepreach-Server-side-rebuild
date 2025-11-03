package com.deepreach.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云ECD配置
 *
 * @author DeepReach Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.ecd")
public class AliyunEcdConfig {

    /**
     * AccessKeyId
     */
    private String accessKeyId;

    /**
     * AccessKeySecret
     */
    private String accessKeySecret;

    /**
     * 服务端点
     */
    private String endpoint = "ecd.us-west-1.aliyuncs.com";

    /**
     * 区域ID
     */
    private String regionId = "us-west-1";

    /**
     * 客户端ID
     */
    private String clientId = "123456789000";

    /**
     * 办公网络ID
     */
    private String officeSiteId = "us-west-1+dir-5588339126";

    /**
     * 最终用户ID
     */
    private String endUserId = "hq01";

    /**
     * 密码
     */
    private String password = "Hq123456789";
}