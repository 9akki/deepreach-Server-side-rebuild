package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
