package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.*;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.user.MemberDto;

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
    List<MemberDto> getTenants(Long appId, String account);

    /**
     * 生成加密Code
     *
     * @param dto CodeDTO
     * @return Reply
     */
    String generateCode(CodeDto dto);

    /**
     * 获取授权码
     *
     * @return Reply
     */
    String getAuthCode();

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
    String getSubmitToken(String key);

    /**
     * 生成Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    TokenDto generateToken(LoginDto login);

    /**
     * 通过微信授权码获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    TokenDto getTokenWithWeChat(LoginDto login);

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    TokenDto getTokenWithUserInfo(LoginDto login);

    /**
     * 扫码授权获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    TokenDto getTokenWithCode(LoginDto login);

    /**
     * 获取指定应用的Token
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌
     * @param appId 应用ID
     * @return Reply
     */
    TokenDto getToken(String fingerprint, AccessToken token, Long appId);

    /**
     * 刷新访问令牌过期时间
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌
     * @return Reply
     */
    TokenDto refreshToken(String fingerprint, AccessToken token);

    /**
     * 用户账号离线
     *
     * @param tokenId 令牌ID
     */
    void deleteToken(String tokenId);

    /**
     * 获取用户授权码
     *
     * @param info 用户登录信息
     * @return Reply
     */
    List<String> getPermits(LoginInfo info);

    /**
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    List<NavDto> getNavigators(LoginInfo info);

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    List<FuncDto> getModuleFunctions(LoginInfo info, Long moduleId);
}
