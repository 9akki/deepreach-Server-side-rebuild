package com.deepreach.web.service;

import com.deepreach.web.domain.dto.CloudComputerData;

/**
 * 云电脑服务接口
 *
 * @author DeepReach Team
 */
public interface CloudComputerService {

    /**
     * 获取云电脑登录令牌
     *
     * @return 云电脑数据
     */
    CloudComputerData getLoginToken();

    /**
     * 获取指定用户的云电脑登录令牌
     *
     * @param endUserId 最终用户ID
     * @param password 密码
     * @return 云电脑数据
     */
    CloudComputerData getLoginToken(String endUserId, String password);

    /**
     * 获取指定用户的云电脑登录令牌（带参数）
     *
     * @param endUserId 最终用户ID
     * @param password 密码
     * @return 云电脑数据
     */
    CloudComputerData getLoginTokenWithParams(String endUserId, String password);
}