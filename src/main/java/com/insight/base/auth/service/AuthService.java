package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.*;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.base.DataBase;
import com.insight.utils.pojo.base.Reply;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 身份验证服务接口
 */
public interface AuthService {

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    List<DataBase> getTenants(Long appId, String account);

    /**
     * 生成加密Code
     *
     * @param dto CodeDTO
     * @return Reply
     */
    Reply generateCode(CodeDto dto);

    /**
     * 获取授权码
     *
     * @return Reply
     */
    Reply getAuthCode();

    /**
     * 扫码授权
     *
     * @param info 用户登录信息
     * @param code 授权识别码
     */
    void authWithCode(LoginInfo info, String code);

    /**
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash
     * @return Reply
     */
    Reply getSubmitToken(String key);

    /**
     * 生成Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply generateToken(LoginDto login);

    /**
     * 通过微信授权码获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithWeChat(LoginDto login);

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithUnionId(LoginDto login);

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithUserInfo(LoginDto login);

    /**
     * 扫码授权获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithCode(LoginDto login);

    /**
     * 获取指定应用的Token
     *
     * @param login 用户登录数据
     * @param key   令牌
     * @return Reply
     */
    Reply getToken(LoginDto login, TokenKey key);

    /**
     * 用户账号离线
     *
     * @param key 用户关键信息
     */
    void deleteToken(TokenKey key);

    /**
     * 获取用户授权码
     *
     * @param key 用户关键信息
     * @return 授权码集合
     */
    List<String> getPermits(TokenKey key);

    /**
     * 获取用户导航栏
     *
     * @param key 用户关键信息
     * @return 导航数据集合
     */
    List<NavDto> getNavigators(TokenKey key);

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param moduleId 功能模块ID
     * @return 功能及权限数据集合
     */
    List<FuncDto> getModuleFunctions(Long tenantId, Long userId, Long moduleId);

    /**
     * 获取学科网授权
     *
     * @param dto 回调数据
     * @return 学科网OpenID
     */
    String xkwAuth(CallbackDto dto);
}
