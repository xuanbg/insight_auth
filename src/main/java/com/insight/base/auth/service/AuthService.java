package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.LoginDTO;
import com.insight.util.pojo.AccessToken;
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
    Reply getToken(LoginDTO login);

    /**
     * 通过微信授权码获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithWeChat(LoginDTO login);

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    Reply getTokenWithUserInfo(LoginDTO login);

    /**
     * 验证访问令牌
     *
     * @param hash  令牌哈希值
     * @param token 访问令牌
     * @return Reply
     */
    Reply verifyToken(String hash, AccessToken token);

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
     * @param hash  令牌哈希值
     * @param token 访问令牌
     * @return Reply
     */
    Reply deleteToken(String hash, AccessToken token);

    /**
     * 验证支付密码
     *
     * @param payPassword 支付密码(MD5)
     * @return Reply
     */
    Reply verifyPayPassword(String payPassword);

    /**
     * 生成短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码;5:修改手机号)
     * @param key     手机号或手机号+验证答案的Hash值
     * @param minutes 验证码有效时长(分钟)
     * @param length  验证码长度
     * @return Reply
     */
    Reply getSmsCode(int type, String key, int minutes, int length);

    /**
     * 验证短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码;5:修改手机号)
     * @param mobile  手机号
     * @param code    验证码
     * @param isCheck 是否检验模式(true:检验模式,验证后验证码不失效;false:验证模式,验证后验证码失效)
     * @return Reply
     */
    Reply verifySmsCode(int type, String mobile, String code, Boolean isCheck);

    /**
     * 获取用户导航栏
     *
     * @return Reply
     */
    Reply getNavigators();

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param navigatorId 导航ID
     * @return Reply
     */
    Reply getModuleFunctions(String navigatorId);
}
