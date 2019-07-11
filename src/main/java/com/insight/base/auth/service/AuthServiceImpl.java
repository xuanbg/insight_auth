package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.Token;
import com.insight.base.auth.common.dto.LoginDTO;
import com.insight.base.auth.common.dto.TokenPackage;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.util.*;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.Reply;
import com.insight.util.pojo.User;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 身份验证服务接口实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Core core;

    /**
     * 构造函数
     *
     * @param core Core
     */
    public AuthServiceImpl(Core core) {
        this.core = core;
    }

    /**
     * 获取Code
     *
     * @param account 用户登录账号
     * @param type    登录类型(0:密码登录、1:验证码登录)
     * @return Reply
     */
    @Override
    public Reply getCode(String account, int type) {
        String userId = core.getUserId(account);
        if (userId == null) {
            return ReplyHelper.notExist("账号或密码错误！");
        }

        String key = "User:" + userId;
        if (!Redis.hasKey(key)) {
            Redis.deleteKey(account);
            return ReplyHelper.fail("发生了一点小意外,请提交一次");
        }

        // 生成Code
        Object code;
        if (type == 0) {
            String password = Redis.get(key, "Password");
            if (password == null || password.isEmpty()) {
                return null;
            }

            code = core.getGeneralCode(userId, account, password);
        } else {
            String json = Redis.get(key, "User");
            if (json == null || json.isEmpty()) {
                return null;
            }

            User user = Json.toBean(json, User.class);
            code = core.getSmsCode(userId, user.getMobile());
            if ("短信发送失败".equals(code)) {
                return ReplyHelper.fail(code.toString());
            }
        }

        return ReplyHelper.success(code);
    }

    /**
     * 获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getToken(LoginDTO login) {
        // 验证签名
        String code = core.getCode(login.getSignature());
        if (code == null) {
            String account = login.getAccount();
            String userId = core.getUserId(account);
            String key = "User:" + userId;
            if (!Redis.hasKey(key)) {
                Redis.deleteKey(account);
                return ReplyHelper.fail("发生了一点小意外,请提交一次");
            }

            if (core.userIsInvalid(userId)) {
                int failureCount = Integer.parseInt(Redis.get(key, "FailureCount"));
                if (failureCount > 10) {
                    return ReplyHelper.fail("错误次数过多，请重设密码");
                }

                failureCount++;
                Redis.set(key, "FailureCount", failureCount);
                Redis.set(key, "LastFailureTime", new Date());
                logger.warn("账号[" + account + "]正在尝试使用错误的签名请求令牌!");

                return ReplyHelper.invalidParam("账号或密码错误！");
            }
        }

        // 验证用户
        String userId = core.getId(code);
        if (userId == null || userId.isEmpty()) {
            return ReplyHelper.fail("缓存异常");
        }

        String key = "User:" + userId;
        boolean isInvalid = Boolean.valueOf(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        TokenPackage tokens = core.creatorToken(code, login, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 通过微信授权码获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithWeChat(LoginDTO login) {
        String code = login.getCode();
        String weChatAppId = login.getWeChatAppId();

        WeChatUser weChatUser = core.getWeChatInfo(code, weChatAppId);
        if (weChatUser == null) {
            return ReplyHelper.invalidParam("微信授权失败");
        }

        String unionId = weChatUser.getUnionid();
        if (unionId == null || unionId.isEmpty()) {
            return ReplyHelper.fail("未取得微信用户的UnionID");
        }

        // 使用微信UnionID读取缓存,如用户不存在,则缓存微信用户信息(30分钟)后返回微信用户信息
        String userId = core.getUserId(unionId);
        if (userId == null || userId.isEmpty()) {
            String key = "Wechat:" + Util.md5(unionId + weChatAppId);
            Redis.set(key, Json.toJson(weChatUser), 30, TimeUnit.MINUTES);

            return ReplyHelper.success(weChatUser, false);
        }

        String key = "User:" + userId;
        boolean isInvalid = Boolean.valueOf(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);
        TokenPackage tokens = core.creatorToken(Generator.uuid(), login, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithUserInfo(LoginDTO login) {
        // 验证账号绑定的手机号
        String mobile = login.getAccount();
        if (!core.verifySmsCode(0, mobile, login.getCode(), false)) {
            return ReplyHelper.invalidParam("短信验证码错误");
        }

        // 从缓存读取微信用户信息
        String unionId = login.getUnionId();
        String key = "Wechat:" + unionId;
        String json = Redis.get(key);
        WeChatUser weChatUser = Json.toBean(json, WeChatUser.class);
        if (weChatUser == null) {
            return ReplyHelper.invalidParam("微信授权已过期，请重新登录");
        }

        String userId = core.getUserId(mobile);
        if (userId == null || userId.isEmpty()) {
            // TODO 根据微信用户信息创建用户

            core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
            TokenPackage tokens = core.creatorToken(Generator.uuid(), login, userId);

            return ReplyHelper.success(tokens);
        }

        User user = core.getUser(userId);
        if (!unionId.equals(user.getUnionId())) {
            if (login.getReplace()) {
                user.setUnionId(unionId);
                key = "User:" + userId;
                Redis.set(key, "User", Json.toJson(user));
            } else {
                return ReplyHelper.invalidParam("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
            }
        }

        key = "User:" + userId;
        boolean isInvalid = Boolean.valueOf(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
        TokenPackage tokens = core.creatorToken(Generator.uuid(), login, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 验证访问令牌
     *
     * @param hash        令牌哈希值
     * @param accessToken 刷新令牌
     * @return Reply
     */
    @Override
    public Reply verifyToken(String hash, AccessToken accessToken) {
        String tokenId = accessToken.getId();
        String userId = accessToken.getUserId();

        // 验证令牌
        Token token = core.getToken(tokenId);
        if (token == null || !token.verify(hash, TokenType.AccessToken, tokenId, userId)) {
            return ReplyHelper.invalidToken();
        }

        // 验证用户
        String key = "User:" + userId;
        boolean isInvalid = Boolean.valueOf(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        long life = token.getLife();
        Date expiry = new Date(life / 2 + System.currentTimeMillis());
        if (token.getAutoRefresh() && expiry.after(token.getExpiryTime())) {
            token.setExpiryTime(new Date(life + System.currentTimeMillis()));
            token.setFailureTime(new Date(life * 12 + System.currentTimeMillis()));
            Redis.set("Token:" + tokenId, Json.toJson(token), life, TimeUnit.MILLISECONDS);
        }

        return ReplyHelper.success();
    }

    /**
     * 刷新访问令牌过期时间
     *
     * @param fingerprint 用户特征串
     * @param accessToken 刷新令牌
     * @return Reply
     */
    @Override
    public Reply refreshToken(String fingerprint, AccessToken accessToken) {
        String tokenId = accessToken.getId();
        String userId = accessToken.getUserId();
        String secret = accessToken.getSecret();

        // 验证令牌
        Token token = core.getToken(tokenId);
        if (token == null || !token.verify(secret, TokenType.RefreshToken, tokenId, userId)) {
            return ReplyHelper.invalidToken();
        }

        // 验证用户
        String key = "User:" + userId;
        boolean isInvalid = Boolean.valueOf(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        TokenPackage tokens = core.refreshToken(token, tokenId, fingerprint, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 用户账号离线
     *
     * @param hash        令牌哈希值
     * @param accessToken 访问令牌
     * @return Reply
     */
    @Override
    public Reply deleteToken(String hash, AccessToken accessToken) {
        String tokenId = accessToken.getId();
        String userId = accessToken.getUserId();

        // 验证令牌
        Token token = core.getToken(tokenId);
        if (token == null || !token.verify(hash, TokenType.AccessToken, tokenId, userId)) {
            return ReplyHelper.invalidToken();
        }

        core.deleteToken(tokenId);

        return ReplyHelper.success();
    }

    /**
     * 获取用户导航栏
     *
     * @return Reply
     */
    @Override
    public Reply getNavigators() {
        return null;
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param navigatorId 导航ID
     * @return Reply
     */
    @Override
    public Reply getModuleFunctions(String navigatorId) {
        return null;
    }

    /**
     * 验证支付密码
     *
     * @param payPassword 支付密码(MD5)
     * @return Reply
     */
    @Override
    public Reply verifyPayPassword(String payPassword) {

        return null;
    }

    /**
     * 生成短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码;5:修改手机号)
     * @param key     手机号或手机号+验证答案的Hash值
     * @param minutes 验证码有效时长(分钟)
     * @param length  验证码长度
     * @return Reply
     */
    @Override
    public Reply getSmsCode(int type, String key, int minutes, int length) {
        return null;
    }

    /**
     * 验证短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码;5:修改手机号)
     * @param mobile  手机号
     * @param code    验证码
     * @param isCheck 是否检验模式(true:检验模式,验证后验证码不失效;false:验证模式,验证后验证码失效)
     * @return Reply
     */
    @Override
    public Reply verifySmsCode(int type, String mobile, String code, Boolean isCheck) {
        return null;
    }
}
