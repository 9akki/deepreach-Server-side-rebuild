package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志记录实体类 sys_oper_log
 *
 * 系统操作日志的核心实体，用于记录用户的操作行为：
 * 1. 用户操作的基本信息（操作人、时间、IP等）
 * 2. 操作的业务信息（模块、方法、参数等）
 * 3. 操作结果信息（成功/失败、错误信息等）
 * 4. 请求上下文信息（URL、方法、参数等）
 * 5. 安全审计相关信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     *
     * 操作日志的唯一标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long operId;

    /**
     * 操作模块
     *
     * 操作所属的功能模块名称
     * 如："用户管理"、"角色管理"、"系统设置"等
     * 用于日志的分类和检索
     */
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     *
     * 操作的业务类型标识：
     * 0 - 其它：无法归类的操作
     * 1 - 新增：数据新增操作
     * 2 - 修改：数据修改操作
     * 3 - 删除：数据删除操作
     * 4 - 授权：权限授权操作
     * 5 - 导出：数据导出操作
     * 6 - 导入：数据导入操作
     *
     * 用于操作分类和统计分析
     */
    private Integer businessType;

    /**
     * 请求方法
     *
     * 请求的具体方法名称，包含类名和方法名
     * 如："com.deepreach.system.service.impl.SysUserServiceImpl.insertUser"
     * 用于精确定位操作的代码位置
     */
    private String method;

    /**
     * 请求方式
     *
     * HTTP请求方法类型：
     * GET - 查询操作
     * POST - 创建操作
     * PUT - 更新操作
     * DELETE - 删除操作
     *
     * 用于请求方式的统计和分析
     */
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     *
     * 操作发起者的类型：
     * 0 - 其它：无法归类的操作者
     * 1 - 后台用户：通过Web管理后台操作
     * 2 - 手机端用户：通过移动端APP操作
     *
     * 用于区分不同客户端的操作行为
     */
    private Integer operatorType;

    /**
     * 操作人员
     *
     * 执行操作的用户名
     * 如："admin"、"test_user"等
     * 用于操作者的身份识别
     */
    private String operName;

    /**
     * 部门名称
     *
     * 操作者所属的部门名称
     * 如："技术部"、"运营部"等
     * 用于按部门统计操作行为
     */
    private String deptName;

    /**
     * 请求URL
     *
     * 请求的完整URL地址
     * 如："http://localhost:8080/api/system/user/list"
     * 用于操作来源的追踪
     */
    private String operUrl;

    /**
     * 操作地址
     *
     * 操作者的IP地址
     * 如："192.168.1.100"、"127.0.0.1"等
     * 用于安全审计和异常追踪
     */
    private String operIp;

    /**
     * 操作地点
     *
     * 根据IP地址解析的地理位置
     * 如："广东省深圳市"、"北京市"等
     * 用于操作地点的统计分析
     */
    private String operLocation;

    /**
     * 请求参数
     *
     * 请求的参数信息，JSON格式
     * 如："{\"username\":\"admin\",\"status\":\"0\"}"
     * 用于操作的详细记录和问题排查
     */
    private String operParam;

    /**
     * 返回参数
     *
     * 操作的返回结果，JSON格式
     * 如："{\"code\":200,\"msg\":\"操作成功\"}"
     * 用于操作结果的详细记录
     */
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     *
     * 操作的执行状态：
     * 0 - 正常：操作执行成功
     * 1 - 异常：操作执行失败
     *
     * 用于操作成功率的统计
     */
    private Integer status;

    /**
     * 错误消息
     *
     * 操作失败时的错误信息
     * 如："用户名已存在"、"参数验证失败"等
     * 用于错误原因的分析和排查
     */
    private String errorMsg;

    /**
     * 操作时间
     *
     * 操作执行的时间戳
     * 精确到毫秒级别，用于操作时间的精确记录
     */
    private LocalDateTime operTime;

    /**
     * 消耗时间
     *
     * 操作执行所消耗的时间，单位：毫秒
     * 用于性能监控和优化分析
     */
    private Long costTime;

    // ==================== 业务判断方法 ====================

    /**
     * 判断操作是否成功
     *
     * @return true如果操作成功，false如果操作失败
     */
    public boolean isSuccess() {
        return Integer.valueOf(0).equals(this.status);
    }

    /**
     * 判断操作是否失败
     *
     * @return true如果操作失败，false如果操作成功
     */
    public boolean isFailed() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断是否为后台用户操作
     *
     * @return true如果是后台用户操作，false否则
     */
    public boolean isBackendUser() {
        return Integer.valueOf(1).equals(this.operatorType);
    }

    /**
     * 判断是否为手机端用户操作
     *
     * @return true如果是手机端用户操作，false否则
     */
    public boolean isMobileUser() {
        return Integer.valueOf(2).equals(this.operatorType);
    }

    /**
     * 判断是否为新增操作
     *
     * @return true如果是新增操作，false否则
     */
    public boolean isInsert() {
        return Integer.valueOf(1).equals(this.businessType);
    }

    /**
     * 判断是否为修改操作
     *
     * @return true如果是修改操作，false否则
     */
    public boolean isUpdate() {
        return Integer.valueOf(2).equals(this.businessType);
    }

    /**
     * 判断是否为删除操作
     *
     * @return true如果是删除操作，false否则
     */
    public boolean isDelete() {
        return Integer.valueOf(3).equals(this.businessType);
    }

    /**
     * 判断是否为GET请求
     *
     * @return true如果是GET请求，false否则
     */
    public boolean isGetRequest() {
        return "GET".equalsIgnoreCase(this.requestMethod);
    }

    /**
     * 判断是否为POST请求
     *
     * @return true如果是POST请求，false否则
     */
    public boolean isPostRequest() {
        return "POST".equalsIgnoreCase(this.requestMethod);
    }

    /**
     * 判断是否为PUT请求
     *
     * @return true如果是PUT请求，false否则
     */
    public boolean isPutRequest() {
        return "PUT".equalsIgnoreCase(this.requestMethod);
    }

    /**
     * 判断是否为DELETE请求
     *
     * @return true如果是DELETE请求，false否则
     */
    public boolean isDeleteRequest() {
        return "DELETE".equalsIgnoreCase(this.requestMethod);
    }

    /**
     * 获取业务类型显示文本
     *
     * @return 业务类型显示文本
     */
    public String getBusinessTypeDisplay() {
        switch (this.businessType) {
            case 0:
                return "其它";
            case 1:
                return "新增";
            case 2:
                return "修改";
            case 3:
                return "删除";
            case 4:
                return "授权";
            case 5:
                return "导出";
            case 6:
                return "导入";
            default:
                return "未知";
        }
    }

    /**
     * 获取操作状态显示文本
     *
     * @return 操作状态显示文本
     */
    public String getStatusDisplay() {
        if (Integer.valueOf(0).equals(this.status)) {
            return "正常";
        } else if (Integer.valueOf(1).equals(this.status)) {
            return "异常";
        } else {
            return "未知";
        }
    }

    /**
     * 获取操作者类型显示文本
     *
     * @return 操作者类型显示文本
     */
    public String getOperatorTypeDisplay() {
        switch (this.operatorType) {
            case 0:
                return "其它";
            case 1:
                return "后台用户";
            case 2:
                return "手机端用户";
            default:
                return "未知";
        }
    }

    /**
     * 获取简短的错误信息
     *
     * 获取错误信息的前50个字符，用于列表显示
     *
     * @return 简短的错误信息
     */
    public String getShortErrorMsg() {
        if (this.errorMsg == null || this.errorMsg.trim().isEmpty()) {
            return "";
        }
        if (this.errorMsg.length() <= 50) {
            return this.errorMsg;
        }
        return this.errorMsg.substring(0, 50) + "...";
    }

    /**
     * 获取简短的请求参数
     *
     * 获取请求参数的前100个字符，用于列表显示
     *
     * @return 简短的请求参数
     */
    public String getShortOperParam() {
        if (this.operParam == null || this.operParam.trim().isEmpty()) {
            return "";
        }
        if (this.operParam.length() <= 100) {
            return this.operParam;
        }
        return this.operParam.substring(0, 100) + "...";
    }

    // ==================== 数据转换方法 ====================

    /**
     * 创建登录操作日志
     *
     * @param username 用户名
     * @param loginIp 登录IP
     * @param operatorType 操作者类型
     * @return 操作日志对象
     */
    public static SysOperLog createLoginLog(String username, String loginIp, Integer operatorType) {
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle("用户登录");
        operLog.setBusinessType(0); // 其它
        operLog.setMethod("com.deepreach.auth.service.impl.AuthServiceImpl.login");
        operLog.setRequestMethod("POST");
        operLog.setOperatorType(operatorType);
        operLog.setOperName(username);
        operLog.setOperIp(loginIp);
        operLog.setOperParam("用户登录成功");
        operLog.setStatus(0); // 成功
        operLog.setOperTime(LocalDateTime.now());
        return operLog;
    }

    /**
     * 创建用户操作日志
     *
     * @param title 操作标题
     * @param businessType 业务类型
     * @param methodName 方法名
     * @param requestMethod 请求方法
     * @param operName 操作者
     * @param operIp 操作IP
     * @param operParam 操作参数
     * @param status 操作状态
     * @param errorMsg 错误信息
     * @return 操作日志对象
     */
    public static SysOperLog createUserOperLog(String title, Integer businessType, String methodName,
                                             String requestMethod, String operName, String operIp,
                                             String operParam, Integer status, String errorMsg) {
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(title);
        operLog.setBusinessType(businessType);
        operLog.setMethod(methodName);
        operLog.setRequestMethod(requestMethod);
        operLog.setOperatorType(1); // 默认后台用户
        operLog.setOperName(operName);
        operLog.setOperIp(operIp);
        operLog.setOperParam(operParam);
        operLog.setStatus(status);
        operLog.setErrorMsg(errorMsg);
        operLog.setOperTime(LocalDateTime.now());
        return operLog;
    }

    /**
     * 创建异常操作日志
     *
     * @param title 操作标题
     * @param methodName 方法名
     * @param operName 操作者
     * @param operIp 操作IP
     * @param exception 异常信息
     * @return 操作日志对象
     */
    public static SysOperLog createErrorLog(String title, String methodName, String operName, String operIp, Exception exception) {
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(title);
        operLog.setBusinessType(0); // 其它
        operLog.setMethod(methodName);
        operLog.setRequestMethod("UNKNOWN");
        operLog.setOperatorType(1); // 默认后台用户
        operLog.setOperName(operName);
        operLog.setOperIp(operIp);
        operLog.setOperParam("操作异常");
        operLog.setStatus(1); // 异常
        operLog.setErrorMsg(exception != null ? exception.getMessage() : "未知异常");
        operLog.setOperTime(LocalDateTime.now());
        return operLog;
    }
}