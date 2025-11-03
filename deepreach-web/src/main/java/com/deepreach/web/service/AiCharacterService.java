package com.deepreach.web.service;

import com.deepreach.web.entity.AiCharacter;

import java.util.List;
import java.util.Map;

/**
 * AI人设Service接口
 *
 * 负责AI人设相关的业务逻辑，包括：
 * 1. 人设基本信息管理（增删改查）
 * 2. 人设分类和来源管理
 * 3. 用户人设权限控制
 * 4. 人设搜索和推荐
 * 5. 人设统计和分析
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
public interface AiCharacterService {

    /**
     * 根据人设ID查询人设
     *
     * 获取人设的基本信息，不包含敏感信息
     * 用于人设信息展示和基本信息管理
     *
     * @param id 人设ID
     * @return 人设对象，如果不存在则返回null
     */
    AiCharacter selectById(Long id);

    /**
     * 查询人设的完整信息
     *
     * 获取人设的完整信息，包括创建者等关联信息
     * 用于人设详情展示和权限验证
     *
     * @param id 人设ID
     * @return 包含完整关联信息的人设对象，如果不存在则返回null
     */
    AiCharacter selectCompleteInfo(Long id);

    /**
     * 根据用户ID查询人设列表
     *
     * 查询指定用户创建的所有人设
     * 用于用户个人人设管理
     *
     * @param userId 用户ID
     * @return 人设列表
     */
    List<AiCharacter> selectByUserId(Long userId);

    /**
     * 根据条件查询人设列表
     *
     * 支持多条件查询，包括人设名称、类型、来源等
     * 自动应用权限过滤，确保只能查看有权限的人设
     *
     * @param character 查询条件对象
     * @return 人设列表
     */
    List<AiCharacter> selectList(AiCharacter character);

    /**
     * 根据条件查询人设列表（带用户权限控制）
     *
     * 支持多条件查询，自动应用权限过滤
     * 权限限制：只能查询系统人设和当前用户创建的人设
     *
     * @param character 查询条件对象
     * @param currentUserId 当前用户ID
     * @return 人设列表，系统人设优先
     */
    List<AiCharacter> selectListWithUserPermission(AiCharacter character, Long currentUserId);

    /**
     * 创建新人设
     *
     * 创建新的人设，包含完整的业务逻辑：
     * 1. 参数验证和唯一性检查
     * 2. 权限验证
     * 3. 创建记录和日志记录
     *
     * @param character 人设对象，包含必要信息
     * @return 创建成功后的人设对象，包含生成的ID
     * @throws Exception 当参数验证失败或数据冲突时抛出异常
     */
    AiCharacter insert(AiCharacter character) throws Exception;

    /**
     * 更新人设信息
     *
     * 更新人设的基本信息
     * 包含参数验证和权限检查
     *
     * @param character 人设对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    boolean update(AiCharacter character) throws Exception;

    /**
     * 删除人设
     *
     * 根据人设ID删除人设记录
     * 包含依赖检查和权限验证
     *
     * @param id 人设ID
     * @return 是否删除成功
     * @throws Exception 当人设不存在或有依赖关系时抛出异常
     */
    boolean deleteById(Long id) throws Exception;

    /**
     * 批量删除人设
     *
     * 根据人设ID列表批量删除人设
     * 包含批量依赖检查和权限验证
     *
     * @param ids 人设ID列表
     * @return 是否删除成功
     * @throws Exception 当有人设不存在或有依赖关系时抛出异常
     */
    boolean deleteByIds(List<Long> ids) throws Exception;

    /**
     * 根据人设类型查询人设列表
     *
     * @param type 人设类型（emotion/business）
     * @return 人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectByType(String type) throws Exception;

    /**
     * 查询系统人设列表
     *
     * 查询所有系统预设的人设
     *
     * @return 系统人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectSystemCharacters() throws Exception;

    /**
     * 查询用户自建人设列表
     *
     * 查询所有用户自建的人设
     *
     * @return 用户自建人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectUserCharacters() throws Exception;

    /**
     * 查询用户可访问的人设列表
     *
     * 查询用户可以访问的所有人设，包括：
     * 1. 用户自己创建的人设
     * 2. 系统提供的人设
     *
     * @param userId 用户ID
     * @return 可访问的人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectAccessibleCharacters(Long userId) throws Exception;

    /**
     * 检查人设名称是否唯一
     *
     * 用于人设创建和修改时的唯一性验证
     *
     * @param name 人设名称
     * @param userId 用户ID
     * @param id 排除的人设ID（用于更新验证）
     * @return true如果唯一，false如果已存在
     */
    boolean checkNameUnique(String name, Long userId, Long id);

    /**
     * 更新人设头像
     *
     * 更新人设的头像地址
     * 需要验证头像文件的有效性
     *
     * @param id 人设ID
     * @param avatarUrl 头像URL地址
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updateAvatar(Long id, String avatarUrl) throws Exception;

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
     * @throws Exception 当搜索失败时抛出异常
     */
    List<AiCharacter> searchCharacters(String keyword, Long userId, Integer limit) throws Exception;

    /**
     * 查询热门人设列表
     *
     * 查询使用频率较高的人设
     * 用于推荐和展示
     *
     * @param limit 限制数量
     * @return 热门人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectPopularCharacters(Integer limit) throws Exception;

    /**
     * 查询最新人设列表
     *
     * 查询最新创建的人设
     * 用于发现和推荐
     *
     * @param limit 限制数量
     * @return 最新人设列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiCharacter> selectLatestCharacters(Integer limit) throws Exception;

    /**
     * 获取人设统计信息
     *
     * 获取人设相关的统计数据
     * 用于管理界面的统计展示
     *
     * @return 统计信息Map
     * @throws Exception 当统计失败时抛出异常
     */
    Map<String, Object> getStatistics() throws Exception;

    /**
     * 获取用户的人设统计信息
     *
     * 获取指定用户的人设统计数据
     *
     * @param userId 用户ID
     * @return 统计信息Map
     * @throws Exception 当统计失败时抛出异常
     */
    Map<String, Object> getUserStatistics(Long userId) throws Exception;

    /**
     * 检查用户是否有权限访问人设
     *
     * 验证当前用户是否有权限访问指定人设
     * 用于权限控制
     *
     * @param id 人设ID
     * @param userId 用户ID
     * @return true如果有权限，false否则
     */
    boolean hasAccessPermission(Long id, Long userId);

    /**
     * 检查用户是否可以删除人设
     *
     * 验证用户是否有权限删除指定人设
     * 系统人设不能被普通用户删除
     *
     * @param id 人设ID
     * @param userId 用户ID
     * @return true如果可以删除，false否则
     */
    boolean canDelete(Long id, Long userId);

    /**
     * 创建系统人设
     *
     * 创建系统预设的人设
     * 需要管理员权限
     *
     * @param character 人设对象
     * @return 创建成功后的人设对象
     * @throws Exception 当创建失败时抛出异常
     */
    AiCharacter createSystemCharacter(AiCharacter character) throws Exception;

    /**
     * 创建用户人设
     *
     * 创建用户自定义的人设
     * 包含权限验证和数量限制检查
     *
     * @param character 人设对象
     * @param userId 创建者用户ID
     * @return 创建成功后的人设对象
     * @throws Exception 当创建失败时抛出异常
     */
    AiCharacter createUserCharacter(AiCharacter character, Long userId) throws Exception;

    /**
     * 验证人设数据
     *
     * 验证人设数据的完整性和有效性
     *
     * @param character 人设对象
     * @return 验证结果
     */
    boolean validateCharacter(AiCharacter character);

    /**
     * 复制人设
     *
     * 基于现有人设创建副本
     * 用于用户收藏和自定义修改
     *
     * @param sourceId 源人设ID
     * @param newName 新人设名称
     * @param userId 目标用户ID
     * @return 复制成功后的人设对象
     * @throws Exception 当复制失败时抛出异常
     */
    AiCharacter copyCharacter(Long sourceId, String newName, Long userId) throws Exception;

    /**
     * 获取人设分类统计
     *
     * 获取按类型分类的人设统计信息
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 分类统计信息
     * @throws Exception 当统计失败时抛出异常
     */
    Map<String, Object> getTypeStatistics(Long userId) throws Exception;

    /**
     * 导入人设数据
     *
     * 批量导入人设数据
     * 支持JSON等格式的批量导入
     *
     * @param characters 人设列表
     * @param updateSupport 是否支持更新已存在的人设
     * @return 导入结果，包含成功和失败信息
     * @throws Exception 当导入过程中发生错误时抛出异常
     */
    Map<String, Object> importCharacters(List<AiCharacter> characters, boolean updateSupport) throws Exception;

    /**
     * 导出人设数据
     *
     * 导出人设数据为指定格式
     * 支持JSON等格式的数据导出
     *
     * @param characters 人设列表
     * @return 导出数据的字符串
     * @throws Exception 当导出过程中发生错误时抛出异常
     */
    String exportCharacters(List<AiCharacter> characters) throws Exception;

    /**
     * 获取人设使用记录
     *
     * 获取人设的使用记录和统计
     * 用于分析和优化
     *
     * @param id 人设ID
     * @return 使用记录列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<Map<String, Object>> getUsageRecords(Long id) throws Exception;

    /**
     * 更新人设使用次数
     *
     * 记录人设的使用情况
     * 用于统计和推荐
     *
     * @param id 人设ID
     * @return 是否更新成功
     * @throws Exception 当更新失败时抛出异常
     */
    boolean incrementUsageCount(Long id) throws Exception;
}