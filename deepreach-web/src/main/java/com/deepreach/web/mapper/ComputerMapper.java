package com.deepreach.web.mapper;

import com.deepreach.common.core.domain.entity.Computer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 云电脑Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Mapper
public interface ComputerMapper {

    /**
     * 根据ID查询云电脑
     *
     * @param id 电脑ID
     * @return 云电脑对象
     */
    Computer selectById(@Param("id") Long id);

    /**
     * 根据电脑ID查询云电脑
     *
     * @param computerId 电脑ID
     * @return 云电脑对象
     */
    Computer selectByComputerId(@Param("computerId") String computerId);

    /**
     * 查询所有云电脑
     *
     * @return 云电脑列表
     */
    List<Computer> selectAll();

    /**
     * 根据条件查询云电脑列表
     *
     * @param computer 查询条件
     * @return 云电脑列表
     */
    List<Computer> selectByCondition(Computer computer);

    /**
     * 根据办公站点ID查询云电脑列表
     *
     * @param officeSiteId 办公站点ID
     * @return 云电脑列表
     */
    List<Computer> selectByOfficeSiteId(@Param("officeSiteId") String officeSiteId);

    /**
     * 插入云电脑
     *
     * @param computer 云电脑对象
     * @return 影响行数
     */
    int insert(Computer computer);

    /**
     * 更新云电脑
     *
     * @param computer 云电脑对象
     * @return 影响行数
     */
    int updateById(Computer computer);

    /**
     * 根据ID删除云电脑
     *
     * @param id 电脑ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查电脑ID是否唯一
     *
     * @param computerId 电脑ID
     * @param id 排除的电脑ID
     * @return 记录数
     */
    int checkComputerIdUnique(@Param("computerId") String computerId, @Param("id") Long id);

    /**
     * 更新电脑状态
     *
     * @param id 电脑ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计指定办公站点的电脑数量
     *
     * @param officeSiteId 办公站点ID
     * @return 电脑数量
     */
    int countByOfficeSiteId(@Param("officeSiteId") String officeSiteId);
}