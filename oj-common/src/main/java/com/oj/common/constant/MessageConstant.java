package com.oj.common.constant;

public class MessageConstant {
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_TOKEN = "您不是管理员！";
    public static final String PROBLEM_TYPE = "该分类下关联题目";
    public static final String PROBLEM_TYPE_GROUP = "该分类下关联题组";
    public static final String CONTEXT_PROBLEM = "该题目关联了比赛，不能下架";
    public static final String GROUP_PROBLEM = "该题目关联了题组，不能下架";
    public static final String CONTEST_PROBLEM = "该比赛关联了题目，不能下架";
    public static final String PROBLEM_STATUS = "该题目处于上架状态，不能删除";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";

    // 判题相关
    public static final String JUDGE0_ERROR = "判题服务异常";
    public static final String TESTCASE_NOT_FOUND = "测试用例不存在";
    public static final String JUDGE_TIMEOUT = "判题超时";
    public static final String CODE_EXECUTION_ERROR = "代码执行错误";

    // 数据库相关
    public static final String DATABASE_ERROR = "数据库操作异常";
    public static final String DATA_INSERT_FAILED = "数据插入失败";
    public static final String DATA_UPDATE_FAILED = "数据更新失败";
    public static final String DATA_DELETE_FAILED = "数据删除失败";
    public static final String DATA_QUERY_FAILED = "数据查询失败";

    // Redis相关
    public static final String REDIS_ERROR = "Redis操作异常";
    public static final String LUA_EXECUTION_ERROR = "Lua脚本执行异常";

    // WebSocket相关
    public static final String WEBSOCKET_ERROR = "WebSocket操作异常";

    // 微服务相关
    public static final String FEIGN_CALL_FAILED = "服务调用失败";
    public static final String USER_INFO_HEADER_MISSING = "用户信息头缺失";
}
