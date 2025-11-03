package com.deepreach.common.core.mapper;

import java.io.Serializable;
import java.util.List;

/**
 * 通用Mapper接口
 * 提供基础的CRUD操作接口，与现有系统架构兼容
 *
 * @param <T> 实体类型
 * @author DeepReach Team
 * @version 1.0
 */
public interface BaseMapper<T> {

    /**
     * 根据主键查询实体
     *
     * @param id 主键
     * @return 实体对象
     */
    T selectById(Serializable id);

    /**
     * 查询所有实体
     *
     * @return 实体列表
     */
    List<T> selectAll();

    /**
     * 根据条件查询实体列表
     *
     * @param entity 查询条件
     * @return 实体列表
     */
    List<T> selectByCondition(T entity);

    /**
     * 插入实体
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(T entity);

    /**
     * 根据主键更新实体
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int updateById(T entity);

    /**
     * 根据主键删除实体
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Serializable id);

    /**
     * 根据主键批量删除实体
     *
     * @param ids 主键数组
     * @return 影响行数
     */
    int deleteByIds(Serializable[] ids);

    /**
     * 根据条件删除实体
     *
     * @param entity 删除条件
     * @return 影响行数
     */
    int deleteByCondition(T entity);

    /**
     * 根据条件统计实体数量
     *
     * @param entity 查询条件
     * @return 记录数
     */
    int selectCount(T entity);

    /**
     * 分页查询实体
     *
     * @param entity 查询条件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 实体列表
     */
    List<T> selectPage(T entity, Integer pageNum, Integer pageSize);

    /**
     * 批量插入实体
     *
     * @param entities 实体列表
     * @return 影响行数
     */
    int batchInsert(List<T> entities);

    /**
     * 批量更新实体
     *
     * @param entities 实体列表
     * @return 影响行数
     */
    int batchUpdate(List<T> entities);
}