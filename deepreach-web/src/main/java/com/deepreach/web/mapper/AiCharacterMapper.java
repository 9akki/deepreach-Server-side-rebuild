package com.deepreach.web.mapper;

import com.deepreach.web.entity.AiCharacter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI人设Mapper接口
 *
 * 负责AI人设相关的数据库操作，包括：
 * 1. 人设基本信息CRUD操作
 * 2. 人设分类和来源查询
 * 3. 用户人设关联查询
 * 4. 人设统计和管理查询
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Mapper
public interface AiCharacterMapper {

    /**
     * 根据人设ID查询人设信息
     *
     * 查询人设的基本信息，包括名称、提示词、描述等
     * 不包含用户信息，仅用于基本的信息获取
     *
     * @param id 人设ID
     * @return 人设实体对象，如果不存在则返回null
     */
    AiCharacter selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询人设列表
     *
     * 查询指定用户创建的所有人设
     * 按创建时间倒序排列
     *
     * @param userId 用户ID
     * @return 人设列表
     */
    List<AiCharacter> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据人设类型查询人设列表
     *
     * 查询指定类型的所有人设
     * 包括用户自建和系统提供的人设
     *
     * @param type 人设类型（emotion/business）
     * @return 人设列表
     */
    List<AiCharacter> selectByType(@Param("type") String type);

    /**
     * 查询系统人设列表
     *
     * 查询所有系统预设的人设
     * 按创建时间倒序排列
     *
     * @return 系统人设列表
     */
    List<AiCharacter> selectSystemCharacters();

    /**
     * 查询用户自建人设列表
     *
     * 查询所有用户自建的人设
     * 按创建时间倒序排列
     *
     * @return 用户自建人设列表
     */
    List<AiCharacter> selectUserCharacters();

    /**
     * 根据条件查询人设列表
     *
     * 支持多条件查询，包括人设名称、类型、来源、用户等
     * 权限限制：只能查询系统人设和当前用户创建的人设
     *
     * @param character 查询条件对象
     * @return 人设列表，按创建时间倒序排列
     */
    List<AiCharacter> selectList(AiCharacter character);

    /**
     * 根据条件查询人设列表（带用户权限控制）
     *
     * 支持多条件查询，包括人设名称、类型、来源等
     * 权限限制：只能查询系统人设和指定用户创建的人设
     *
     * @param character 查询条件对象
     * @param currentUserId 当前用户ID
     * @return 人设列表，系统人设优先，按创建时间倒序排列
     */
    List<AiCharacter> selectListWithUserPermission(@Param("character") AiCharacter character,
                                                   @Param("currentUserId") Long currentUserId);

    /**
     * 查询人设总数
     *
     * 用于后台统计功能，统计符合条件的人设数量
     *
     * @param character 查询条件对象
     * @return 人设总数
     */
    int countCharacters(AiCharacter character);

    /**
     * 统计用户的人设数量
     *
     * 统计指定用户创建的人设数量
     *
     * @param userId 用户ID
     * @return 人设数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计指定用户集合的人设数量分布
     *
     * @param userIds 用户ID列表
     * @return 人设数量统计
     */
    com.deepreach.web.entity.dto.AiCharacterStatistics countStatisticsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 统计指定类型的人设数量
     *
     * @param type 人设类型
     * @return 人设数量
     */
    int countByType(@Param("type") String type);

    /**
     * 插入新人设
     *
     * 创建新人设记录，包含基本信息
     * 创建时间由数据库自动设置
     *
     * @param character 人设对象，包含必要的基本信息
     * @return 成功插入的记录数，通常为1
     */
    int insert(AiCharacter character);

