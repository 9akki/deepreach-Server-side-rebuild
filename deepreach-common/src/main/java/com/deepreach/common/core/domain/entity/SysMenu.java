package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统菜单权限实体类 sys_menu
 *
 * 系统菜单权限的核心实体，包含：
 * 1. 菜单基本信息（名称、路径、组件等）
 * 2. 菜单层级结构（父菜单、排序等）
 * 3. 菜单类型和显示配置
 * 4. 路由和权限标识
 * 5. 子菜单关联数据
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     *
     * 菜单的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long menuId;

    /**
     * 菜单名称
     *
     * 菜单的显示名称，用于系统界面显示
     * 可以包含中文，如"系统管理"、"用户管理"
     * 长度限制：最多50个字符
     */
    private String menuName;

    /**
     * 父菜单ID
     *
     * 父菜单的ID，用于构建菜单树形结构
     * 0表示顶级菜单（一级菜单）
     * 通过parent_id可以构建多级菜单结构
     */
    private Long parentId;

    /**
     * 显示顺序
     *
     * 菜单在同级菜单中的排序权重
     * 数值越小排序越靠前
     * 用于菜单管理界面的排序显示
     */
    private Integer orderNum;

    /**
     * 路由地址
     *
     * 前端路由的路径，用于页面跳转
     * 如"/system/user"、"/system/role"
     * 对于外链，这里填写完整的URL
     */
    private String path;

    /**
     * 组件路径
     *
     * 前端组件的相对路径，用于动态加载组件
     * 如"system/user/index"、"system/role/index"
     * 对于目录类型的菜单，这里可以为空
     */
    private String component;

    /**
     * 路由参数
     *
     * 路由的查询参数，用于页面间数据传递
     * 如"id=123&type=edit"
     * JSON格式字符串
     */
    private String query;

    /**
     * 是否为外链
     *
     * 标识菜单是否为外部链接：
     * 0 - 否：内部页面路由
     * 1 - 是：外部链接，会在新窗口打开
     *
     * 外链菜单通常不需要组件路径
     */
    private Integer isFrame;

    /**
     * 是否缓存
     *
     * 标识页面是否需要缓存：
     * 0 - 缓存：页面会被浏览器缓存，提高访问速度
     * 1 - 不缓存：页面不会被缓存，每次都重新加载
     *
     * 缓存设置影响前端页面的性能
     */
    private Integer isCache;

    /**
     * 菜单类型
     *
     * 菜单的类型标识：
     * M - 目录：包含子菜单的目录，不可点击
     * C - 菜单：具体的页面菜单，可以点击跳转
     * F - 按钮：页面中的功能按钮，用于权限控制
     *
     * 不同类型的菜单在前端有不同的渲染方式
     */
    private String menuType;

    /**
     * 菜单状态
     *
     * 菜单的显示状态：
     * 0 - 显示：菜单在界面中正常显示
     * 1 - 隐藏：菜单在界面中隐藏，但权限仍然有效
     *
     * 隐藏的菜单仍然可以用于权限控制
     */
    private Integer visible;

    /**
     * 菜单状态
     *
     * 菜单的启用状态：
     * 0 - 正常：菜单可以使用和显示
     * 1 - 停用：菜单停用，不显示且权限无效
     *
     * 停用的菜单完全不可用
     */
    private Integer status;

    /**
     * 权限标识
     *
     * 菜单的权限标识，用于后端权限验证
     * 如"system:user:list"、"system:user:add"
     * 格式通常为"模块:功能:操作"
     *
     * 这个标识对应Spring Security中的权限配置
     */
    private String perms;

    /**
     * 菜单图标
     *
     * 菜单显示的图标，用于界面美观
     * 支持图标库的图标名称，如"user"、"setting"
     * 对于没有图标的菜单，使用"#"
     */
    private String icon;

    /**
     * 子菜单列表
     *
     * 该菜单下的所有子菜单
     * 用于构建菜单树形结构
     * 通过parentId关联查询获得
     */
    private List<SysMenu> children = new ArrayList<>();

    // ==================== 业务判断方法 ====================

    /**
     * 判断是否为顶级菜单
     *
     * 检查菜单是否没有父菜单
     * 顶级菜单的parentId为0或null
     *
     * @return true如果是顶级菜单，false否则
     */
    public boolean isTopLevel() {
        return this.parentId == null || this.parentId == 0L;
    }

    /**
     * 判断是否为目录类型
     *
     * 目录类型的菜单主要用于组织子菜单
     * 通常不能直接点击跳转
     *
     * @return true如果是目录类型，false否则
     */
    public boolean isDirectory() {
        return "M".equals(this.menuType);
    }

    /**
     * 判断是否为菜单类型
     *
     * 菜单类型对应具体的页面
     * 可以点击跳转到对应页面
     *
     * @return true如果是菜单类型，false否则
     */
    public boolean isMenu() {
        return "C".equals(this.menuType);
    }

    /**
     * 判断是否为按钮类型
     *
     * 按钮类型对应页面中的功能按钮
     * 主要用于权限控制，不会在菜单中显示
     *
     * @return true如果是按钮类型，false否则
     */
    public boolean isButton() {
        return "F".equals(this.menuType);
    }

    /**
     * 判断菜单是否可见
     *
     * 检查菜单是否在界面中显示
     * 隐藏的菜单仍然可以用于权限控制
     *
     * @return true如果菜单可见，false如果菜单隐藏
     */
    public boolean isVisible() {
        return Integer.valueOf(0).equals(this.visible);
    }

    /**
     * 判断菜单是否隐藏
     *
     * 检查菜单是否在界面中隐藏
     *
     * @return true如果菜单隐藏，false如果菜单可见
     */
    public boolean isHidden() {
        return Integer.valueOf(1).equals(this.visible);
    }

    /**
     * 判断菜单是否正常状态
     *
     * 检查菜单是否可以正常使用
     *
     * @return true如果菜单正常，false如果菜单停用
     */
    public boolean isNormal() {
        return Integer.valueOf(0).equals(this.status);
    }

    /**
     * 判断菜单是否被停用
     *
     * 检查菜单是否被停用
     * 停用的菜单完全不可用
     *
     * @return true如果菜单停用，false如果菜单正常
     */
    public boolean isDisabled() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断菜单是否为外链
     *
     * 检查菜单是否指向外部链接
     *
     * @return true如果是外链，false否则
     */
    public boolean isExternalLink() {
        return Integer.valueOf(1).equals(this.isFrame);
    }

    /**
     * 判断菜单是否需要缓存
     *
     * 检查页面是否需要被浏览器缓存
     *
     * @return true如果需要缓存，false如果不需要缓存
     */
    public boolean needCache() {
        return Integer.valueOf(0).equals(this.isCache);
    }

    /**
     * 判断菜单是否可以使用
     *
     * 综合判断菜单状态，确定是否可以使用
     * 需要菜单正常且可见
     *
     * @return true如果可以使用，false否则
     */
    public boolean isAvailable() {
        return isNormal() && isVisible();
    }

    /**
     * 判断菜单是否有子菜单
     *
     * @return true如果有子菜单，false如果没有子菜单
     */
    public boolean hasChildren() {
        return this.children != null && !this.children.isEmpty();
    }

    /**
     * 获取菜单类型显示文本
     *
     * @return 菜单类型显示文本
     */
    public String getMenuTypeDisplay() {
        switch (this.menuType) {
            case "M":
                return "目录";
            case "C":
                return "菜单";
            case "F":
                return "按钮";
            default:
                return "未知";
        }
    }

    /**
     * 获取菜单状态显示文本
     *
     * @return 状态显示文本：正常/停用
     */
    public String getStatusDisplay() {
        if (Integer.valueOf(0).equals(this.status)) {
            return "正常";
        } else if (Integer.valueOf(1).equals(this.status)) {
            return "停用";
        } else {
            return "未知";
        }
    }

    /**
     * 获取可见性显示文本
     *
     * @return 可见性显示文本：显示/隐藏
     */
    public String getVisibleDisplay() {
        if (Integer.valueOf(0).equals(this.visible)) {
            return "显示";
        } else if (Integer.valueOf(1).equals(this.visible)) {
            return "隐藏";
        } else {
            return "未知";
        }
    }

    /**
     * 获取外链状态显示文本
     *
     * @return 外链状态显示文本：是/否
     */
    public String getFrameDisplay() {
        if (Integer.valueOf(1).equals(this.isFrame)) {
            return "是";
        } else {
            return "否";
        }
    }

    /**
     * 获取缓存状态显示文本
     *
     * @return 缓存状态显示文本：缓存/不缓存
     */
    public String getCacheDisplay() {
        if (Integer.valueOf(0).equals(this.isCache)) {
            return "缓存";
        } else {
            return "不缓存";
        }
    }

    /**
     * 检查是否有指定权限
     *
     * @param permission 权限标识
     * @return true如果有该权限，false否则
     */
    public boolean hasPermission(String permission) {
        return this.perms != null && this.perms.equals(permission);
    }

    /**
     * 添加子菜单
     *
     * @param child 子菜单对象
     */
    public void addChild(SysMenu child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    /**
     * 获取所有子孙菜单ID（递归）
     *
     * @return 子孙菜单ID列表
     */
    public List<Long> getAllChildIds() {
        List<Long> ids = new ArrayList<>();
        if (children != null) {
            for (SysMenu child : children) {
                ids.add(child.getMenuId());
                ids.addAll(child.getAllChildIds());
            }
        }
        return ids;
    }

    /**
     * 获取菜单层级深度
     *
     * @return 层级深度（从1开始）
     */
    public int getLevel() {
        // 这里需要根据实际情况计算，可以通过递归查询父菜单
        // 临时实现：如果父ID为0，则为第1级
        return isTopLevel() ? 1 : 2;
    }

    /**
     * 获取完整的菜单路径
     *
     * @return 完整路径，如"系统管理/用户管理"
     */
    public String getFullPath() {
        // 这里需要通过递归查询父菜单来构建完整路径
        // 临时实现：直接返回菜单名称
        return this.menuName;
    }

    // ==================== 数据转换方法 ====================

    /**
     * 创建目录类型的菜单
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单ID
     * @param orderNum 排序
     * @param icon 图标
     * @return 目录菜单对象
     */
    public static SysMenu createDirectory(String menuName, Long parentId, Integer orderNum, String icon) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setOrderNum(orderNum);
        menu.setMenuType("M");
        menu.setVisible(0);
        menu.setStatus(0);
        menu.setIcon(icon);
        return menu;
    }

    /**
     * 创建菜单类型的菜单
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单ID
     * @param orderNum 排序
     * @param path 路由路径
     * @param component 组件路径
     * @param perms 权限标识
     * @param icon 图标
     * @return 菜单对象
     */
    public static SysMenu createMenu(String menuName, Long parentId, Integer orderNum,
                                   String path, String component, String perms, String icon) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setOrderNum(orderNum);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setMenuType("C");
        menu.setVisible(0);
        menu.setStatus(0);
        menu.setPerms(perms);
        menu.setIcon(icon);
        return menu;
    }

    /**
     * 创建按钮类型的菜单
     *
     * @param menuName 按钮名称
     * @param parentId 父菜单ID
     * @param orderNum 排序
     * @param perms 权限标识
     * @return 按钮菜单对象
     */
    public static SysMenu createButton(String menuName, Long parentId, Integer orderNum, String perms) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setOrderNum(orderNum);
        menu.setMenuType("F");
        menu.setVisible(0);
        menu.setStatus(0);
        menu.setPerms(perms);
        menu.setIcon("#");
        return menu;
    }
}