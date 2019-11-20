package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.LoginDto;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.LoginInfo;
import com.insight.util.pojo.Reply;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 身份验证服务接口
 */
public interface AuthService {

    /**
     * 获取Code
     *
     * @param account 用户登录账号
     * @param type    登录类型(0:密码登录、1:验证码登录)
     * @return Reply
     */
    Reply getCode(String account, int type);

    /**
     * 获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getToken(LoginDto login);

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
    Reply getTokenWithUserInfo(LoginDto login);

    /**
     * 获取用户授权码
     *
     * @param info 用户登录信息
     * @return Reply
     */
    Reply getPermits(LoginInfo info);

    /**
     * 刷新访问令牌过期时间
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌
     * @return Reply
     */
    Reply refreshToken(String fingerprint, AccessToken token);

    /**
     * 用户账号离线
     *
     * @param tokenId 令牌ID
     * @return Reply
     */
    Reply deleteToken(String tokenId);

    /**
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    Reply getNavigators(LoginInfo info);

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    Reply getModuleFunctions(LoginInfo info, String moduleId);
}