    /**
     * 更新人设信息
     *
     * 更新人设的基本信息，不包括创建时间
     * 更新时间由数据库自动更新
     *
     * @param character 人设对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int update(AiCharacter character);

    /**
     * 删除人设
     *
     * 根据人设ID删除人设记录
     * 操作不可逆，请谨慎使用
     *
     * @param id 人设ID
     * @return 成功删除的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除人设
     *
     * 根据人设ID列表批量删除人设
     * 用于批量管理功能，提高操作效率
     *
     * @param ids 人设ID列表
     * @return 成功删除的记录数
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 检查人设名称是否唯一
     *
     * 用于人设创建和修改时的唯一性验证
     * 同一用户下人设名称不能重复
     *
     * @param name 人设名称
     * @param userId 用户ID
     * @param id 排除的人设ID（用于更新验证）
     * @return 存在相同名称的记录数，0表示唯一
     */
    int checkNameUnique(@Param("name") String name, @Param("userId") Long userId, @Param("id") Long id);

    /**
     * 检查用户是否可以删除人设
     *
     * 系统人设不能被用户删除
     * 用户只能删除自己创建的人设
     *
     * @param id 人设ID
     * @param userId 用户ID
     * @return 是否可以删除
     */
    boolean canDelete(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询热门人设列表
     *
     * 查询使用频率较高的人设
     * 用于推荐和展示
     *
     * @param limit 限制数量
     * @return 热门人设列表
     */
    List<AiCharacter> selectPopularCharacters(@Param("limit") Integer limit);

    /**
     * 查询最新人设列表
     *
     * 查询最新创建的人设
     * 用于发现和推荐
     *
     * @param limit 限制数量
     * @return 最新人设列表
     */
    List<AiCharacter> selectLatestCharacters(@Param("limit") Integer limit);

    /**
     * 查询人设的完整信息
     *
     * 获取人设的完整信息，包括创建者信息
     * 用于人设详情展示
     *
     * @param id 人设ID
     * @return 人设完整信息对象
     */
    AiCharacter selectCompleteInfo(@Param("id") Long id);

    /**
     * 更新人设头像
     *
     * 专门用于更新人设头像
     *
     * @param id 人设ID
     * @param avatar 头像URL
     * @return 成功更新的记录数
     */
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /**
     * 更新人设使用次数
     *
     * 用于人设使用统计
     *
     * @param id 人设ID
     * @return 成功更新的记录数
     */
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 查询用户可访问的人设列表
     *
     * 查询用户可以访问的所有人设，包括：
     * 1. 用户自己创建的人设
     * 2. 系统提供的人设
     *
     * @param userId 用户ID
     * @return 可访问的人设列表
     */
    List<AiCharacter> selectAccessibleCharacters(@Param("userId") Long userId);

    /**
     * 查询指定时间范围内的人设
     *
     * 用于统计和分析
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param userId 用户ID（可选）
     * @return 人设列表
     */
    List<AiCharacter> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("userId") Long userId);

    /**
     * 搜索人设
     *
     * 根据关键词搜索人设
     * 搜索范围：人设名称、描述、提示词
     *
     * @param keyword 关键词
     * @param userId 用户ID（可选，限制搜索范围）
     * @param limit 限制数量
     * @return 搜索结果
     */
    List<AiCharacter> searchCharacters(@Param("keyword") String keyword,
                                       @Param("userId") Long userId,
                                       @Param("limit") Integer limit);

    /**
     * 获取人设统计信息
     *
     * 获取人设的统计数据
     *
     * @return 统计信息Map
     */
    java.util.Map<String, Object> getCharacterStatistics();

    /**
     * 获取用户的人设统计信息
     *
     * 获取指定用户的人设统计数据
     *
     * @param userId 用户ID
     * @return 统计信息Map
     */
    java.util.Map<String, Object> getUserCharacterStatistics(@Param("userId") Long userId);

    /**
     * 检查人设是否存在
     *
     * @param id 人设ID
     * @return 是否存在
     */
    boolean existsById(@Param("id") Long id);

    /**
     * 检查用户是否有权限访问人设
     *
     * @param id 人设ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasAccessPermission(@Param("id") Long id, @Param("userId") Long userId);
}
