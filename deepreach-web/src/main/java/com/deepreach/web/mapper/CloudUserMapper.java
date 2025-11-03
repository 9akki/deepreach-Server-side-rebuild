package com.deepreach.web.mapper;

import com.deepreach.common.core.domain.entity.CloudUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 云用户Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Mapper
public interface CloudUserMapper {

    /**
     * 根据ID查询云用户
     *
     * @param id 用户ID
     * @return 云用户对象
     */
    CloudUser selectById(@Param("id") Long id);

    /**
     * 根据终端用户ID查询云用户
     *
     * @param endUserId 终端用户ID
     * @return 云用户对象
     */
    CloudUser selectByEndUserId(@Param("endUserId") String endUserId);

    /**
     * 根据客户端用户名查询云用户
     *
     * @param clientUsername 客户端用户名
     * @return 云用户对象
     */
    CloudUser selectByClientUsername(@Param("clientUsername") String clientUsername);

    /**
     * 查询所有云用户
     *
     * @return 云用户列表
     */
    List<CloudUser> selectAll();

    /**
     * 根据条件查询云用户列表
     *
     * @param cloudUser 查询条件
     * @return 云用户列表
     */
    List<CloudUser> selectByCondition(CloudUser cloudUser);

    /**
     * 插入云用户
     *
     * @param cloudUser 云用户对象
     * @return 影响行数
     */
    int insert(CloudUser cloudUser);

    /**
     * 更新云用户
     *
     * @param cloudUser 云用户对象
     * @return 影响行数
     */
    int updateById(CloudUser cloudUser);

    /**
     * 根据ID删除云用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查终端用户ID是否唯一
     *
     * @param endUserId 终端用户ID
     * @param id 排除的用户ID
     * @return 记录数
     */
    int checkEndUserIdUnique(@Param("endUserId") String endUserId, @Param("id") Long id);

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}