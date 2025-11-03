package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门实体类 sys_dept
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 负责人用户ID
     */
    private Long leaderUserId;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;

    // ==================== 基于部门类型的字段 ====================

    /**
     * 部门类型
     *
     * 部门类型决定用户类型和权限范围：
     * 1 - 系统部门：系统管理员、技术管理员、运营管理员等后台用户
     * 2 - 代理部门：代理用户，只有代理部门有层级（1-3级）
     * 3 - 买家总账户部门：买家总账户用户，可以创建买家子账户
     * 4 - 买家子账户部门：买家子账户用户，只能操作自己的数据
     *
     * 通过部门类型自动确定用户角色，简化角色管理
     */
    private String deptType;

    /**
     * 部门层级
     *
     * 部门在组织架构中的层级：
     * - 系统部门：固定为1级
     * - 代理部门：1-3级（一级代理、二级代理、三级代理）
     * - 买家总账户：固定为1级（相对于子账户）
     * - 买家子账户：固定为2级（相对于总账户）
     *
     * 用于权限控制和层级管理
     */
    private Integer level;

    /**
     * 子部门列表
     */
    private List<SysDept> children = new ArrayList<>();

    /**
     * 获取部门树节点类型
     *
     * @return 节点类型
     */
    public String getNodeType() {
        if (parentId == null || parentId == 0) {
            return "root";
        }
        if (children != null && !children.isEmpty()) {
            return "branch";
        }
        return "leaf";
    }

    /**
     * 是否为根节点
     *
     * @return 是否为根节点
     */
    public boolean isRoot() {
        return "root".equals(getNodeType());
    }

    /**
     * 是否为分支节点
     *
     * @return 是否为分支节点
     */
    public boolean isBranch() {
        return "branch".equals(getNodeType());
    }

    /**
     * 是否为叶子节点
     *
     * @return 是否为叶子节点
     */
    public boolean isLeaf() {
        return "leaf".equals(getNodeType());
    }

    /**
     * 是否启用状态
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return "0".equals(status);
    }

    /**
     * 是否已删除
     *
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return "2".equals(delFlag);
    }

    /**
     * 添加子部门
     *
     * @param child 子部门
     */
    public void addChild(SysDept child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    /**
     * 获取所有子部门ID（递归）
     *
     * @return 子部门ID列表
     */
    public List<Long> getAllChildIds() {
        List<Long> ids = new ArrayList<>();
        if (children != null) {
            for (SysDept child : children) {
                ids.add(child.getDeptId());
                ids.addAll(child.getAllChildIds());
            }
        }
        return ids;
    }

    /**
     * 获取部门层级深度
     *
     * @return 层级深度
     */
    public int getLevel() {
        return level != null ? level : 0;
    }

    /**
     * 检查是否为指定部门的祖先
     *
     * @param childDeptId 子部门ID
     * @return 是否为祖先部门
     */
    public boolean isAncestorOf(Long childDeptId) {
        if (ancestors == null || ancestors.trim().isEmpty()) {
            return false;
        }
        return ancestors.contains("," + childDeptId + ",") ||
               ancestors.startsWith(childDeptId + ",") ||
               ancestors.endsWith("," + childDeptId) ||
               ancestors.equals(childDeptId.toString());
    }

    /**
     * 获取部门完整路径
     *
     * @return 完整路径
     */
    public String getFullPath() {
        if (ancestors == null || ancestors.trim().isEmpty()) {
            return deptName;
        }
        return ancestors.replace(",", " / ") + " / " + deptName;
    }

    /**
     * 创建简单的部门对象（用于树形结构显示）
     *
     * @param deptId 部门ID
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @return 部门对象
     */
    public static SysDept createSimple(Long deptId, String deptName, Long parentId) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setStatus("0");
        dept.setDelFlag("0");
        return dept;
    }

    /**
     * 转换为树形节点DTO
     *
     * @return 树形节点DTO
     */
    public DeptTreeNode toTreeNode() {
        DeptTreeNode node = new DeptTreeNode();
        node.setId(deptId);
        node.setLabel(deptName);
        node.setParentId(parentId);
        node.setChildren(children.stream()
            .map(SysDept::toTreeNode)
            .collect(java.util.stream.Collectors.toList()));
        return node;
    }

    // ==================== 基于部门类型的业务判断方法 ====================

    /**
     * 判断是否为系统部门
     *
     * @return true如果是系统部门，false否则
     */
    public boolean isSystemDept() {
        return "1".equals(this.deptType);
    }

    /**
     * 判断是否为代理部门
     *
     * @return true如果是代理部门，false否则
     */
    public boolean isAgentDept() {
        return "2".equals(this.deptType);
    }

    /**
     * 判断是否为买家总账户部门
     *
     * @return true如果是买家总账户部门，false否则
     */
    public boolean isBuyerMainDept() {
        return "3".equals(this.deptType);
    }

    /**
     * 判断是否为买家子账户部门
     *
     * @return true如果是买家子账户部门，false否则
     */
    public boolean isBuyerSubDept() {
        return "4".equals(this.deptType);
    }

    /**
     * 判断是否为买家部门（总账户或子账户）
     *
     * @return true如果是买家部门，false否则
     */
    public boolean isBuyerDept() {
        return isBuyerMainDept() || isBuyerSubDept();
    }

    /**
     * 判断是否为后台用户部门
     *
     * 系统部门和代理部门都属于后台用户
     *
     * @return true如果是后台用户部门，false否则
     */
    public boolean isBackendDept() {
        return isSystemDept() || isAgentDept();
    }

    /**
     * 判断是否为前端用户部门
     *
     * 只有买家子账户部门属于前端用户
     *
     * @return true如果是前端用户部门，false否则
     */
    public boolean isFrontendDept() {
        return isBuyerSubDept();
    }

    /**
     * 获取部门类型显示文本
     *
     * @return 部门类型显示文本
     */
    public String getDeptTypeDisplay() {
        switch (this.deptType) {
            case "1":
                return "系统部门";
            case "2":
                return "代理部门";
            case "3":
                return "买家总账户";
            case "4":
                return "买家子账户";
            default:
                return "未知类型";
        }
    }

    /**
     * 获取层级显示文本
     *
     * @return 层级显示文本
     */
    public String getLevelDisplay() {
        if (this.level == null) {
            return "未设置";
        }

        // 系统部门固定显示为1级
        if (isSystemDept()) {
            return "系统级";
        }

        // 代理部门显示具体层级
        if (isAgentDept()) {
            switch (this.level) {
                case 1:
                    return "一级代理";
                case 2:
                    return "二级代理";
                case 3:
                    return "三级代理";
                default:
                    return "未知代理层级";
            }
        }

        // 买家账户显示相对层级
        if (isBuyerMainDept()) {
            return "总账户";
        }

        if (isBuyerSubDept()) {
            return "子账户";
        }

        return "未知层级";
    }

    /**
     * 检查是否可以创建下级代理
     *
     * 只有代理部门有层级控制，1级和2级代理可以创建下级代理
     *
     * @return true如果可以创建下级代理，false否则
     */
    public boolean canCreateChildAgent() {
        // 只有代理部门可以创建下级代理
        if (!isAgentDept()) {
            return false;
        }

        // 1级和2级代理可以创建下级代理
        Integer currentLevel = this.level;
        return currentLevel != null && currentLevel < 3;
    }

    /**
     * 检查是否可以创建买家总账户
     *
     * 系统部门和所有代理部门都可以创建买家总账户
     * 不论代理层级，都可以创建买家总账户部门和其中的用户
     *
     * @return true如果可以创建买家总账户，false否则
     */
    public boolean canCreateBuyerAccount() {
        // 系统部门可以创建买家总账户
        if (isSystemDept()) {
            return true;
        }

        // 所有代理部门都可以创建买家总账户（不论层级）
        if (isAgentDept()) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否可以创建买家子账户
     *
     * 只有买家总账户部门可以创建买家子账户
     *
     * @return true如果可以创建买家子账户，false否则
     */
    public boolean canCreateSubAccount() {
        return isBuyerMainDept();
    }

    /**
     * 获取可创建的下一级代理层级
     *
     * @return 可创建的下一级代理层级，如果不能创建则返回null
     */
    public Integer getNextAgentLevel() {
        if (!isAgentDept()) {
            return null;
        }

        Integer currentLevel = this.level;
        if (currentLevel == null || currentLevel >= 3) {
            return null;
        }

        return currentLevel + 1;
    }

    /**
     * 检查是否可以创建下级部门
     *
     * 根据部门类型和层级判断是否可以创建下级部门
     *
     * @return true如果可以创建下级部门，false否则
     */
    public boolean canCreateChildDept() {
        if ("4".equals(this.deptType)) {
            // 买家子账户不能创建下级部门
            return false;
        }

        if ("2".equals(this.deptType) && this.level != null && this.level >= 3) {
            // 三级代理不能创建下级代理部门
            return false;
        }

        // 其他情况都可以创建下级部门
        return true;
    }

    /**
     * 检查是否为叶子部门
     *
     * @return true如果是叶子部门，false否则
     */
    public boolean isLeafDept() {
        // 这里简化处理，实际应该检查是否有子部门
        // 为了避免循环依赖，这里返回一个默认值
        return false;
    }

    /**
     * 获取用户类型标识
     *
     * 根据部门类型返回对应的用户类型标识
     *
     * @return 用户类型标识
     */
    public String getUserType() {
        switch (this.deptType) {
            case "1":
                return "BACKEND";
            case "2":
                return "BACKEND";
            case "3":
                return "BACKEND";
            case "4":
                return "FRONTEND";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 获取默认角色标识
     *
     * 根据部门类型返回对应的默认角色标识
     *
     * @return 默认角色标识
     */
    public String getDefaultRoleKey() {
        switch (this.deptType) {
            case "1":
                return "system_admin";
            case "2":
                return "agent";
            case "3":
                return "buyer_main";
            case "4":
                return "buyer_sub";
            default:
                return "user";
        }
    }

    /**
     * 部门树形节点DTO
     */
    @Data
    public static class DeptTreeNode {
        /**
         * 节点ID
         */
        private Long id;

        /**
         * 显示名称
         */
        private String label;

        /**
         * 父节点ID
         */
        private Long parentId;

        /**
         * 子节点列表
         */
        private List<DeptTreeNode> children;

        /**
         * 节点类型
         */
        private String type;

        /**
         * 是否禁用
         */
        private Boolean disabled;

        /**
         * 扩展属性
         */
        private Object data;
    }

    
    }
