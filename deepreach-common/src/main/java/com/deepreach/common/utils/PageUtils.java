package com.deepreach.common.utils;

import com.deepreach.common.core.page.PageDomain;
import com.deepreach.common.core.page.TableSupport;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.Collections;
import java.util.List;

/**
 * 分页工具类
 *
 * 集成PageHelper实现真正的分页功能
 *
 * @author DeepReach Team
 */
public class PageUtils {

    /**
     * 手动分页信息上下文（在未启用 PageHelper 时使用）
     */
    private static final ThreadLocal<PageState> MANUAL_PAGE_STATE = new ThreadLocal<>();

    /**
     * 手动分页状态
     */
    public static final class PageState {
        private final long total;
        private final int pageNum;
        private final int pageSize;
        private final int pages;

        private PageState(long total, int pageNum, int pageSize) {
            long safeTotal = Math.max(total, 0);
            int safePageSize = pageSize <= 0 ? (int) (safeTotal > 0 ? safeTotal : 10) : pageSize;
            int safePageNum = pageNum <= 0 ? 1 : pageNum;
            this.total = safeTotal;
            this.pageSize = safePageSize;
            this.pageNum = safePageNum;
            this.pages = safePageSize <= 0
                ? (safeTotal > 0 ? 1 : 0)
                : (int) Math.ceil((double) safeTotal / safePageSize);
        }

        public long getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getPages() {
            return pages;
        }
    }

    /**
     * 设置请求分页数据
     */
    public static void startPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = pageDomain.getOrderBy();
        Boolean reasonable = pageDomain.getReasonable();

        // 使用PageHelper设置分页参数
        if (pageNum != null && pageSize != null) {
            // 合理化分页参数，防止页码超出范围
            MANUAL_PAGE_STATE.remove();
            if (reasonable != null && reasonable) {
                if (pageNum < 1) {
                    pageNum = 1;
                }
            }

            // 设置分页参数
            if (orderBy != null && !orderBy.trim().isEmpty()) {
                PageHelper.startPage(pageNum, pageSize, orderBy);
            } else {
                PageHelper.startPage(pageNum, pageSize);
            }

            System.out.println("PageHelper分页参数: pageNum=" + pageNum + ", pageSize=" + pageSize + ", orderBy=" + orderBy);
        }
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        // 清理PageHelper分页
        PageHelper.clearPage();
        MANUAL_PAGE_STATE.remove();
    }

    /**
     * 获取分页信息
     *
     * @param list 数据列表
     * @return 分页信息对象
     */
    public static <T> PageInfo<T> getPageInfo(List<T> list) {
        return new PageInfo<>(list);
    }

    /**
     * 获取总记录数
     *
     * @param list 数据列表
     * @return 总记录数
     */
    public static <T> long getTotal(List<T> list) {
        PageState manualState = MANUAL_PAGE_STATE.get();
        if (manualState != null) {
            return manualState.getTotal();
        }
        com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            return page.getTotal();
        }
        return list == null ? 0 : list.size();
    }

    /**
     * 获取当前页码
     *
     * @param list 数据列表
     * @return 当前页码
     */
    public static <T> int getPageNum(List<T> list) {
        PageState manualState = MANUAL_PAGE_STATE.get();
        if (manualState != null) {
            return manualState.getPageNum();
        }
        com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            return page.getPageNum();
        }
        return 1;
    }

    /**
     * 获取每页大小
     *
     * @param list 数据列表
     * @return 每页大小
     */
    public static <T> int getPageSize(List<T> list) {
        PageState manualState = MANUAL_PAGE_STATE.get();
        if (manualState != null) {
            return manualState.getPageSize();
        }
        com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            return page.getPageSize();
        }
        return list == null ? 0 : list.size();
    }

    /**
     * 获取总页数
     *
     * @param list 数据列表
     * @return 总页数
     */
    public static <T> int getPages(List<T> list) {
        PageState manualState = MANUAL_PAGE_STATE.get();
        if (manualState != null) {
            return manualState.getPages();
        }
        com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            return page.getPages();
        }
        return 1;
    }

    /**
     * 对已有集合执行手动分页，并在上下文中记录分页信息，供 {@link BaseController#getDataTable(List)} 等方法读取。
     *
     * @param source   完整数据集
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 当前页数据
     */
    public static <T> List<T> manualPage(List<T> source, Integer pageNum, Integer pageSize) {
        if (source == null || source.isEmpty()) {
            MANUAL_PAGE_STATE.set(new PageState(0, pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize));
            return Collections.emptyList();
        }
        int total = source.size();
        int size = pageSize == null || pageSize <= 0 ? total : pageSize;
        int current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int fromIndex = Math.max((current - 1) * size, 0);
        if (fromIndex >= total) {
            fromIndex = Math.max(total - size, 0);
        }
        int toIndex = Math.min(fromIndex + size, total);
        MANUAL_PAGE_STATE.set(new PageState(total, current, size));
        return source.subList(fromIndex, toIndex);
    }

    /**
     * 手动设置分页信息（适用于外部已计算好分页元数据的情况）
     */
    public static void setManualPage(long total, int pageNum, int pageSize) {
        MANUAL_PAGE_STATE.set(new PageState(total, pageNum, pageSize));
    }

    /**
     * 获取当前线程保存的手动分页状态
     */
    public static PageState getCurrentPageState() {
        return MANUAL_PAGE_STATE.get();
    }

    /**
     * 清理手动分页上下文（不影响 PageHelper）
     */
    public static void clearManualPage() {
        MANUAL_PAGE_STATE.remove();
    }
}
