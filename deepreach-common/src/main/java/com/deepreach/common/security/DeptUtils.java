package com.deepreach.common.security;

import com.deepreach.common.core.domain.entity.SysDept;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门权限辅助工具。
 *
 * 由于历史代码依赖 {@code DeptUtils}，但类缺失会导致运行时异常，
 * 这里提供一个精简版实现，满足现有调用场景：
 * <ul>
 *     <li>数据权限范围常量定义</li>
 *     <li>数据权限判断逻辑</li>
 *     <li>部门显示名称拼装</li>
 *     <li>SQL 片段构造</li>
 * </ul>
 */
public final class DeptUtils {

    private DeptUtils() {
        // 工具类不允许实例化
    }

    /**
     * 数据权限范围常量（与 DataScopeCalculator 保持一致）。
     */
    public static final class DataScope {
        public static final String ALL = "1";
        public static final String CUSTOM = "2";
        public static final String DEPT = "3";
        public static final String DEPT_AND_CHILD = "4";
        public static final String SELF = "5";

        private DataScope() {
        }
    }

    /**
     * 判断数据权限范围值是否有效。
     */
    public static boolean isValidDataScope(String dataScope) {
        return DataScope.ALL.equals(dataScope)
            || DataScope.CUSTOM.equals(dataScope)
            || DataScope.DEPT.equals(dataScope)
            || DataScope.DEPT_AND_CHILD.equals(dataScope)
            || DataScope.SELF.equals(dataScope);
    }

    /**
     * 根据数据权限范围判断是否有权访问目标部门。
     *
     * @param dataScope      数据权限范围
     * @param userDeptId     当前用户部门ID
     * @param targetDeptId   目标部门ID
     * @param childDeptIds   当前用户可访问的子部门ID集合
     * @return 是否有权限访问
     */
    public static boolean calculateDataScopePermission(String dataScope,
                                                       Long userDeptId,
                                                       Long targetDeptId,
                                                       List<Long> childDeptIds) {
        if (!isValidDataScope(dataScope) || targetDeptId == null) {
            return false;
        }

        if (DataScope.ALL.equals(dataScope)) {
            return true;
        }

        if (DataScope.DEPT.equals(dataScope) || DataScope.SELF.equals(dataScope)) {
            return Objects.equals(userDeptId, targetDeptId);
        }

        if (DataScope.DEPT_AND_CHILD.equals(dataScope) || DataScope.CUSTOM.equals(dataScope)) {
            if (Objects.equals(userDeptId, targetDeptId)) {
                return true;
            }
            return !CollectionUtils.isEmpty(childDeptIds) && childDeptIds.contains(targetDeptId);
        }

        return false;
    }

    /**
     * 构建部门显示名称。
     */
    public static String getDeptDisplayName(SysDept dept) {
        if (dept == null) {
            return "未知部门";
        }
        String deptName = StringUtils.hasText(dept.getDeptName()) ? dept.getDeptName() : "未知部门";
        String deptTypeDisplay = dept.getDeptTypeDisplay();
        if (StringUtils.hasText(deptTypeDisplay)) {
            return deptName + " (" + deptTypeDisplay + ")";
        }
        return deptName;
    }

    /**
     * 判断子部门列表中是否包含目标部门。
     */
    public static boolean isChildDeptByList(List<Long> childDeptIds, Long childDeptId) {
        return childDeptId != null
            && !CollectionUtils.isEmpty(childDeptIds)
            && childDeptIds.contains(childDeptId);
    }

    /**
     * 构建部门权限 SQL 片段。
     *
     * @param tableAlias   表别名，可为空
     * @param deptField    部门字段名，默认 {@code dept_id}
     * @param deptIds      允许访问的部门 ID 集合
     * @return SQL 条件字符串
     */
    public static String buildDeptPermissionSql(String tableAlias,
                                                String deptField,
                                                Collection<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return "";
        }
        String column = StringUtils.hasText(deptField) ? deptField : "dept_id";
        if (StringUtils.hasText(tableAlias)) {
            column = tableAlias + "." + column;
        }

        String idList = deptIds.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        if (!StringUtils.hasText(idList)) {
            return "";
        }
        return column + " IN (" + idList + ")";
    }
}
