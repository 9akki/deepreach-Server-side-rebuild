package com.deepreach.common.utils;

import com.deepreach.common.core.page.PageDomain;
import com.deepreach.common.core.page.TableSupport;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

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
        if (list instanceof com.github.pagehelper.Page) {
            return ((com.github.pagehelper.Page<T>) list).getTotal();
        }
        return list.size();
    }

    /**
     * 获取当前页码
     *
     * @param list 数据列表
     * @return 当前页码
     */
    public static <T> int getPageNum(List<T> list) {
        if (list instanceof com.github.pagehelper.Page) {
            return ((com.github.pagehelper.Page<T>) list).getPageNum();
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
        if (list instanceof com.github.pagehelper.Page) {
            return ((com.github.pagehelper.Page<T>) list).getPageSize();
        }
        return list.size();
    }

    /**
     * 获取总页数
     *
     * @param list 数据列表
     * @return 总页数
     */
    public static <T> int getPages(List<T> list) {
        if (list instanceof com.github.pagehelper.Page) {
            return ((com.github.pagehelper.Page<T>) list).getPages();
        }
        return 1;
    }
}