package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.SysDept;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.mapper.SysDeptMapper;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.mapper.AgentCommissionAccountStatMapper;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.security.DeptUtils;
import com.deepreach.common.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门Service实现类
 *
 * 提供部门管理的完整业务逻辑实现，包括：
 * 1. 部门基本信息管理
 * 2. 部门树形结构处理
 * 3. 部门数据权限相关查询
 * 4. 部门层级关系管理
 * 5. 部门统计分析功能
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Slf4j
@Service
public class SysDeptServiceImpl implements SysDeptService {

    private static final BigDecimal LEVEL1_COMMISSION_RATE = new BigDecimal("0.30");
    private static final BigDecimal LEVEL2_COMMISSION_RATE = new BigDecimal("0.20");
    private static final BigDecimal LEVEL3_COMMISSION_RATE = new BigDecimal("0.10");
    private static final int MONEY_SCALE = 2;
    private static final Set<String> MARKETING_PLATFORM_TYPES =
            Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("marketing")));
    private static final Set<String> PROSPECTING_PLATFORM_TYPES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("customer-acquisition", "prospecting")));

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private AgentCommissionAccountStatMapper agentCommissionAccountMapper;

    @Override
    public SysDept selectDeptById(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return null;
        }
        return deptMapper.selectDeptById(deptId);
    }

    @Override
    public List<SysDept> selectDeptTreeList(SysDept dept) {
        List<SysDept> depts = selectDeptList(dept);
        return buildDeptTree(depts);
    }

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }

    @Override
    public List<SysDept> selectManagedDeptTreeByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        // 1. 查询当前用户作为负责人的所有部门
        List<SysDept> managedDepts = deptMapper.selectDeptsByLeaderUserId(userId);
        if (managedDepts == null || managedDepts.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取所有管理的部门ID（包含自身及所有子部门）
        Set<Long> allManagedDeptIds = new HashSet<>();
        for (SysDept dept : managedDepts) {
            allManagedDeptIds.add(dept.getDeptId());
            // 递归获取所有子部门ID
            List<Long> childIds = selectChildDeptIds(dept.getDeptId());
            allManagedDeptIds.addAll(childIds);
        }

        // 3. 查询所有相关部门信息
        List<SysDept> allDepts = deptMapper.selectDeptsByIds(allManagedDeptIds);
        if (allDepts == null || allDepts.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 构建部门树形结构
        return buildDeptTree(allDepts);
    }

    @Override
    public List<SysDept> selectDeptsByLeaderUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return deptMapper.selectDeptsByLeaderUserId(userId);
    }

    @Override
    public List<SysDept> selectChildrenByParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return new ArrayList<>();
        }
        return deptMapper.selectChildrenByParentId(parentId);
    }

    @Override
    public List<Long> selectChildDeptIds(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return new ArrayList<>();
        }

        try {
            List<Long> result = new ArrayList<>();
            selectChildDeptIdsRecursive(deptId, result);
            return result;
        } catch (Exception e) {
            log.error("获取子部门ID列表失败：部门ID={}", deptId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 递归查询子部门ID列表
     *
     * @param deptId 当前部门ID
     * @param result 结果列表
     */
    private void selectChildDeptIdsRecursive(Long deptId, List<Long> result) {
        if (deptId == null || deptId <= 0) {
            return;
        }

        // 添加当前部门ID
        result.add(deptId);

        // 查询直接子部门
        List<SysDept> children = selectChildrenByParentId(deptId);
        for (SysDept child : children) {
            selectChildDeptIdsRecursive(child.getDeptId(), result);
        }
    }

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }

        List<SysDept> returnList = new ArrayList<>();
        Map<Long, SysDept> deptMap = new HashMap<>();

        // 构建部门映射
        for (SysDept dept : depts) {
            deptMap.put(dept.getDeptId(), dept);
            // 确保children列表被初始化
            if (dept.getChildren() == null) {
                dept.setChildren(new ArrayList<>());
            }
        }

        // 构建树形结构
        for (SysDept dept : depts) {
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0) {
                // 根节点
                returnList.add(dept);
            } else {
                // 子节点
                SysDept parent = deptMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(dept);
                } else {
                    // 如果找不到父节点，可能是数据问题，将此节点作为根节点处理
                    System.err.println("警告：找不到部门ID=" + dept.getDeptId() + "的父部门，父部门ID=" + parentId);
                    returnList.add(dept);
                }
            }
        }

        // 对每个节点的children按orderNum排序
        sortDeptTree(returnList);

        return returnList;
    }

    /**
     * 递归排序部门树
     */
    private void sortDeptTree(List<SysDept> deptList) {
        if (deptList == null || deptList.isEmpty()) {
            return;
        }

        // 按orderNum排序
        deptList.sort((a, b) -> {
            if (a.getOrderNum() == null && b.getOrderNum() == null) {
                return 0;
            }
            if (a.getOrderNum() == null) {
                return 1;
            }
            if (b.getOrderNum() == null) {
                return -1;
            }
            return a.getOrderNum().compareTo(b.getOrderNum());
        });

        // 递归排序子节点
        for (SysDept dept : deptList) {
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                sortDeptTree(dept.getChildren());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept insertDept(SysDept dept) throws Exception {
        // 参数验证
        if (dept == null) {
            throw new IllegalArgumentException("部门信息不能为空");
        }
        if (dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        if (dept.getDeptType() == null || dept.getDeptType().trim().isEmpty()) {
            throw new IllegalArgumentException("部门类型不能为空");
        }

        // 获取当前登录用户
        LoginUser currentUser = getCurrentLoginUser();
        if (currentUser == null) {
            throw new Exception("用户未登录");
        }

        boolean isSuperAdmin = currentUser.isSuperAdmin();

        // 获取当前用户的部门信息
        SysDept currentUserDept = null;
        if (currentUser.getDeptId() != null) {
            currentUserDept = selectDeptById(currentUser.getDeptId());
        }
        if (!isSuperAdmin && currentUserDept == null) {
            throw new Exception("当前用户部门信息不存在");
        }

        boolean isSystemAdmin = currentUserDept != null && "1".equals(currentUserDept.getDeptType());

        // ========== 父部门与类型校验 ==========
        Long requestedParentId = dept.getParentId();
        SysDept parentDept = null;
        String newDeptType = dept.getDeptType();

        if (StringUtils.isBlank(newDeptType)) {
            throw new IllegalArgumentException("部门类型不能为空");
        }

        if (requestedParentId == null || requestedParentId <= 0) {
            if (isSuperAdmin || isSystemAdmin) {
                throw new Exception("必须指定上级部门");
            }
            if (currentUserDept != null && "2".equals(currentUserDept.getDeptType())) {
                parentDept = currentUserDept;
                dept.setParentId(parentDept.getDeptId());
            } else {
                throw new Exception("必须指定上级部门");
            }
        } else {
            parentDept = selectDeptById(requestedParentId);
            if (parentDept == null) {
                throw new Exception("父部门不存在");
            }
            dept.setParentId(parentDept.getDeptId());

            if (!isSuperAdmin && !isSystemAdmin) {
                if (currentUserDept == null) {
                    throw new Exception("当前用户部门信息不存在");
                }
                String currentDeptType = currentUserDept.getDeptType();
                if ("2".equals(currentDeptType)) {
                    Long selfDeptId = currentUserDept.getDeptId();
                    if (!Objects.equals(parentDept.getDeptId(), selfDeptId) &&
                        !isAncestorOrSelf(selfDeptId, parentDept)) {
                        throw new Exception("无权限在指定父部门下创建子部门");
                    }
                } else {
                    throw new Exception("当前部门类型无权指定父部门");
                }
            }
        }

        if (parentDept == null) {
            throw new Exception("父部门不存在");
        }

        String parentDeptType = parentDept.getDeptType();
        Integer requestedLevel = dept.getLevel();
        boolean hasRequestedLevel = requestedLevel != null;
        switch (newDeptType) {
            case "2": {
                if (hasRequestedLevel) {
                    if (requestedLevel < 1 || requestedLevel > 3) {
                        throw new IllegalArgumentException("代理部门层级仅支持 1~3 级");
                    }
                    dept.setLevel(requestedLevel);
                } else {
                    if ("1".equals(parentDeptType)) {
                        dept.setLevel(1);
                    } else if ("2".equals(parentDeptType)) {
                        int parentLevel = parentDept.getLevel();
                        if (parentLevel <= 0) {
                            parentLevel = 1;
                        }
                        if (parentLevel >= 3) {
                            throw new Exception("三级代理部门不能再创建代理子部门");
                        }
                        dept.setLevel(parentLevel + 1);
                    } else {
                        throw new Exception("仅允许在系统或代理部门下创建代理部门");
                    }
                }
                break;
            }
            case "3": {
                // 管理员可以直接创建买家总账户部门，不限制父部门类型
                if (!isSuperAdmin && !isSystemAdmin && !"2".equals(parentDeptType)) {
                    throw new Exception("买家总部门必须挂载在代理部门下");
                }
                dept.setLevel(0);
                break;
            }
            case "4": {
                if (!"3".equals(parentDeptType)) {
                    throw new Exception("买家子账户部门必须挂载在买家总部门下");
                }
                if (!isSuperAdmin && !isSystemAdmin) {
                    throw new Exception("只有管理员可以创建买家子账户部门");
                }
                dept.setLevel(0);
                break;
            }
            default:
                throw new Exception("暂不支持创建该类型部门");
        }

        // 检查部门名称唯一性（在确定的父部门下检查）
        int count = checkDeptNameUnique(dept.getDeptName(), dept.getParentId(), null);
        if (count > 0) {
            throw new Exception("在当前部门下已存在同名部门");
        }

        // 设置默认值
        if (dept.getStatus() == null) {
            dept.setStatus("0"); // 正常状态
        }
        if (dept.getDelFlag() == null) {
            dept.setDelFlag("0"); // 未删除
        }
        if (dept.getOrderNum() == null) {
            dept.setOrderNum(0);
        }

        Long leaderUserId = dept.getLeaderUserId();
        boolean parentIsCreatorDept = currentUserDept != null
            && Objects.equals(dept.getParentId(), currentUserDept.getDeptId());
        if (leaderUserId == null || leaderUserId <= 0) {
            if (isSuperAdmin || isSystemAdmin || !parentIsCreatorDept) {
                throw new Exception("必须指定部门负责人");
            }
            dept.setLeaderUserId(currentUser.getUserId());
            if (StringUtils.isBlank(dept.getLeader())) {
                dept.setLeader(currentUser.getUsername());
            }
            log.info("设置部门 {} 的负责人为用户ID: {}, 用户名: {}",
                dept.getDeptName(), currentUser.getUserId(), currentUser.getUsername());
        } else {
            SysUser leaderUser = userMapper.selectUserById(leaderUserId);
            if (leaderUser == null) {
                throw new Exception("指定的负责人用户不存在");
            }
            if (!Objects.equals(leaderUser.getDeptId(), dept.getParentId())) {
                throw new Exception("负责人必须归属父部门");
            }
            if (StringUtils.isBlank(dept.getLeader())) {
                String leaderName = StringUtils.isNotBlank(leaderUser.getNickname())
                    ? leaderUser.getNickname()
                    : leaderUser.getUsername();
                dept.setLeader(leaderName);
            }
            dept.setLeaderUserId(leaderUser.getUserId());
            log.info("设置新部门 {} 的负责人为用户ID: {}, 显示名称: {}",
                dept.getDeptName(), leaderUser.getUserId(), dept.getLeader());
        }

        // 构建祖级路径
        buildAncestors(dept);

        // 插入部门
        deptMapper.insertDept(dept);

        // 重新查询部门信息，确保获取数据库中的实际值（可能有触发器或其他逻辑修改了数据）
        SysDept result = selectDeptById(dept.getDeptId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(SysDept dept) throws Exception {
        if (dept == null || dept.getDeptId() == null) {
            throw new IllegalArgumentException("部门ID不能为空");
        }

        // 获取原部门信息
        SysDept oldDept = selectDeptById(dept.getDeptId());
        if (oldDept == null) {
            throw new Exception("部门不存在");
        }

        // 检查部门名称唯一性
        int count = checkDeptNameUnique(dept.getDeptName(), dept.getParentId(), dept.getDeptId());
        if (count > 0) {
            throw new Exception("部门名称已存在");
        }

        // 检查层级关系变更
        if (!Objects.equals(oldDept.getParentId(), dept.getParentId())) {
            // 检查是否会产生循环引用
            if (wouldCreateCycle(dept.getDeptId(), dept.getParentId())) {
                throw new Exception("部门层级关系变更会产生循环引用");
            }
            // 重新构建祖级路径
            buildAncestors(dept);
        }

        // 更新部门
        return deptMapper.updateDept(dept) > 0;
    }

    private boolean isAncestorOrSelf(Long ancestorDeptId, SysDept dept) {
        if (ancestorDeptId == null || ancestorDeptId <= 0 || dept == null) {
            return false;
        }
        if (Objects.equals(ancestorDeptId, dept.getDeptId())) {
            return true;
        }
        String ancestors = dept.getAncestors();
        if (StringUtils.isBlank(ancestors)) {
            return false;
        }
        String[] parts = ancestors.split(",");
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                long value = Long.parseLong(part.trim());
                if (value == ancestorDeptId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeptById(Long deptId) throws Exception {
        if (deptId == null || deptId <= 0) {
            throw new IllegalArgumentException("部门ID不能为空");
        }

        // 检查是否存在子部门
        int childCount = countChildrenByDeptId(deptId);
        if (childCount > 0) {
            throw new Exception("存在子部门，无法删除");
        }

        // 检查是否存在用户
        int userCount = countUsersByDeptId(deptId);
        if (userCount > 0) {
            throw new Exception("部门下存在用户，无法删除");
        }

        // 删除部门
        return deptMapper.deleteDeptById(deptId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeptByIds(List<Long> deptIds) throws Exception {
        if (deptIds == null || deptIds.isEmpty()) {
            throw new IllegalArgumentException("部门ID列表不能为空");
        }

        boolean success = true;
        for (Long deptId : deptIds) {
            try {
                if (!deleteDeptById(deptId)) {
                    success = false;
                }
            } catch (Exception e) {
                log.error("删除部门失败：部门ID={}", deptId, e);
                success = false;
            }
        }

        return success;
    }

    @Override
    public int checkDeptNameUnique(String deptName, Long parentId, Long deptId) {
        return deptMapper.checkDeptNameUnique(deptName, parentId, deptId);
    }

    @Override
    public int countChildrenByDeptId(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return 0;
        }
        return deptMapper.countChildrenByDeptId(deptId);
    }

    @Override
    public int countUsersByDeptId(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return 0;
        }
        return deptMapper.countUsersByDeptId(deptId);
    }

    @Override
    public String getDeptFullPath(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return "";
        }

        List<String> pathNames = new ArrayList<>();
        buildDeptPath(deptId, pathNames);
        
        // 反转路径并拼接
        Collections.reverse(pathNames);
        return String.join("/", pathNames);
    }

    /**
     * 递归构建部门路径
     *
     * @param deptId 部门ID
     * @param pathNames 路径名称列表
     */
    private void buildDeptPath(Long deptId, List<String> pathNames) {
        SysDept dept = selectDeptById(deptId);
        if (dept != null) {
            pathNames.add(dept.getDeptName());
            if (dept.getParentId() != null && dept.getParentId() != 0) {
                buildDeptPath(dept.getParentId(), pathNames);
            }
        }
    }

    @Override
    public int getDeptLevel(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return 0;
        }

        SysDept dept = selectDeptById(deptId);
        if (dept == null) {
            return 0;
        }

        String ancestors = dept.getAncestors();
        if (ancestors == null || ancestors.trim().isEmpty()) {
            return 1; // 根部门
        }

        return ancestors.split(",").length + 1;
    }

    @Override
    public boolean isLeafDept(Long deptId) {
        return countChildrenByDeptId(deptId) == 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveDept(Long deptId, Long newParentId) throws Exception {
        if (deptId == null || deptId <= 0) {
            throw new IllegalArgumentException("部门ID不能为空");
        }

        // 检查是否会产生循环引用
        if (wouldCreateCycle(deptId, newParentId)) {
            throw new Exception("部门移动会产生循环引用");
        }

        SysDept dept = selectDeptById(deptId);
        if (dept == null) {
            throw new Exception("部门不存在");
        }

        dept.setParentId(newParentId);
        buildAncestors(dept);

        return updateDept(dept);
    }

    @Override
    public Map<String, Object> getDeptStatistics(Long deptId) {
        Map<String, Object> statistics = new HashMap<>();

        if (deptId == null || deptId <= 0) {
            return statistics;
        }

        // ===== 权限验证 =====
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 排除买家子账户用户（客户端用户无权查看统计信息）
        SysUser currentUserObj = getCurrentUser();
        if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
            throw new SecurityException("客户端用户无权查看统计信息");
        }

        // 检查部门数据权限
        if (!hasDeptDataPermission(deptId)) {
            throw new SecurityException("无权访问该部门的统计信息");
        }

        // 子部门数量
        int childCount = countChildrenByDeptId(deptId);
        statistics.put("childCount", childCount);

        // 用户数量
        int userCount = countUsersByDeptId(deptId);
        statistics.put("userCount", userCount);

        // 是否为叶子节点
        statistics.put("isLeaf", isLeafDept(deptId));

        // 层级深度
        statistics.put("level", getDeptLevel(deptId));

        return statistics;
    }

    @Override
    public int countUsersByDataScope(Long deptId, String dataScope) {
        if (deptId == null || deptId <= 0 || dataScope == null) {
            return 0;
        }

        switch (dataScope) {
            case "1": // 全部数据权限
                return deptMapper.countAllUsers();
            case "2": // 自定义数据权限
                return deptMapper.countUsersByCustomDept(deptId);
            case "3": // 本部门数据权限
                return countUsersByDeptId(deptId);
            case "4": // 本部门及以下数据权限
                List<Long> childDeptIds = selectChildDeptIds(deptId);
                return deptMapper.countUsersByDeptIds(childDeptIds);
            default:
                return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncDeptHierarchy(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return false;
        }

        try {
            SysDept dept = selectDeptById(deptId);
            if (dept == null) {
                return false;
            }

            // 重新构建祖级路径
            buildAncestors(dept);

            // 更新部门信息
            return deptMapper.updateDept(dept) > 0;
        } catch (Exception e) {
            log.error("同步部门层级数据失败：部门ID={}", deptId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> validateDeptData(Long deptId) {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (deptId == null || deptId <= 0) {
            issues.add("部门ID无效");
            result.put("valid", false);
            result.put("issues", issues);
            result.put("suggestions", suggestions);
            return result;
        }

        SysDept dept = selectDeptById(deptId);
        if (dept == null) {
            issues.add("部门不存在");
            result.put("valid", false);
            result.put("issues", issues);
            result.put("suggestions", suggestions);
            return result;
        }

        // 检查祖级路径
        if (dept.getAncestors() != null && !dept.getAncestors().trim().isEmpty()) {
            String[] ancestorIds = dept.getAncestors().split(",");
            for (String ancestorId : ancestorIds) {
                if (ancestorId.equals(deptId.toString())) {
                    issues.add("部门祖级路径包含自身，存在循环引用");
                    suggestions.add("重新构建部门层级关系");
                    break;
                }
            }
        }

        // 检查父部门是否存在
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            SysDept parent = selectDeptById(dept.getParentId());
            if (parent == null) {
                issues.add("父部门不存在");
                suggestions.add("设置正确的父部门或设为根部门");
            }
        }

        result.put("valid", issues.isEmpty());
        result.put("issues", issues);
        result.put("suggestions", suggestions);

        return result;
    }

    /**
     * 构建部门祖级路径
     *
     * @param dept 部门对象
     */
    private void buildAncestors(SysDept dept) {
        if (dept.getParentId() == null || dept.getParentId() == 0) {
            dept.setAncestors("0");
        } else {
            SysDept parent = selectDeptById(dept.getParentId());
            if (parent != null) {
                String parentAncestors = parent.getAncestors();
                if (parentAncestors == null || parentAncestors.trim().isEmpty() || "0".equals(parentAncestors)) {
                    // 如果父部门是根部门（ancestors="0"），则子部门的ancestors设置为父部门ID
                    dept.setAncestors(parent.getDeptId().toString());
                } else {
                    // 正常情况：在父部门的ancestors基础上加上父部门ID
                    dept.setAncestors(parentAncestors + "," + parent.getDeptId());
                }
            } else {
                dept.setAncestors("0");
            }
        }
    }

    /**
     * 检查是否会产生循环引用
     *
     * @param deptId 部门ID
     * @param parentId 父部门ID
     * @return 是否会产生循环引用
     */
    private boolean wouldCreateCycle(Long deptId, Long parentId) {
        if (parentId == null || parentId == 0) {
            return false;
        }

        if (deptId.equals(parentId)) {
            return true;
        }

        // 递归检查父部门的祖先路径
        SysDept parent = selectDeptById(parentId);
        if (parent != null) {
            return wouldCreateCycle(deptId, parent.getParentId());
        }

        return false;
    }

    // ==================== 部门权限相关方法实现 ====================

    @Override
    public boolean hasDeptDataPermission(Long deptId) {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null || deptId == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (loginUser.isAdmin()) {
            return true;
        }

        // 获取用户的数据权限范围
        String dataScope = getUserDataScope(loginUser);
        if (dataScope == null) {
            return false;
        }

        Long userDeptId = loginUser.getDeptId();
        List<Long> userChildDeptIds = selectChildDeptIds(userDeptId);

        // 调用工具类进行权限计算
        return DeptUtils.calculateDataScopePermission(dataScope, userDeptId, deptId, userChildDeptIds);
    }

    @Override
    public boolean checkUserDataPermission(Long userId, Long targetDeptId) {
        if (userId == null || targetDeptId == null) {
            return false;
        }

        try {
            // 获取当前用户信息，如果userId与当前用户一致则使用当前用户
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            LoginUser loginUser = null;

            if (currentUser != null && userId.equals(currentUser.getUserId())) {
                // 使用当前用户信息
                loginUser = currentUser;
            } else {
                // 对于非当前用户的权限检查，暂不支持，返回false
                // 实际项目中可以：
                // 1. 从数据库查询用户信息
                // 2. 从缓存获取用户信息
                // 3. 通过参数传递用户信息
                log.warn("不支持查询其他用户权限：用户ID={}", userId);
                return false;
            }

            // 超级管理员拥有所有权限
            if (loginUser.isAdmin()) {
                return true;
            }

            // 获取用户的数据权限范围
            String dataScope = getUserDataScope(loginUser);
            if (dataScope == null) {
                return false;
            }

            Long userDeptId = loginUser.getDeptId();
            List<Long> userChildDeptIds = selectChildDeptIds(userDeptId);

            // 调用工具类进行权限计算
            return DeptUtils.calculateDataScopePermission(dataScope, userDeptId, targetDeptId, userChildDeptIds);
        } catch (Exception e) {
            log.error("检查用户数据权限失败：用户ID={}, 目标部门ID={}", userId, targetDeptId, e);
            return false;
        }
    }

    @Override
    public List<Long> getAccessibleDeptIds() {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null) {
            return new ArrayList<>();
        }

        return getAccessibleDeptIds(loginUser.getUserId());
    }

    @Override
    public List<Long> getAccessibleDeptIds(Long userId) {
        try {
            // 获取当前用户信息，如果userId与当前用户一致则使用当前用户
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            LoginUser loginUser = null;

            if (currentUser != null && userId.equals(currentUser.getUserId())) {
                // 使用当前用户信息
                loginUser = currentUser;
            } else {
                // 对于非当前用户的权限检查，暂不支持，返回空列表
                log.warn("不支持查询其他用户权限：用户ID={}", userId);
                return new ArrayList<>();
            }

            // 超级管理员可以访问所有部门
            if (loginUser.isAdmin()) {
                List<SysDept> allDepts = selectDeptList(null);
                return allDepts.stream()
                        .map(SysDept::getDeptId)
                        .collect(Collectors.toList());
            }

            String dataScope = getUserDataScope(loginUser);
            Long userDeptId = loginUser.getDeptId();

            switch (dataScope) {
                case DeptUtils.DataScope.ALL:
                    // 全部数据权限，返回所有部门ID
                    List<SysDept> allDepts = selectDeptList(null);
                    return allDepts.stream()
                            .map(SysDept::getDeptId)
                            .collect(Collectors.toList());

                case DeptUtils.DataScope.CUSTOM:
                    // 自定义数据权限，这里可以根据具体业务需求实现
                    // 当前实现：返回用户所在部门及其子部门
                    return selectChildDeptIds(userDeptId);

                case DeptUtils.DataScope.DEPT:
                    // 本部门数据权限
                    return userDeptId != null ? List.of(userDeptId) : new ArrayList<>();

                case DeptUtils.DataScope.DEPT_AND_CHILD:
                    // 本部门及以下数据权限
                    return selectChildDeptIds(userDeptId);

                default:
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("获取用户可访问部门ID失败：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean canManageDept(Long deptId) {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null || deptId == null) {
            return false;
        }

        // 管理员可以管理所有部门
        if (loginUser.isAdmin()) {
            return true;
        }

        // 检查是否有部门管理权限
        Set<String> permissions = loginUser.getPermissions();
        if (permissions != null && (
                permissions.contains("system:dept:manage") ||
                permissions.contains("dept:manage") ||
                permissions.contains("system:dept:edit")
        )) {
            return hasDeptDataPermission(deptId);
        }

        return false;
    }

    @Override
    public boolean canViewDeptUsers(Long userDeptId) {
        return hasDeptDataPermission(userDeptId);
    }

    @Override
    public String getDeptDisplayName(Long deptId) {
        if (deptId == null) {
            return "未分配部门";
        }

        try {
            SysDept dept = selectDeptById(deptId);
            return DeptUtils.getDeptDisplayName(dept);
        } catch (Exception e) {
            log.error("获取部门显示名称失败：部门ID={}", deptId, e);
            return "未知部门";
        }
    }

    @Override
    public boolean isChildDept(Long parentDeptId, Long childDeptId) {
        if (parentDeptId == null || childDeptId == null) {
            return false;
        }

        // 如果是同一个部门，返回true
        if (parentDeptId.equals(childDeptId)) {
            return true;
        }

        try {
            List<Long> childDeptIds = selectChildDeptIds(parentDeptId);
            return DeptUtils.isChildDeptByList(childDeptIds, childDeptId);
        } catch (Exception e) {
            log.error("检查子部门关系失败：父部门ID={}, 子部门ID={}", parentDeptId, childDeptId, e);
            return false;
        }
    }

    @Override
    public String getUserDataScope(LoginUser loginUser) {
        if (loginUser == null) {
            return null;
        }

        Set<String> roles = loginUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        // 如果用户有管理员角色，返回全部数据权限
        if (roles.contains("admin")) {
            return DeptUtils.DataScope.ALL;
        }

        // 根据角色的优先级返回数据权限范围
        // 实际项目中应该从数据库中查询角色的data_scope字段
        // 这里提供一个简化的实现

        // 检查是否有特殊权限角色
        if (roles.contains("dept_manager") || roles.contains("department_manager")) {
            return DeptUtils.DataScope.DEPT_AND_CHILD;
        }

        // 默认返回本部门数据权限
        return DeptUtils.DataScope.DEPT;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept createBuyerSubAccountDept(SysDept dept, Long parentBuyerDeptId) throws Exception {
        if (dept == null) {
            throw new IllegalArgumentException("部门信息不能为空");
        }

        if (parentBuyerDeptId == null || parentBuyerDeptId <= 0) {
            throw new IllegalArgumentException("父买家总账户部门ID不能为空");
        }

        // 验证父部门是否存在且为买家总账户部门
        SysDept parentDept = selectDeptById(parentBuyerDeptId);
        if (parentDept == null) {
            throw new Exception("父买家总账户部门不存在");
        }

        if (!"3".equals(parentDept.getDeptType())) {
            throw new Exception("父部门必须为买家总账户部门");
        }

        // 设置部门类型为买家子账户部门
        dept.setDeptType("4");
        dept.setParentId(parentBuyerDeptId);

        // 设置层级（买家子账户部门固定为2级）
        dept.setLevel(2);

        // 构建祖先路径
        String ancestors = parentDept.getAncestors();
        if (ancestors == null || ancestors.trim().isEmpty()) {
            dept.setAncestors(parentBuyerDeptId.toString());
        } else {
            dept.setAncestors(ancestors + "," + parentBuyerDeptId);
        }

        // 设置默认状态
        dept.setStatus("0");
        dept.setDelFlag("0");

        // 插入部门
        SysDept result = insertDept(dept);
        if (result == null) {
            throw new Exception("创建买家子账户部门失败");
        }

        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept createAgentDept(SysDept dept, Long parentDeptId) throws Exception {
        if (dept == null) {
            throw new IllegalArgumentException("部门信息不能为空");
        }

        if (parentDeptId == null || parentDeptId <= 0) {
            throw new IllegalArgumentException("父部门ID不能为空");
        }

        // 验证父部门是否存在且为代理部门或系统部门
        SysDept parentDept = selectDeptById(parentDeptId);
        if (parentDept == null) {
            throw new Exception("父部门不存在");
        }

        // 只有系统部门和代理部门可以创建代理部门
        if (!"1".equals(parentDept.getDeptType()) && !"2".equals(parentDept.getDeptType())) {
            throw new Exception("父部门必须为系统部门或代理部门");
        }

        // 检查父部门是否可以创建下级代理
        if (!parentDept.canCreateChildAgent()) {
            throw new Exception("父部门不能创建下级代理部门");
        }

        // 设置部门类型为代理部门
        dept.setDeptType("2");
        dept.setParentId(parentDeptId);

        // 设置层级（继承父部门层级+1）
        Integer parentLevel = parentDept.getLevel();
        if (parentLevel == null) {
            parentLevel = 0;
        }
        dept.setLevel(parentLevel + 1);

        // 构建祖先路径
        String ancestors = parentDept.getAncestors();
        if (ancestors == null || ancestors.trim().isEmpty()) {
            dept.setAncestors(parentDeptId.toString());
        } else {
            dept.setAncestors(ancestors + "," + parentDeptId);
        }

        // 设置默认状态
        dept.setStatus("0");
        dept.setDelFlag("0");

        // 插入部门
        SysDept result = insertDept(dept);
        if (result == null) {
            throw new Exception("创建代理部门失败");
        }

        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept createBuyerMainAccountDept(SysDept dept, Long parentAgentDeptId) throws Exception {
        if (dept == null) {
            throw new IllegalArgumentException("部门信息不能为空");
        }

        if (parentAgentDeptId == null || parentAgentDeptId <= 0) {
            throw new IllegalArgumentException("父代理部门ID不能为空");
        }

        // 验证父代理部门是否存在且为代理部门
        SysDept parentDept = selectDeptById(parentAgentDeptId);
        if (parentDept == null) {
            throw new Exception("父代理部门不存在");
        }

        if (!"2".equals(parentDept.getDeptType())) {
            throw new Exception("父部门必须为代理部门");
        }

        // 设置部门类型为买家总账户部门
        dept.setDeptType("3");
        dept.setParentId(parentAgentDeptId);

        // 设置层级（买家总账户部门相对于子账户为1级）
        dept.setLevel(1);

        // 构建祖先路径
        String ancestors = parentDept.getAncestors();
        if (ancestors == null || ancestors.trim().isEmpty()) {
            dept.setAncestors(parentAgentDeptId.toString());
        } else {
            dept.setAncestors(ancestors + "," + parentAgentDeptId);
        }

        // 设置默认状态
        dept.setStatus("0");
        dept.setDelFlag("0");

        // 插入部门
        SysDept result = insertDept(dept);
        if (result == null) {
            throw new Exception("创建买家总账户部门失败");
        }

        return dept;
    }

    @Override
    public java.util.Map<String, Object> getDeptOrgInfo(Long deptId) throws Exception {
        java.util.Map<String, Object> orgInfo = new java.util.HashMap<>();

        if (deptId == null || deptId <= 0) {
            return orgInfo;
        }

        try {
            // 获取部门信息
            SysDept dept = selectDeptById(deptId);
            if (dept == null) {
                return orgInfo;
            }

            // 部门基本信息
            orgInfo.put("deptId", dept.getDeptId());
            orgInfo.put("deptName", dept.getDeptName());
            orgInfo.put("deptType", dept.getDeptType());
            orgInfo.put("deptTypeDisplay", dept.getDeptTypeDisplay());
            orgInfo.put("level", dept.getLevel());
            orgInfo.put("levelDisplay", dept.getLevelDisplay());
            orgInfo.put("ancestors", dept.getAncestors());
            orgInfo.put("fullPath", dept.getFullPath());
            orgInfo.put("status", dept.getStatus());
            orgInfo.put("parentId", dept.getParentId());
            orgInfo.put("orderNum", dept.getOrderNum());
            orgInfo.put("leader", dept.getLeader());
            orgInfo.put("phone", dept.getPhone());
            orgInfo.put("email", dept.getEmail());

            // 获取父部门信息
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                SysDept parentDept = selectDeptById(dept.getParentId());
                if (parentDept != null) {
                    orgInfo.put("parentId", parentDept.getDeptId());
                    orgInfo.put("parentName", parentDept.getDeptName());
                    orgInfo.put("parentType", parentDept.getDeptType());
                    orgInfo.put("parentTypeDisplay", parentDept.getDeptTypeDisplay());
                }
            }

            // 获取子部门信息
            List<SysDept> childDepts = selectChildrenByParentId(deptId);
            orgInfo.put("childCount", childDepts.size());

            java.util.List<java.util.Map<String, Object>> childInfo = new java.util.ArrayList<>();
            for (SysDept child : childDepts) {
                java.util.Map<String, Object> childMap = new java.util.HashMap<>();
                childMap.put("deptId", child.getDeptId());
                childMap.put("deptName", child.getDeptName());
                childMap.put("deptType", child.getDeptType());
                childMap.put("deptTypeDisplay", child.getDeptTypeDisplay());
                childMap.put("level", child.getLevel());
                childMap.put("status", child.getStatus());
                childInfo.add(childMap);
            }
            orgInfo.put("children", childInfo);

            // 获取部门用户信息
            List<SysUser> deptUsers = userMapper.selectUsersByDeptId(deptId, null);
            orgInfo.put("userCount", deptUsers.size());

            java.util.List<java.util.Map<String, Object>> userInfo = new java.util.ArrayList<>();
            for (SysUser user : deptUsers) {
                java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("nickname", user.getNickname());
                userMap.put("email", user.getEmail());
                userMap.put("phone", user.getPhone());
                userMap.put("status", user.getStatus());
                userMap.put("userType", user.getUserType());
                userMap.put("userTypeDisplay", user.getUserTypeDisplay());
                userMap.put("createTime", user.getCreateTime());
                userInfo.add(userMap);
            }
            orgInfo.put("users", userInfo);

            // 业务能力信息
            orgInfo.put("canCreateChildDept", dept.canCreateChildDept());
            orgInfo.put("canCreateBuyerAccount", dept.canCreateBuyerAccount());
            orgInfo.put("canCreateSubAccount", dept.canCreateSubAccount());
            orgInfo.put("isLeafDept", dept.isLeafDept());

            // 统计信息
            orgInfo.put("createTime", dept.getCreateTime());
            orgInfo.put("updateTime", dept.getUpdateTime());

        } catch (Exception e) {
            log.error("获取部门组织架构信息异常：部门ID={}", deptId, e);
            throw new Exception("获取部门组织架构信息失败：" + e.getMessage(), e);
        }

        return orgInfo;
    }

    @Override
    public java.util.Map<String, Object> getAgentLevelStatistics() throws Exception {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 排除买家子账户用户（客户端用户无权查看统计信息）
            SysUser currentUserObj = getCurrentUser();
            if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
                throw new SecurityException("客户端用户无权查看统计信息");
            }

            // 获取所有代理部门
            SysDept queryDept = new SysDept();
            queryDept.setDeptType("2"); // 代理部门
            List<SysDept> agentDepts = selectDeptList(queryDept);

            // 按代理层级统计
            java.util.Map<Integer, Integer> levelCount = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> userCountByLevel = new java.util.HashMap<>();
            int totalAgentDepts = 0;
            int totalAgentUsers = 0;

            for (SysDept agentDept : agentDepts) {
                Integer level = agentDept.getLevel();
                if (level != null && level > 0) {
                    levelCount.put(level, levelCount.getOrDefault(level, 0) + 1);

                    // 统计该代理部门的用户数量（需要通过SysUserService）
                    // 这里暂时使用简单计数，实际应该注入SysUserService
                    int deptUserCount = 0; // TODO: 实现用户计数逻辑
                    userCountByLevel.put(level, userCountByLevel.getOrDefault(level, 0) + deptUserCount);

                    totalAgentUsers += deptUserCount;
                }
                totalAgentDepts++;
            }

            statistics.put("totalAgentDepts", totalAgentDepts);
            statistics.put("totalAgentUsers", totalAgentUsers);
            statistics.put("levelDistribution", levelCount);
            statistics.put("userDistributionByLevel", userCountByLevel);

            // 添加各级代理的详细信息
            java.util.List<java.util.Map<String, Object>> levelDetails = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Integer, Integer> entry : levelCount.entrySet()) {
                java.util.Map<String, Object> levelInfo = new java.util.HashMap<>();
                levelInfo.put("level", entry.getKey());
                levelInfo.put("deptCount", entry.getValue());
                levelInfo.put("userCount", userCountByLevel.getOrDefault(entry.getKey(), 0));
                levelInfo.put("levelDisplay", entry.getKey() + "级代理");
                levelDetails.add(levelInfo);
            }
            statistics.put("levelDetails", levelDetails);

        } catch (Exception e) {
            log.error("获取代理层级统计信息异常", e);
            throw new Exception("获取代理层级统计信息失败：" + e.getMessage(), e);
        }

        return statistics;
    }

    @Override
    public java.util.Map<String, Object> getDeptTypeStatistics() throws Exception {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 排除买家子账户用户（客户端用户无权查看统计信息）
            SysUser currentUserObj = getCurrentUser();
            if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
                throw new SecurityException("客户端用户无权查看统计信息");
            }

            // 获取所有部门
            List<SysDept> allDepts = selectDeptList(new SysDept());

            // 按部门类型统计
            java.util.Map<String, Integer> deptTypeCount = new java.util.HashMap<>();
            java.util.Map<String, Integer> userCountByDeptType = new java.util.HashMap<>();

            for (SysDept dept : allDepts) {
                String deptTypeDisplay = dept.getDeptTypeDisplay();
                deptTypeCount.put(deptTypeDisplay, deptTypeCount.getOrDefault(deptTypeDisplay, 0) + 1);

                // 统计该部门下的用户数量
                List<SysUser> deptUsers = userMapper.selectUsersByDeptId(dept.getDeptId(), null);
                userCountByDeptType.put(deptTypeDisplay, userCountByDeptType.getOrDefault(deptTypeDisplay, 0) + deptUsers.size());
            }

            // 统计总用户数
            List<SysUser> allUsers = userMapper.selectUserList(new SysUser());
            int totalUsers = allUsers.size();

            // 按用户类型统计
            java.util.Map<String, Integer> userTypeCount = new java.util.HashMap<>();
            for (SysUser user : allUsers) {
                String userType = user.getUserTypeDisplay();
                userTypeCount.put(userType, userTypeCount.getOrDefault(userType, 0) + 1);
            }

            statistics.put("totalDepartments", allDepts.size());
            statistics.put("totalUsers", totalUsers);
            statistics.put("departmentTypeStatistics", deptTypeCount);
            statistics.put("userTypeStatistics", userTypeCount);
            statistics.put("userDistributionByDeptType", userCountByDeptType);

        } catch (Exception e) {
            log.error("获取部门类型统计信息异常", e);
            throw new Exception("获取部门类型统计信息失败：" + e.getMessage(), e);
        }

        return statistics;
    }

    /**
     * 获取当前登录用户的SysUser对象
     *
     * 由于LoginUser中没有getUser()方法，通过userId查询数据库获取完整的SysUser信息
     *
     * @return 当前登录用户的SysUser对象，如果用户不存在则返回null
     */
    private LoginUser getCurrentLoginUser() {
        return SecurityUtils.getCurrentLoginUser();
    }

    private SysUser getCurrentUser() {
        LoginUser loginUser = getCurrentLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }

        // 直接使用mapper查询用户完整信息（包含部门）
return userMapper.selectUserWithDept(loginUser.getUserId());
    }

    @Override
    public boolean canCreateSubAccount(Long deptId) throws Exception {
        if (deptId == null || deptId <= 0) {
            return false;
        }

        try {
            SysDept dept = selectDeptById(deptId);
            if (dept == null) {
                return false;
            }

            return dept.canCreateSubAccount();

        } catch (Exception e) {
            log.error("检查部门是否可以创建子账户异常：部门ID={}", deptId, e);
            return false;
        }
    }

    @Override
    public boolean canCreateBuyerAccount(Long deptId) throws Exception {
        if (deptId == null || deptId <= 0) {
            return false;
        }

        try {
            SysDept dept = selectDeptById(deptId);
            if (dept == null) {
                return false;
            }

            return dept.canCreateBuyerAccount();

        } catch (Exception e) {
            log.error("检查部门是否可以创建买家账户异常：部门ID={}", deptId, e);
            return false;
        }
    }

    @Override
    public boolean canCreateChildAgent(Long deptId) throws Exception {
        if (deptId == null || deptId <= 0) {
            return false;
        }

        try {
            SysDept dept = selectDeptById(deptId);
            if (dept == null) {
                return false;
            }

            return dept.canCreateChildAgent();

        } catch (Exception e) {
            log.error("检查部门是否可以创建下级代理异常：部门ID={}", deptId, e);
            return false;
        }
    }

    @Override
    public List<SysDept> selectBuyerMainAccountsByParentId(Long parentId) throws Exception {
        if (parentId == null || parentId <= 0) {
            return new ArrayList<>();
        }

        try {
            // 根据父代理部门ID查询买家总账户部门列表
            return deptMapper.selectBuyerMainAccountsByParentId(parentId);
        } catch (Exception e) {
            log.error("查询买家总账户部门列表异常：父代理部门ID={}", parentId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysDept> selectBuyerSubAccountsByParentId(Long parentBuyerDeptId) throws Exception {
        if (parentBuyerDeptId == null || parentBuyerDeptId <= 0) {
            return new ArrayList<>();
        }

        try {
            // 根据父买家部门ID查询子账户部门列表
            return deptMapper.selectBuyerSubAccountsByParentId(parentBuyerDeptId);
        } catch (Exception e) {
            log.error("查询买家子账户部门列表异常：父买家部门ID={}", parentBuyerDeptId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void validateDeptCreatePermission(SysDept dept) throws Exception {
        if (dept == null) {
            throw new Exception("部门信息不能为空");
        }

        // 获取当前登录用户
        LoginUser currentUser = getCurrentLoginUser();
        if (currentUser == null) {
            throw new Exception("用户未登录");
        }

        // 获取当前用户的部门信息
        SysDept currentUserDept = selectDeptById(currentUser.getDeptId());
        if (currentUserDept == null) {
            throw new Exception("当前用户部门信息不存在");
        }

        // 检查用户是否有创建部门的权限
        if (!currentUserDept.canCreateChildDept()) {
            throw new Exception("当前用户没有创建下级部门的权限");
        }

        // 检查部门类型的合法性
        String parentDeptType = currentUserDept.getDeptType();
        String newDeptType = dept.getDeptType();

        // 验证部门类型的创建权限
        switch (parentDeptType) {
            case "1": // 系统部门
                if (!"1".equals(newDeptType) && !"2".equals(newDeptType)) {
                    throw new Exception("系统部门只能创建系统部门和代理部门");
                }
                break;
            case "2": // 代理部门
                Integer agentLevel = currentUserDept.getLevel();
                if (agentLevel != null && agentLevel >= 3) {
                    // 三级代理只能创建买家总账户
                    if (!"3".equals(newDeptType)) {
                        throw new Exception("三级代理只能创建买家总账户部门");
                    }
                } else {
                    // 一级、二级代理可以创建下级代理部门和买家总账户
                    if (!"2".equals(newDeptType) && !"3".equals(newDeptType)) {
                        throw new Exception("代理部门只能创建下级代理部门和买家总账户部门");
                    }
                }
                break;
            case "3": // 买家总账户
                if (!"4".equals(newDeptType)) {
                    throw new Exception("买家总账户只能创建买家子账户部门");
                }
                break;
            case "4": // 买家子账户
                throw new Exception("买家子账户不能创建下级部门");
            default:
                throw new Exception("未知的部门类型");
        }
    }

    @Override
    public List<SysDept> selectChildAgentsRecursive(Long deptId) throws Exception {
        if (deptId == null || deptId <= 0) {
            return new ArrayList<>();
        }

        try {
            // 递归查询所有下级代理部门
            return deptMapper.selectChildAgentsRecursive(deptId);
        } catch (Exception e) {
            log.error("递归查询下级代理部门异常：部门ID={}", deptId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysDept> selectAgentHierarchy() throws Exception {
        try {
            // 查询代理层级结构
            return deptMapper.selectAgentHierarchy();
        } catch (Exception e) {
            log.error("查询代理层级结构异常", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysDept> selectAgentsByLevel(Integer level) throws Exception {
        if (level == null || level <= 0 || level > 3) {
            return new ArrayList<>();
        }

        try {
            // 根据层级查询代理部门
            return deptMapper.selectAgentsByLevel(level);
        } catch (Exception e) {
            log.error("根据层级查询代理部门异常：层级={}", level, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysDept> selectDeptsByDeptType(String deptType) throws Exception {
        if (deptType == null || deptType.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 根据部门类型查询部门列表
            return deptMapper.selectDeptsByDeptType(deptType);
        } catch (Exception e) {
            log.error("根据部门类型查询部门列表异常：deptType={}", deptType, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysDept> selectDeptsByDeptTypeAndLevel(String deptType, Integer level) throws Exception {
        if (deptType == null || deptType.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 根据部门类型和层级查询部门列表
            return deptMapper.selectDeptsByDeptTypeAndLevel(deptType, level);
        } catch (Exception e) {
            log.error("根据部门类型和层级查询部门列表异常：deptType={}, level={}", deptType, level, e);
            return new ArrayList<>();
        }
    }

    // ==================== 统计方法实现 ====================

    @Override
    public Map<String, Object> getManagedDeptsStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            if (userId == null) {
                return statistics;
            }

            // 获取用户管理的所有部门ID
            Set<Long> managedDeptIds = getManagedDeptIds(userId);
            if (managedDeptIds.isEmpty()) {
                // 初始化所有统计为0
                initializeDeptStatistics(statistics, 0L, 0L, 0L, 0L, 0L, 0L);
                return statistics;
            }

            // 统计各类型部门数量
            List<Map<String, Object>> deptTypeStatsList = deptMapper.countDeptsByTypeAndIds(managedDeptIds);

            // 统计代理层级分布
            List<Map<String, Object>> agentLevelStatsList = deptMapper.countAgentDeptsByLevelAndIds(managedDeptIds);

            // 初始化计数器
            Long systemDeptCount = 0L;
            Long agentDeptCount = 0L;
            Long level1AgentCount = 0L;
            Long level2AgentCount = 0L;
            Long level3AgentCount = 0L;
            Long buyerMainCount = 0L;
            Long buyerSubCount = 0L;

            // 处理部门类型统计结果
            for (Map<String, Object> stat : deptTypeStatsList) {
                Object deptTypeObj = stat.get("dept_type");
                String deptType = null;

                // 处理dept_type字段可能的类型
                if (deptTypeObj instanceof String) {
                    deptType = (String) deptTypeObj;
                } else if (deptTypeObj instanceof Number) {
                    deptType = String.valueOf(deptTypeObj);
                }

                Object countObj = stat.get("count");
                Long count = 0L;

                if (countObj instanceof Number) {
                    count = ((Number) countObj).longValue();
                } else if (countObj instanceof String) {
                    try {
                        count = Long.parseLong((String) countObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析部门数量: {}", countObj);
                    }
                }

                if (deptType != null) {
                    switch (deptType) {
                        case "1":
                            systemDeptCount = count;
                            break;
                        case "2":
                            agentDeptCount = count;
                            break;
                        case "3":
                            buyerMainCount = count;
                            break;
                        case "4":
                            buyerSubCount = count;
                            break;
                        default:
                            log.warn("未知的部门类型: {}", deptType);
                    }
                }
            }

            // 处理代理层级统计结果
            for (Map<String, Object> stat : agentLevelStatsList) {
                Object levelObj = stat.get("level");
                String level = null;

                // 处理level字段可能的类型
                if (levelObj instanceof String) {
                    level = (String) levelObj;
                } else if (levelObj instanceof Number) {
                    level = String.valueOf(levelObj);
                }

                Object countObj = stat.get("count");
                Long count = 0L;

                if (countObj instanceof Number) {
                    count = ((Number) countObj).longValue();
                } else if (countObj instanceof String) {
                    try {
                        count = Long.parseLong((String) countObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析代理层级数量: {}", countObj);
                    }
                }

                if (level != null) {
                    switch (level) {
                        case "1":
                            level1AgentCount = count;
                            break;
                        case "2":
                            level2AgentCount = count;
                            break;
                        case "3":
                            level3AgentCount = count;
                            break;
                        default:
                            log.warn("未知的代理层级: {}", level);
                    }
                }
            }

            // 构建统计结果 - 将代理部门按层级细分
            initializeDeptStatistics(statistics, systemDeptCount, level1AgentCount, level2AgentCount, level3AgentCount, buyerMainCount, buyerSubCount);

            // 添加详细统计
            statistics.put("totalDepts", systemDeptCount + agentDeptCount + buyerMainCount + buyerSubCount);
            statistics.put("totalAgents", level1AgentCount + level2AgentCount + level3AgentCount);
            statistics.put("managedDeptIds", managedDeptIds);

            log.info("用户 {} 管理的部门统计完成: 系统={}, 一级代理={}, 二级代理={}, 三级代理={}, 买家总={}, 买家子={}",
                    userId, systemDeptCount, level1AgentCount, level2AgentCount, level3AgentCount, buyerMainCount, buyerSubCount);

        } catch (Exception e) {
            log.error("获取管理部门统计信息失败：userId={}", userId, e);
            // 返回默认值
            initializeDeptStatistics(statistics, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getManagedAgentLevelsStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            if (userId == null) {
                return statistics;
            }

            // 获取用户管理的所有代理部门ID
            Set<Long> managedDeptIds = getManagedDeptIds(userId);
            if (managedDeptIds.isEmpty()) {
                initializeAgentStatistics(statistics, 0L, 0L, 0L);
                return statistics;
            }

            // 查询详细部门信息，便于后续统计
            List<SysDept> managedDeptsDetail = deptMapper.selectDeptsByIds(managedDeptIds);

            Long level1Count = 0L;
            Long level2Count = 0L;
            Long level3Count = 0L;
            Map<Long, SysDept> agentDeptMap = managedDeptsDetail.stream()
                    .filter(dept -> dept != null && "2".equals(dept.getDeptType()) && dept.getDeptId() != null)
                    .collect(Collectors.toMap(SysDept::getDeptId, dept -> dept, (left, right) -> left));

            List<SysUser> agentUsers = Collections.emptyList();
            if (!agentDeptMap.isEmpty()) {
                agentUsers = userMapper.selectUsersByDeptIds(new ArrayList<>(agentDeptMap.keySet()));
                if (agentUsers != null) {
                    for (SysUser user : agentUsers) {
                        if (user == null || user.getDeptId() == null) {
                            continue;
                        }
                        SysDept userDept = agentDeptMap.get(user.getDeptId());
                        Integer userDeptLevel = userDept != null ? userDept.getLevel() : null;
                        if (userDeptLevel == null) {
                            continue;
                        }
                        if (userDeptLevel == 1) {
                            level1Count++;
                        } else if (userDeptLevel == 2) {
                            level2Count++;
                        } else if (userDeptLevel == 3) {
                            level3Count++;
                        }
                    }
                }
            } else {
                agentUsers = Collections.emptyList();
            }

            // 构建统计结果
            initializeAgentStatistics(statistics, level1Count, level2Count, level3Count);

            statistics.put("totalAgents", level1Count + level2Count + level3Count);
            Map<String, Object> rechargeStatistics = buildAgentRechargeStatistics(managedDeptsDetail);
            statistics.put("rechargeStatistics", rechargeStatistics);
            statistics.put("commissionStatistics", buildAgentCommissionStatistics(agentDeptMap, agentUsers));
            statistics.put("merchantPerformance", buildMerchantPerformance(managedDeptsDetail));

            log.info("用户 {} 管理的代理层级统计完成: 一级={}, 二级={}, 三级={}, 充值统计={}",
                    userId, level1Count, level2Count, level3Count, rechargeStatistics);

        } catch (Exception e) {
            log.error("获取代理层级统计信息失败：userId={}", userId, e);
            initializeAgentStatistics(statistics, 0L, 0L, 0L);
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getManagedBuyerAccountsStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            if (userId == null) {
                return statistics;
            }

            // 获取用户管理的所有部门ID
            Set<Long> managedDeptIds = getManagedDeptIds(userId);
            if (managedDeptIds.isEmpty()) {
                initializeBuyerStatistics(statistics, 0L, 0L);
                return statistics;
            }

            // 统计买家账户
            List<Map<String, Object>> buyerStatsList = deptMapper.countBuyerAccountsByDeptIds(managedDeptIds);

            Long buyerMainCount = 0L;
            Long buyerSubCount = 0L;

            // 处理买家账户统计结果
            for (Map<String, Object> stat : buyerStatsList) {
                Object accountTypeObj = stat.get("account_type");
                String accountType = null;

                // 处理account_type字段可能的类型
                if (accountTypeObj instanceof String) {
                    accountType = (String) accountTypeObj;
                } else if (accountTypeObj instanceof Number) {
                    accountType = String.valueOf(accountTypeObj);
                }

                Object countObj = stat.get("count");
                Long count = 0L;

                if (countObj instanceof Number) {
                    count = ((Number) countObj).longValue();
                } else if (countObj instanceof String) {
                    try {
                        count = Long.parseLong((String) countObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析买家账户数量: {}", countObj);
                    }
                }

                if (accountType != null) {
                    switch (accountType) {
                        case "buyer_main":
                            buyerMainCount = count;
                            break;
                        case "buyer_sub":
                            buyerSubCount = count;
                            break;
                        default:
                            log.warn("未知的买家账户类型: {}", accountType);
                    }
                }
            }

            // 构建统计结果
            initializeBuyerStatistics(statistics, buyerMainCount, buyerSubCount);

            statistics.put("totalBuyerAccounts", buyerMainCount + buyerSubCount);

            // 获取详细的买家账户分布
            if (buyerMainCount > 0) {
                List<Map<String, Object>> buyerMainDetails = deptMapper.getBuyerMainAccountDetails(managedDeptIds);
                statistics.put("buyerMainDetails", buyerMainDetails);
            }

            log.info("用户 {} 管理的买家账户统计完成: 总账户={}, 子账户={}",
                    userId, buyerMainCount, buyerSubCount);

        } catch (Exception e) {
            log.error("获取买家账户统计信息失败：userId={}", userId, e);
            initializeBuyerStatistics(statistics, 0L, 0L);
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getDashboardStatistics(Long userId) {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            if (userId == null) {
                return dashboard;
            }

            // 获取各种统计信息
            Map<String, Object> deptStats = getManagedDeptsStatistics(userId);
            Map<String, Object> agentStats = getManagedAgentLevelsStatistics(userId);
            Map<String, Object> buyerStats = getManagedBuyerAccountsStatistics(userId);

            // 整合到仪表板
            dashboard.put("deptStatistics", deptStats);
            dashboard.put("agentStatistics", agentStats);
            dashboard.put("buyerStatistics", buyerStats);

            
            // 添加时间戳
            dashboard.put("statisticsTime", LocalDateTime.now().toString());

            log.info("用户 {} 的仪表板统计完成", userId);

        } catch (Exception e) {
            log.error("获取仪表板统计信息失败：userId={}", userId, e);
        }

        return dashboard;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户管理的所有部门ID
     */
    private Set<Long> getManagedDeptIds(Long userId) {
        Set<Long> managedDeptIds = new HashSet<>();

        // 查询用户作为负责人的部门
        List<SysDept> managedDepts = deptMapper.selectDeptsByLeaderUserId(userId);
        for (SysDept dept : managedDepts) {
            managedDeptIds.add(dept.getDeptId());
            // 递归获取子部门ID
            List<Long> childIds = selectChildDeptIds(dept.getDeptId());
            managedDeptIds.addAll(childIds);
        }

        List<Long> accessibleDeptIds = getAccessibleDeptIds(userId);
        if (accessibleDeptIds != null && !accessibleDeptIds.isEmpty()) {
            managedDeptIds.addAll(accessibleDeptIds);
        }

        return managedDeptIds;
    }

    /**
     * 初始化部门统计信息
     */
    private void initializeDeptStatistics(Map<String, Object> statistics,
                                       Long systemDept, Long level1Agent, Long level2Agent, Long level3Agent, Long buyerMain, Long buyerSub) {
        statistics.put("systemDeptCount", systemDept);
        statistics.put("level1AgentCount", level1Agent);
        statistics.put("level2AgentCount", level2Agent);
        statistics.put("level3AgentCount", level3Agent);
        statistics.put("buyerMainAccountCount", buyerMain);
        statistics.put("buyerSubAccountCount", buyerSub);
    }

    /**
     * 构建代理层级的充值与佣金统计数据
     */
    private Map<String, Object> buildAgentRechargeStatistics(List<SysDept> managedDepts) {
        Map<String, Object> rechargeStats = new HashMap<>();
        rechargeStats.put("level1Recharge", formatAmount(BigDecimal.ZERO));
        rechargeStats.put("level2Recharge", formatAmount(BigDecimal.ZERO));
        rechargeStats.put("level3Recharge", formatAmount(BigDecimal.ZERO));
        rechargeStats.put("level1commission", formatAmount(BigDecimal.ZERO));
        rechargeStats.put("level2commission", formatAmount(BigDecimal.ZERO));
        rechargeStats.put("level3commission", formatAmount(BigDecimal.ZERO));

        if (managedDepts == null || managedDepts.isEmpty()) {
            return rechargeStats;
        }

        Map<Long, SysDept> deptMap = managedDepts.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getDeptId() != null)
                .collect(Collectors.toMap(SysDept::getDeptId, dept -> dept, (left, right) -> left));
        if (deptMap.isEmpty()) {
            return rechargeStats;
        }

        List<SysDept> buyerMainDepts = deptMap.values().stream()
                .filter(dept -> "3".equals(dept.getDeptType()))
                .collect(Collectors.toList());
        if (buyerMainDepts.isEmpty()) {
            return rechargeStats;
        }

        List<Long> buyerMainDeptIds = buyerMainDepts.stream()
                .map(SysDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (buyerMainDeptIds.isEmpty()) {
            return rechargeStats;
        }

        List<SysUser> buyerDeptUsers = userMapper.selectUsersByDeptIds(buyerMainDeptIds);
        if (buyerDeptUsers == null || buyerDeptUsers.isEmpty()) {
            return rechargeStats;
        }

        final Map<Long, List<SysUser>> usersByDept = buyerDeptUsers.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getDeptId() != null)
                .collect(Collectors.groupingBy(SysUser::getDeptId));
        if (usersByDept.isEmpty()) {
            return rechargeStats;
        }

        Set<Long> buyerUserIds = buyerDeptUsers.stream()
                .map(SysUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (buyerUserIds.isEmpty()) {
            return rechargeStats;
        }

        final Map<Long, BigDecimal> userRechargeMap = fetchRechargeByUserIds(buyerUserIds);

        Map<Long, BigDecimal> agentRechargeMap = new HashMap<>();

        for (SysDept buyerMainDept : buyerMainDepts) {
            Long buyerDeptId = buyerMainDept.getDeptId();
            if (buyerDeptId == null) {
                continue;
            }

            BigDecimal buyerRecharge = usersByDept.getOrDefault(buyerDeptId, Collections.emptyList()).stream()
                    .map(SysUser::getUserId)
                    .map(userId -> userRechargeMap.getOrDefault(userId, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (buyerRecharge.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Long currentParentId = buyerMainDept.getParentId();
            while (currentParentId != null && currentParentId > 0) {
                SysDept parentDept = deptMap.get(currentParentId);
                if (parentDept == null) {
                    parentDept = deptMapper.selectDeptById(currentParentId);
                    if (parentDept == null) {
                        break;
                    }
                    deptMap.put(parentDept.getDeptId(), parentDept);
                }

                if ("2".equals(parentDept.getDeptType())) {
                    agentRechargeMap.merge(parentDept.getDeptId(), buyerRecharge, BigDecimal::add);
                }

                currentParentId = parentDept.getParentId();
            }
        }

        BigDecimal level1Recharge = BigDecimal.ZERO;
        BigDecimal level2Recharge = BigDecimal.ZERO;
        BigDecimal level3Recharge = BigDecimal.ZERO;

        for (SysDept dept : deptMap.values()) {
            if (!"2".equals(dept.getDeptType())) {
                continue;
            }
            Integer level = dept.getLevel();
            if (level == null) {
                continue;
            }
            BigDecimal agentTotal = agentRechargeMap.getOrDefault(dept.getDeptId(), BigDecimal.ZERO);
            switch (level) {
                case 1:
                    level1Recharge = level1Recharge.add(agentTotal);
                    break;
                case 2:
                    level2Recharge = level2Recharge.add(agentTotal);
                    break;
                case 3:
                    level3Recharge = level3Recharge.add(agentTotal);
                    break;
                default:
                    break;
            }
        }

        rechargeStats.put("level1Recharge", formatAmount(level1Recharge));
        rechargeStats.put("level2Recharge", formatAmount(level2Recharge));
        rechargeStats.put("level3Recharge", formatAmount(level3Recharge));
        rechargeStats.put("level1commission", formatAmount(level1Recharge.multiply(LEVEL1_COMMISSION_RATE)));
        rechargeStats.put("level2commission", formatAmount(level2Recharge.multiply(LEVEL2_COMMISSION_RATE)));
        rechargeStats.put("level3commission", formatAmount(level3Recharge.multiply(LEVEL3_COMMISSION_RATE)));

        return rechargeStats;
    }

    /**
     * 构建商户业绩统计数据
     */
    private Map<String, Object> buildMerchantPerformance(List<SysDept> managedDepts) {
        Map<String, Object> merchantStats = new HashMap<>();
        merchantStats.put("totalMerchants", 0);
        merchantStats.put("totalRecharge", formatAmount(BigDecimal.ZERO));
        merchantStats.put("buyerSubAccountCount", 0);
        merchantStats.put("instanceStatistics", Collections.emptyMap());
        merchantStats.put("aiCharacterStatistics", Collections.emptyMap());

        if (managedDepts == null || managedDepts.isEmpty()) {
            return merchantStats;
        }

        Map<Long, SysDept> deptMap = managedDepts.stream()
                .filter(Objects::nonNull)
                .filter(dept -> dept.getDeptId() != null)
                .collect(Collectors.toMap(SysDept::getDeptId, dept -> dept, (left, right) -> left));
        if (deptMap.isEmpty()) {
            return merchantStats;
        }

        List<SysDept> buyerMainDepts = deptMap.values().stream()
                .filter(dept -> "3".equals(dept.getDeptType()))
                .collect(Collectors.toList());
        if (buyerMainDepts.isEmpty()) {
            return merchantStats;
        }

        Set<Long> buyerMainDeptIds = buyerMainDepts.stream()
                .map(SysDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (buyerMainDeptIds.isEmpty()) {
            return merchantStats;
        }

        List<SysUser> buyerMainUsers = userMapper.selectUsersByDeptIds(new ArrayList<>(buyerMainDeptIds));
        Set<Long> buyerMainUserIds = buyerMainUsers == null ? Collections.emptySet() : buyerMainUsers.stream()
                .map(SysUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        merchantStats.put("totalMerchants", buyerMainUserIds.size());

        BigDecimal totalRecharge = BigDecimal.ZERO;
        if (!buyerMainUserIds.isEmpty()) {
            Map<Long, BigDecimal> userRechargeMap = fetchRechargeByUserIds(buyerMainUserIds);
            totalRecharge = buyerMainUserIds.stream()
                    .map(userId -> userRechargeMap.getOrDefault(userId, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        merchantStats.put("totalRecharge", formatAmount(totalRecharge));

        Set<Long> buyerSubDeptIds = deptMap.values().stream()
                .filter(dept -> "4".equals(dept.getDeptType()))
                .filter(dept -> isDescendantOfBuyerMain(dept, buyerMainDeptIds))
                .map(SysDept::getDeptId)
                .collect(Collectors.toSet());

        List<SysUser> buyerSubUsers = buyerSubDeptIds.isEmpty()
                ? Collections.emptyList()
                : userMapper.selectUsersByDeptIds(new ArrayList<>(buyerSubDeptIds));

        Set<Long> buyerSubUserIds = buyerSubUsers.stream()
                .map(SysUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        merchantStats.put("buyerSubAccountCount", buyerSubUserIds.size());

        Set<Long> instanceUserIds = new HashSet<>(buyerSubUserIds);
        Map<String, Object> instanceStatistics = buildInstanceStatistics(instanceUserIds);
        merchantStats.put("instanceStatistics", instanceStatistics);

        Set<Long> aiUserIds = new HashSet<>(buyerSubUserIds);
        aiUserIds.addAll(buyerMainUserIds);
        Map<String, Object> aiStatistics = buildAiCharacterStatistics(aiUserIds);
        merchantStats.put("aiCharacterStatistics", aiStatistics);

        return merchantStats;
    }

    private Map<String, Object> buildAgentCommissionStatistics(Map<Long, SysDept> agentDeptMap,
                                                              List<SysUser> agentUsers) {
        Map<String, Object> commissionStats = new HashMap<>();
        BigDecimal level1 = BigDecimal.ZERO;
        BigDecimal level2 = BigDecimal.ZERO;
        BigDecimal level3 = BigDecimal.ZERO;

        if (agentDeptMap == null || agentDeptMap.isEmpty() || agentUsers == null || agentUsers.isEmpty()) {
            commissionStats.put("level1Commission", formatAmount(level1));
            commissionStats.put("level2Commission", formatAmount(level2));
            commissionStats.put("level3Commission", formatAmount(level3));
            commissionStats.put("totalCommission", formatAmount(BigDecimal.ZERO));
            return commissionStats;
        }

        Map<Long, Integer> userLevelMap = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        for (SysUser user : agentUsers) {
            if (user == null || user.getUserId() == null || user.getDeptId() == null) {
                continue;
            }
            SysDept dept = agentDeptMap.get(user.getDeptId());
            if (dept == null) {
                continue;
            }
            int level = dept.getLevel();
            if (level <= 0) {
                continue;
            }
            userLevelMap.put(user.getUserId(), level);
            userIds.add(user.getUserId());
        }

        if (userIds.isEmpty()) {
            commissionStats.put("level1Commission", formatAmount(level1));
            commissionStats.put("level2Commission", formatAmount(level2));
            commissionStats.put("level3Commission", formatAmount(level3));
            commissionStats.put("totalCommission", formatAmount(BigDecimal.ZERO));
            return commissionStats;
        }

        List<Map<String, Object>> rows = agentCommissionAccountMapper.selectCommissionByUserIds(userIds);
        Map<Long, BigDecimal> commissionMap = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row == null || row.isEmpty()) {
                    continue;
                }
                Long userId = parseLong(row.get("userId"));
                if (userId == null) {
                    continue;
                }
                BigDecimal commission = toBigDecimal(row.get("totalCommission"));
                commissionMap.put(userId, commission);
            }
        }

        for (Map.Entry<Long, Integer> entry : userLevelMap.entrySet()) {
            Long userId = entry.getKey();
            Integer levelObj = entry.getValue();
            if (levelObj == null || levelObj <= 0) {
                continue;
            }
            BigDecimal commission = commissionMap.getOrDefault(userId, BigDecimal.ZERO);
            if (commission.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int level = levelObj;
            switch (level) {
                case 1:
                    level1 = level1.add(commission);
                    break;
                case 2:
                    level2 = level2.add(commission);
                    break;
                case 3:
                    level3 = level3.add(commission);
                    break;
                default:
                    break;
            }
        }

        BigDecimal total = level1.add(level2).add(level3);
        commissionStats.put("level1Commission", formatAmount(level1));
        commissionStats.put("level2Commission", formatAmount(level2));
        commissionStats.put("level3Commission", formatAmount(level3));
        commissionStats.put("totalCommission", formatAmount(total));
        return commissionStats;
    }

    private Map<Long, BigDecimal> fetchRechargeByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rechargeRows = userMapper.sumTotalRechargeByUserIds(userIds);
        if (rechargeRows == null || rechargeRows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rechargeRows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            BigDecimal recharge = toBigDecimal(row.get("totalRecharge"));
            result.put(userId, recharge);
        }
        return result;
    }

    private Map<String, Long> initializePlatformMap(List<Map<String, Object>> platformRows, Set<String> filterTypes) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (platformRows != null) {
            for (Map<String, Object> row : platformRows) {
                if (row == null) {
                    continue;
                }
                String normalizedType = normalizePlatformType(Objects.toString(row.get("platformType"), "unknown"));
                if (filterTypes != null && !filterTypes.isEmpty()
                        && !filterTypes.contains(normalizedType)
                        && !"unknown".equals(normalizedType)) {
                    continue;
                }
                String platformName = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                result.putIfAbsent(platformName, 0L);
            }
        }
        if (result.isEmpty()) {
            result.put("UNKNOWN_PLATFORM", 0L);
        }
        return result;
    }

    private String normalizePlatformName(String rawName) {
        if (rawName == null) {
            return "UNKNOWN_PLATFORM";
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            return "UNKNOWN_PLATFORM";
        }
        switch (name.toLowerCase(Locale.ROOT)) {
            case "sms":
                return "SMS";
            case "whatsapp":
                return "WhatsApp";
            case "facebook":
                return "Facebook";
            case "telegram":
                return "Telegram";
            case "instagram":
                return "Instagram";
            case "tiktok":
                return "TikTok";
            case "x":
            case "twitter":
                return "X";
            case "googlevoice":
            case "google_voice":
            case "google voice":
                return "GoogleVoice";
            default:
                return name;
        }
    }

    private String normalizePlatformType(String rawType) {
        if (rawType == null) {
            return "unknown";
        }
        String type = rawType.trim().toLowerCase(Locale.ROOT);
        if (type.isEmpty()) {
            return "unknown";
        }
        type = type.replace('_', '-');
        return type;
    }

    private Map<String, Object> buildInstanceStatistics(Set<Long> userIds) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("marketingInstanceCount", 0L);
        statistics.put("prospectingInstanceCount", 0L);
        statistics.put("marketingPlatformBreakdown", Collections.emptyMap());
        statistics.put("prospectingPlatformBreakdown", Collections.emptyMap());

        List<Map<String, Object>> platformRows = userMapper.selectAllPlatforms();
        Map<String, Long> marketingBreakdown = initializePlatformMap(platformRows, MARKETING_PLATFORM_TYPES);
        Map<String, Long> prospectingBreakdown = initializePlatformMap(platformRows, PROSPECTING_PLATFORM_TYPES);

        if (userIds == null || userIds.isEmpty()) {
            statistics.put("marketingPlatformBreakdown", marketingBreakdown);
            statistics.put("prospectingPlatformBreakdown", prospectingBreakdown);
            return statistics;
        }

        List<Map<String, Object>> typeRows = userMapper.countInstancesByType(userIds);
        long marketingCount = 0L;
        long prospectingCount = 0L;

        if (typeRows != null) {
            for (Map<String, Object> row : typeRows) {
                String instanceType = Objects.toString(row.get("instanceType"), "");
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                switch (instanceType) {
                    case "0":
                        marketingCount = count;
                        break;
                    case "1":
                        prospectingCount = count;
                        break;
                    default:
                        break;
                }
            }
        }

        statistics.put("marketingInstanceCount", marketingCount);
        statistics.put("prospectingInstanceCount", prospectingCount);

        List<Map<String, Object>> marketingRows = userMapper.countInstancesByPlatform(userIds, "0");
        if (marketingRows != null) {
            for (Map<String, Object> row : marketingRows) {
                String platform = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                long current = marketingBreakdown.getOrDefault(platform, 0L);
                marketingBreakdown.put(platform, current + count);
            }
        }

        List<Map<String, Object>> prospectingRows = userMapper.countInstancesByPlatform(userIds, "1");
        if (prospectingRows != null) {
            for (Map<String, Object> row : prospectingRows) {
                String platform = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                long current = prospectingBreakdown.getOrDefault(platform, 0L);
                prospectingBreakdown.put(platform, current + count);
            }
        }

        statistics.put("marketingPlatformBreakdown", marketingBreakdown);
        statistics.put("prospectingPlatformBreakdown", prospectingBreakdown);

        return statistics;
    }

    private Map<String, Object> buildAiCharacterStatistics(Set<Long> userIds) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalCharacters", 0L);
        statistics.put("socialAiCount", 0L);
        statistics.put("customerServiceAiCount", 0L);

        if (userIds == null || userIds.isEmpty()) {
            return statistics;
        }

        List<Map<String, Object>> rows = userMapper.countAiCharactersByType(userIds);
        long total = 0L;
        long social = 0L;
        long customerService = 0L;

        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String type = Objects.toString(row.get("type"), "");
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                total += count;
                if ("emotion".equalsIgnoreCase(type) || "social".equalsIgnoreCase(type)) {
                    social += count;
                } else if ("business".equalsIgnoreCase(type)
                        || "service".equalsIgnoreCase(type)
                        || "customer_service".equalsIgnoreCase(type)
                        || "customer".equalsIgnoreCase(type)) {
                    customerService += count;
                }
            }
        }

        statistics.put("totalCharacters", total);
        statistics.put("socialAiCount", social);
        statistics.put("customerServiceAiCount", customerService);

        return statistics;
    }

    private boolean isDescendantOfBuyerMain(SysDept dept, Set<Long> buyerMainDeptIds) {
        if (dept == null || buyerMainDeptIds == null || buyerMainDeptIds.isEmpty()) {
            return false;
        }
        if (dept.getParentId() != null && buyerMainDeptIds.contains(dept.getParentId())) {
            return true;
        }
        String ancestors = dept.getAncestors();
        if (ancestors == null || ancestors.trim().isEmpty()) {
            return false;
        }
        String[] tokens = ancestors.split(",");
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            try {
                Long ancestorId = Long.valueOf(token.trim());
                if (buyerMainDeptIds.contains(ancestorId)) {
                    return true;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return false;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number || value instanceof String) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException ex) {
                log.warn("无法解析充值金额: {}", value, ex);
            }
        }
        return BigDecimal.ZERO;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ex) {
                log.warn("无法解析部门ID: {}", value, ex);
            }
        }
        return null;
    }

    private BigDecimal formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 初始化代理统计信息
     */
    private void initializeAgentStatistics(Map<String, Object> statistics,
                                         Long level1, Long level2, Long level3) {
        statistics.put("level1AgentCount", level1);
        statistics.put("level2AgentCount", level2);
        statistics.put("level3AgentCount", level3);
    }

    /**
     * 初始化买家账户统计信息
     */
    private void initializeBuyerStatistics(Map<String, Object> statistics,
                                          Long buyerMain, Long buyerSub) {
        statistics.put("buyerMainAccountCount", buyerMain);
        statistics.put("buyerSubAccountCount", buyerSub);
    }

}
