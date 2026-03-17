package com.oj.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_TOKEN = "您不是管理员！";
    public static final String PROBLEM_TYPE = "该分类下关联题目";
    public static final String PROBLEM_TYPE_GROUP = "该分类下关联题组";
    public static final String Context_PROBLEM = "该题目关联了比赛，不能下架";
    public static final String GROUP_PROBLEM = "该题目关联了题组，不能下架";
    public static final String CONTEST_PROBLEM = "该比赛关联了题目，不能下架";
    public static final String PROBLEM_STATUS = "该题目处于上架状态，不能删除";

    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String Order_Ready="该订单不处于待接单状态";
    public static final String Order_Rejection_check="没有填写拒单理由";

    // 判题相关异常信息
    public static final String JUDGE0_ERROR = "判题服务异常";
    public static final String TESTCASE_NOT_FOUND = "测试用例不存在";
    public static final String JUDGE_TIMEOUT = "判题超时";
    public static final String CODE_EXECUTION_ERROR = "代码执行错误";

    // 数据库相关异常信息
    public static final String DATABASE_ERROR = "数据库操作异常";
    public static final String DATA_INSERT_FAILED = "数据插入失败";
    public static final String DATA_UPDATE_FAILED = "数据更新失败";
    public static final String DATA_DELETE_FAILED = "数据删除失败";
    public static final String DATA_QUERY_FAILED = "数据查询失败";

    // Redis相关异常信息
    public static final String REDIS_ERROR = "Redis操作异常";
    public static final String LUA_EXECUTION_ERROR = "Lua脚本执行异常";

    // WebSocket相关异常信息
    public static final String WEBSOCKET_ERROR = "WebSocket操作异常";

} 
