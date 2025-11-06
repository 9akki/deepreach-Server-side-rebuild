package com.deepreach.web.service.impl;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.exception.BalanceNotEnoughException;
import com.deepreach.web.entity.AiCharacter;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.entity.DrPriceConfig;
import com.deepreach.web.mapper.AiCharacterMapper;
import com.deepreach.web.service.AiCharacterService;
import com.deepreach.web.service.DrPriceConfigService;
import com.deepreach.web.service.UserDrBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI人设Service实现类
 *
 * 实现AI人设相关的业务逻辑，包括：
 * 1. 人设基本信息管理
 * 2. 人设权限控制
 * 3. 人设搜索和推荐
 * 4. 人设统计和分析
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@Service
public class AiCharacterServiceImpl implements AiCharacterService {

    @Autowired
    private AiCharacterMapper characterMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DrPriceConfigService priceConfigService;

    @Autowired
    private UserDrBalanceService userDrBalanceService;

    @Override
    public AiCharacter selectById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return characterMapper.selectById(id);
        } catch (Exception e) {
            log.error("查询人设失败：人设ID={}", id, e);
            return null;
        }
    }

    @Override
    public AiCharacter selectCompleteInfo(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return characterMapper.selectCompleteInfo(id);
        } catch (Exception e) {
            log.error("查询人设完整信息失败：人设ID={}", id, e);
            return null;
        }
    }

    @Override
    public List<AiCharacter> selectByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            return characterMapper.selectByUserId(userId);
        } catch (Exception e) {
            log.error("查询用户人设列表失败：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AiCharacter> selectList(AiCharacter character) {
        if (character == null) {
            character = new AiCharacter();
        }
        try {
            return characterMapper.selectList(character);
        } catch (Exception e) {
            log.error("查询人设列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AiCharacter> selectListWithUserPermission(AiCharacter character, Long currentUserId) {
        if (character == null) {
            character = new AiCharacter();
        }
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        try {
            return characterMapper.selectListWithUserPermission(character, currentUserId);
        } catch (Exception e) {
            log.error("查询人设列表失败（带权限控制）", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiCharacter insert(AiCharacter character) throws Exception {
        // 参数验证
        if (character == null) {
            throw new Exception("人设信息不能为空");
        }

        if (!character.isNameValid()) {
            throw new Exception("人设名称无效，长度应在1-100个字符之间");
        }

        if (!character.isPromptValid()) {
            throw new Exception("人设提示词不能为空");
        }

        if (!character.isTypeValid()) {
            throw new Exception("人设类型无效，只支持emotion或business");
        }

        if (character.getUserId() == null) {
            throw new Exception("用户ID不能为空");
        }

        // 检查名称唯一性
        if (!checkNameUnique(character.getName(), character.getUserId(), null)) {
            throw new Exception("人设名称已存在");
        }

        SysUser creator = sysUserService.selectUserWithDept(character.getUserId());
        if (creator == null) {
            throw new Exception("用户信息不存在");
        }

        boolean buyerMain = creator.isBuyerMainIdentity();
        boolean buyerSub = creator.isBuyerSubIdentity();
        if (!buyerMain && !buyerSub) {
            throw new Exception("仅支持商家总账户或子账户创建人设");
        }

        Long chargeAccountId = buyerSub ? creator.getParentUserId() : creator.getUserId();
        if (chargeAccountId == null || chargeAccountId <= 0) {
            throw new Exception("未找到商家计费账户信息，无法创建人设");
        }

        DrPriceConfig priceConfig = priceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_AI_CHARACTER);
        if (priceConfig == null || !priceConfig.isActive() || priceConfig.getDrPrice() == null) {
            throw new Exception("AI人设价格配置不存在或未启用");
        }
        BigDecimal characterPrice = priceConfig.getDrPrice();
        boolean consumedFreeQuota = false;
        if (characterPrice.compareTo(BigDecimal.ZERO) > 0) {
            consumedFreeQuota = userDrBalanceService.tryConsumeAiCharacterFreeTimes(chargeAccountId);
        }
        if (!consumedFreeQuota
            && characterPrice.compareTo(BigDecimal.ZERO) > 0
            && !userDrBalanceService.checkBalanceSufficient(chargeAccountId, characterPrice)) {
            throw new BalanceNotEnoughException("账户余额不足，无法创建AI人设");
        }


        try {
            int result = characterMapper.insert(character);
            if (result <= 0) {
                throw new Exception("创建人设失败");
            }
            log.info("创建人设成功：人设ID={}, 名称={}", character.getId(), character.getName());

            if (characterPrice.compareTo(BigDecimal.ZERO) > 0 && !consumedFreeQuota) {
                Integer billingType = priceConfig.getBillingType() != null
                    ? priceConfig.getBillingType()
                    : DrPriceConfig.BILLING_TYPE_INSTANT;
                boolean deducted = userDrBalanceService.consume(
                    chargeAccountId,
                    characterPrice,
                    DrBillingRecord.BUSINESS_TYPE_AI_CHARACTER,
                    character.getId(),
                    billingType,
                    character.getUserId(),
                    "创建AI人设：" + character.getName()
                );
                if (!deducted) {
                    throw new Exception("扣除AI人设创建费用失败");
                }
                log.info("创建AI人设扣费成功：计费账户ID={}, 扣费金额={}", chargeAccountId, characterPrice);
            } else if (consumedFreeQuota) {
                log.info("创建AI人设使用免费额度成功：计费账户ID={}", chargeAccountId);
            }
            return character;
        } catch (Exception e) {
            log.error("创建人设失败：名称={}", character.getName(), e);
            throw new Exception("创建人设失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(AiCharacter character) throws Exception {
        if (character == null || character.getId() == null) {
            throw new Exception("人设ID不能为空");
        }

        // 检查人设是否存在
        AiCharacter existing = selectById(character.getId());
        if (existing == null) {
            throw new Exception("人设不存在");
        }

        // 系统人设的某些字段不允许修改
        if (existing.isSystemCharacter()) {
            if (character.getName() != null && !character.getName().equals(existing.getName())) {
                throw new Exception("系统人设名称不允许修改");
            }
            if (character.getUserId() != null && !character.getUserId().equals(existing.getUserId())) {
                throw new Exception("系统人设归属不允许修改");
            }
        }

        // 参数验证
        if (character.getName() != null && !character.isNameValid()) {
            throw new Exception("人设名称无效，长度应在1-100个字符之间");
        }

        if (character.getPrompt() != null && character.getPrompt().trim().isEmpty()) {
            throw new Exception("人设提示词不能为空");
        }

        if (character.getType() != null && !character.isTypeValid()) {
            throw new Exception("人设类型无效，只支持emotion或business");
        }

        // 检查名称唯一性
        if (character.getName() != null && !character.getName().equals(existing.getName())) {
            if (!checkNameUnique(character.getName(), existing.getUserId(), character.getId())) {
                throw new Exception("人设名称已存在");
            }
        }

        // 数据库会自动设置updated_time

        try {
            int result = characterMapper.update(character);
            if (result <= 0) {
                throw new Exception("更新人设失败");
            }
            log.info("更新人设成功：人设ID={}", character.getId());
            return true;
        } catch (Exception e) {
            log.error("更新人设失败：人设ID={}", character.getId(), e);
            throw new Exception("更新人设失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) throws Exception {
        if (id == null) {
            throw new Exception("人设ID不能为空");
        }

        // 检查人设是否存在
        AiCharacter existing = selectById(id);
        if (existing == null) {
            throw new Exception("人设不存在");
        }

        // 系统人设不能删除
        if (existing.isSystemCharacter()) {
            throw new Exception("系统人设不能删除");
        }

        try {
            int result = characterMapper.deleteById(id);
            if (result <= 0) {
                throw new Exception("删除人设失败");
            }
            log.info("删除人设成功：人设ID={}, 名称={}", id, existing.getName());
            return true;
        } catch (Exception e) {
            log.error("删除人设失败：人设ID={}", id, e);
            throw new Exception("删除人设失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) throws Exception {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        // 过滤掉系统人设
        List<Long> validIds = new ArrayList<>();
        for (Long id : ids) {
            AiCharacter character = selectById(id);
            if (character != null && !character.isSystemCharacter()) {
                validIds.add(id);
            }
        }

        if (validIds.isEmpty()) {
            throw new Exception("没有可删除的人设（系统人设不能删除）");
        }

        try {
            int result = characterMapper.deleteByIds(validIds);
            if (result <= 0) {
                throw new Exception("批量删除人设失败");
            }
            log.info("批量删除人设成功：删除数量={}", result);
            return true;
        } catch (Exception e) {
            log.error("批量删除人设失败：人设IDs={}", ids, e);
            throw new Exception("批量删除人设失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectByType(String type) throws Exception {
        if (type == null || type.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return characterMapper.selectByType(type);
        } catch (Exception e) {
            log.error("按类型查询人设失败：类型={}", type, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectSystemCharacters() throws Exception {
        try {
            return characterMapper.selectSystemCharacters();
        } catch (Exception e) {
            log.error("查询系统人设失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectUserCharacters() throws Exception {
        try {
            return characterMapper.selectUserCharacters();
        } catch (Exception e) {
            log.error("查询用户人设失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectAccessibleCharacters(Long userId) throws Exception {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            return characterMapper.selectAccessibleCharacters(userId);
        } catch (Exception e) {
            log.error("查询可访问人设失败：用户ID={}", userId, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public boolean checkNameUnique(String name, Long userId, Long id) {
        if (name == null || name.trim().isEmpty() || userId == null) {
            return false;
        }
        try {
            int count = characterMapper.checkNameUnique(name, userId, id);
            return count == 0;
        } catch (Exception e) {
            log.error("检查人设名称唯一性失败：名称={}, 用户ID={}", name, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAvatar(Long id, String avatarUrl) throws Exception {
        if (id == null) {
            throw new Exception("人设ID不能为空");
        }

        if (avatarUrl != null && avatarUrl.length() > 500) {
            throw new Exception("头像URL长度不能超过500个字符");
        }

        // 检查人设是否存在
        AiCharacter existing = selectById(id);
        if (existing == null) {
            throw new Exception("人设不存在");
        }

        try {
            int result = characterMapper.updateAvatar(id, avatarUrl);
            if (result <= 0) {
                throw new Exception("更新头像失败");
            }
            log.info("更新人设头像成功：人设ID={}", id);
            return true;
        } catch (Exception e) {
            log.error("更新人设头像失败：人设ID={}", id, e);
            throw new Exception("更新头像失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> searchCharacters(String keyword, Long userId, Integer limit) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return characterMapper.searchCharacters(keyword, userId, limit);
        } catch (Exception e) {
            log.error("搜索人设失败：关键词={}", keyword, e);
            throw new Exception("搜索失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectPopularCharacters(Integer limit) throws Exception {
        try {
            return characterMapper.selectPopularCharacters(limit != null ? limit : 10);
        } catch (Exception e) {
            log.error("查询热门人设失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiCharacter> selectLatestCharacters(Integer limit) throws Exception {
        try {
            return characterMapper.selectLatestCharacters(limit != null ? limit : 10);
        } catch (Exception e) {
            log.error("查询最新人设失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStatistics() throws Exception {
        try {
            return characterMapper.getCharacterStatistics();
        } catch (Exception e) {
            log.error("获取人设统计信息失败", e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserStatistics(Long userId) throws Exception {
        if (userId == null) {
            return new HashMap<>();
        }
        try {
            return characterMapper.getUserCharacterStatistics(userId);
        } catch (Exception e) {
            log.error("获取用户人设统计信息失败：用户ID={}", userId, e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    public boolean hasAccessPermission(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }
        try {
            return characterMapper.hasAccessPermission(id, userId);
        } catch (Exception e) {
            log.error("检查人设访问权限失败：人设ID={}, 用户ID={}", id, userId, e);
            return false;
        }
    }

    @Override
    public boolean canDelete(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }
        try {
            return characterMapper.canDelete(id, userId);
        } catch (Exception e) {
            log.error("检查人设删除权限失败：人设ID={}, 用户ID={}", id, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiCharacter createSystemCharacter(AiCharacter character) throws Exception {
        if (character == null) {
            throw new Exception("人设信息不能为空");
        }

        // 设置为系统人设
        character.setIsSystem(true);
        character.setUserId(1L); // 归属于超级管理员

        return insert(character);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiCharacter createUserCharacter(AiCharacter character, Long userId) throws Exception {
        if (character == null) {
            throw new Exception("人设信息不能为空");
        }

        // 设置为用户自建人设
        character.setIsSystem(false);
        character.setUserId(userId);

        // 检查用户创建数量限制（可选）
        int userCount = characterMapper.countByUserId(userId);
        if (userCount >= 50) { // 假设限制为50个
            throw new Exception("您创建的人设数量已达上限（50个）");
        }

        return insert(character);
    }

    @Override
    public boolean validateCharacter(AiCharacter character) {
        if (character == null) {
            return false;
        }

        return character.isNameValid()
               && character.isPromptValid()
               && character.isTypeValid()
               && character.isAvatarValid()
               && character.getUserId() != null
               && character.getUserId() > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiCharacter copyCharacter(Long sourceId, String newName, Long userId) throws Exception {
        if (sourceId == null || userId == null) {
            throw new Exception("源人设ID和用户ID不能为空");
        }

        // 获取源人设
        AiCharacter source = selectById(sourceId);
        if (source == null) {
            throw new Exception("源人设不存在");
        }

        // 检查访问权限
        if (!hasAccessPermission(sourceId, userId)) {
            throw new Exception("无权限访问该人设");
        }

        // 检查新名称唯一性
        if (!checkNameUnique(newName, userId, null)) {
            throw new Exception("人设名称已存在");
        }

        // 创建副本
        AiCharacter copy = new AiCharacter();
        copy.setName(newName);
        copy.setPrompt(source.getPrompt());
        copy.setDescription(source.getDescription());
        copy.setAvatar(source.getAvatar());
        copy.setType(source.getType());
        copy.setIsSystem(false); // 副本都是用户自建的
        copy.setUserId(userId);
        copy.setRemark("复制自：" + source.getName());

        return insert(copy);
    }

    @Override
    public Map<String, Object> getTypeStatistics(Long userId) throws Exception {
        try {
            Map<String, Object> result = new HashMap<>();

            if (userId != null) {
                Map<String, Object> userStats = getUserStatistics(userId);
                result.put("emotion", userStats.get("emotionCount"));
                result.put("business", userStats.get("businessCount"));
                result.put("total", userStats.get("totalCount"));
            } else {
                Map<String, Object> globalStats = getStatistics();
                result.put("emotion", globalStats.get("emotionCount"));
                result.put("business", globalStats.get("businessCount"));
                result.put("total", globalStats.get("totalCount"));
            }

            return result;
        } catch (Exception e) {
            log.error("获取人设分类统计失败", e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCharacters(List<AiCharacter> characters, boolean updateSupport) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        if (characters == null || characters.isEmpty()) {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("successList", successList);
            result.put("failList", failList);
            return result;
        }

        for (AiCharacter character : characters) {
            try {
                if (!validateCharacter(character)) {
                    failList.add("人设数据无效：" + character.getName());
                    continue;
                }

                // 检查是否已存在
                AiCharacter existing = null;
                if (character.getId() != null) {
                    existing = selectById(character.getId());
                }

                if (existing != null) {
                    if (updateSupport) {
                        update(character);
                        successList.add("更新成功：" + character.getName());
                    } else {
                        failList.add("人设已存在：" + character.getName());
                    }
                } else {
                    insert(character);
                    successList.add("创建成功：" + character.getName());
                }
            } catch (Exception e) {
                failList.add("处理失败：" + character.getName() + "，原因：" + e.getMessage());
            }
        }

        result.put("successCount", successList.size());
        result.put("failCount", failList.size());
        result.put("successList", successList);
        result.put("failList", failList);

        return result;
    }

    @Override
    public String exportCharacters(List<AiCharacter> characters) throws Exception {
        try {
            // 简单的JSON格式导出
            StringBuilder json = new StringBuilder();
            json.append("[\n");

            for (int i = 0; i < characters.size(); i++) {
                AiCharacter character = characters.get(i);
                json.append("  {\n");
                json.append("    \"id\": ").append(character.getId()).append(",\n");
                json.append("    \"name\": \"").append(character.getName()).append("\",\n");
                json.append("    \"prompt\": \"").append(character.getPrompt()).append("\",\n");
                json.append("    \"description\": \"").append(character.getDescription()).append("\",\n");
                json.append("    \"type\": \"").append(character.getType()).append("\",\n");
                json.append("    \"isSystem\": ").append(character.getIsSystem()).append(",\n");
                json.append("    \"createdAt\": \"").append(character.getCreatedAt()).append("\"\n");
                json.append("  }");
                if (i < characters.size() - 1) {
                    json.append(",\n");
                }
            }

            json.append("\n]");
            return json.toString();
        } catch (Exception e) {
            log.error("导出人设数据失败", e);
            throw new Exception("导出失败：" + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getUsageRecords(Long id) throws Exception {
        // 这里可以根据实际需求实现使用记录查询
        // 暂时返回空列表
        return new ArrayList<>();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementUsageCount(Long id) throws Exception {
        if (id == null) {
            return false;
        }
        try {
            int result = characterMapper.incrementUsageCount(id);
            return result > 0;
        } catch (Exception e) {
            log.error("更新人设使用次数失败：人设ID={}", id, e);
            return false;
        }
    }
}
