package com.deepreach.web.mapper;

import com.deepreach.common.core.domain.entity.UserComputer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户电脑关联Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Mapper
public interface UserComputerMapper {

    /**
     * 根据ID查询用户电脑关联
     *
     * @param id 关联ID
     * @return 用户电脑关联对象
     */
    UserComputer selectById(@Param("id") Long id);

    /**
     * 根据终端用户ID查询用户电脑关联列表
     *
     * @param endUserId 终端用户ID
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectByEndUserId(@Param("endUserId") String endUserId);

    /**
     * 根据电脑ID查询用户电脑关联列表
     *
     * @param computerId 电脑ID
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectByComputerId(@Param("computerId") String computerId);

    /**
     * 查询所有用户电脑关联
     *
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectAll();

    /**
     * 根据条件查询用户电脑关联列表
     *
     * @param userComputer 查询条件
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectByCondition(UserComputer userComputer);

    /**
     * 插入用户电脑关联
     *
     * @param userComputer 用户电脑关联对象
     * @return 影响行数
     */
    int insert(UserComputer userComputer);

    /**
     * 更新用户电脑关联
     *
     * @param userComputer 用户电脑关联对象
     * @return 影响行数
     */
    int updateById(UserComputer userComputer);

    /**
     * 根据ID删除用户电脑关联
     *
     * @param id 关联ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据终端用户ID和电脑ID删除关联
     *
     * @param endUserId 终端用户ID
     * @param computerId 电脑ID
     * @return 影响行数
     */
    int deleteByEndUserIdAndComputerId(@Param("endUserId") String endUserId, 
                                     @Param("computerId") String computerId);

    /**
     * 检查用户电脑关联是否已存在
     *
     * @param endUserId 终端用户ID
     * @param computerId 电脑ID
     * @param id 排除的关联ID
     * @return 记录数
     */
    int checkUserComputerExists(@Param("endUserId") String endUserId, 
                             @Param("computerId") String computerId, 
                             @Param("id") Long id);

    /**
     * 更新关联状态
     *
     * @param id 关联ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据终端用户ID和状态查询关联
     *
     * @param endUserId 终端用户ID
     * @param status 状态
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectByEndUserIdAndStatus(@Param("endUserId") String endUserId, 
                                               @Param("status") Integer status);

    /**
     * 根据电脑ID和状态查询关联
     *
     * @param computerId 电脑ID
     * @param status 状态
     * @return 用户电脑关联列表
     */
    List<UserComputer> selectByComputerIdAndStatus(@Param("computerId") String computerId, 
                                               @Param("status") Integer status);

    /**
     * 统计指定终端用户的电脑数量
     *
     * @param endUserId 终端用户ID
     * @return 电脑数量
     */
    int countByEndUserId(@Param("endUserId") String endUserId);

    /**
     * 统计指定电脑的用户数量
     *
     * @param computerId 电脑ID
     * @return 用户数量
     */
    int countByComputerId(@Param("computerId") String computerId);
}