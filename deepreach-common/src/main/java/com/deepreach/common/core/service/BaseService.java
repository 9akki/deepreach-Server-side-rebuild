package com.deepreach.common.core.service;

import java.io.Serializable;
import java.util.List;

/**
 * 通用Service接口
 *
 * @param <T> 实体类型
 * @author DeepReach Team
 * @version 1.0
 */
public interface BaseService<T> {

    /**
     * 查询单条记录
     *
     * @param id 主键
     * @return 实体对象
     */
    T selectById(Serializable id);

    /**
     * 查询所有记录
     *
     * @return 实体列表
     */
    List<T> selectAll();

    /**
     * 根据条件查询记录
     *
     * @param entity 查询条件
     * @return 实体列表
     */
    List<T> selectByCondition(T entity);

    /**
     * 插入单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(T entity);

    /**
     * 更新单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int updateById(T entity);

    /**
     * 根据主键删除记录
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Serializable id);

    /**
     * 根据主键批量删除记录
     *
     * @param ids 主键数组
     * @return 影响行数
     */
    int deleteByIds(Serializable[] ids);

    /**
     * 根据条件删除记录
     *
     * @param entity 删除条件
     * @return 影响行数
     */
    int deleteByCondition(T entity);

    /**
     * 查询记录总数
     *
     * @param entity 查询条件
     * @return 记录数
     */
    int selectCount(T entity);

    /**
     * 分页查询
     *
     * @param entity 查询条件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 实体列表
     */
    List<T> selectPage(T entity, Integer pageNum, Integer pageSize);

    /**
     * 批量插入记录
     *
     * @param entities 实体列表
     * @return 影响行数
     */
    int batchInsert(List<T> entities);

    /**
     * 批量更新记录
     *
     * @param entities 实体列表
     * @return 影响行数
     */
    int batchUpdate(List<T> entities);

    /**
     * 验证实体对象
     *
     * @param entity 实体对象
     * @return 验证结果，null表示验证通过
     */
    String validate(T entity);

    /**
     * 保存或更新实体
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int saveOrUpdate(T entity);
}