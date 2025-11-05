package com.deepreach.common.web;

import com.deepreach.common.core.page.PageDomain;
import com.deepreach.common.core.page.TableSupport;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.utils.PageUtils;
import com.deepreach.common.web.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 基础控制器
 *
 * @author DeepReach Team
 */
public abstract class BaseController {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    @ModelAttribute
    public void setReqAndRes(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * 设置请求分页数据
     */
    protected void startPage() {
        PageUtils.startPage();
    }

    /**
     * 设置请求排序数据
     */
    protected void startOrderBy() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (pageDomain.getOrderBy() != null && !pageDomain.getOrderBy().isEmpty()) {
            String orderBy = pageDomain.getOrderBy();
            // TODO: 实现排序逻辑
            logger.info("排序参数: {}", orderBy);
        }
    }

    /**
     * 清理分页的线程变量
     */
    protected void clearPage() {
        PageUtils.clearPage();
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> TableDataInfo<T> getDataTable(List<?> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows((List<T>) list);

        com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            rspData.setTotal(page.getTotal());
            rspData.setPageNum(page.getPageNum());
            rspData.setPageSize(page.getPageSize());
            rspData.setPages(page.getPages());
        } else {
            com.deepreach.common.utils.PageUtils.PageState manualState = com.deepreach.common.utils.PageUtils.getCurrentPageState();
            if (manualState != null) {
                rspData.setTotal(manualState.getTotal());
                rspData.setPageNum(manualState.getPageNum());
                rspData.setPageSize(manualState.getPageSize());
                rspData.setPages(manualState.getPages());
                com.deepreach.common.utils.PageUtils.clearManualPage();
            } else {
                int size = list == null ? 0 : list.size();
                rspData.setTotal(size);
                rspData.setPageNum(1);
                rspData.setPageSize(size);
                rspData.setPages(size > 0 ? 1 : 0);
            }
        }

        return rspData;
    }

    /**
     * 获取当前用户ID
     */
    protected Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * 获取客户端IP地址
     */
    protected String getClientIp() {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 成功响应
     */
    protected <T> Result<T> success() {
        return Result.success();
    }

    /**
     * 成功响应（带数据）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功响应（自定义消息）
     */
    protected <T> Result<T> success(String message, T data) {
        return Result.success(message, data);
    }

    /**
     * 失败响应
     */
    protected <T> Result<T> error() {
        return Result.error();
    }

    /**
     * 失败响应（自定义消息）
     */
    protected <T> Result<T> error(String message) {
        return Result.error(message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    protected <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }
}
