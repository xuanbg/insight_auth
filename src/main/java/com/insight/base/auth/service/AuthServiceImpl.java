package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.Token;
import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.client.RabbitClient;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.util.*;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.LoginInfo;
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
    private final MessageClient messageClient;
    private final Core core;

    /**
     * 构造函数
     *
     * @param messageClient MessageClient
     * @param core          Core
     */
    public AuthServiceImpl(MessageClient messageClient, Core core) {
        this.messageClient = messageClient;
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
        String code;
        if (type == 0) {
            String password = Redis.get(key, "Password");
            if (password == null || password.isEmpty()) {
                return null;
            }

            code = core.getGeneralCode(userId, account, password);
        } else {
            code = core.getSmsCode(userId, account);
            if(!code.matches("[0-9a-f]{32}")){
                return ReplyHelper.fail(code);
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
    public Reply getToken(LoginDto login) {
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
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        TokenDto tokens = core.creatorToken(code, login, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 通过微信授权码获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithWeChat(LoginDto login) {
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
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);
        TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithUserInfo(LoginDto login) {
        // 验证账号绑定的手机号
        String mobile = login.getAccount();
        String verifyKey = Util.md5(0 + mobile + login.getCode());
        Reply reply = messageClient.verifySmsCode(verifyKey);
        if (!reply.getSuccess()) {
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
            String account = Generator.uuid();
            String password = Util.md5(Generator.uuid());
            User user = new User();
            user.setName(weChatUser.getNickname());
            user.setAccount(account);
            user.setMobile(mobile);
            user.setUnionId(unionId);
            user.setPassword(password);
            user.setHeadImg(weChatUser.getHeadimgurl());
            RabbitClient.sendTopic(user);

            core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
            TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

            return ReplyHelper.success(tokens);
        }

        User user = core.getUser(userId);
        if (unionId.equals(user.getUnionId())) {
            if (login.getReplace()) {
                user.setUnionId(unionId);
                key = "User:" + userId;
                Redis.set(key, "User", Json.toJson(user));
            } else {
                return ReplyHelper.invalidParam("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
            }
        }

        key = "User:" + userId;
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
        TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

        return ReplyHelper.success(tokens);
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
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "IsInvalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止登录");
        }

        TokenDto tokens = core.refreshToken(token, tokenId, fingerprint, userId);

        return ReplyHelper.success(tokens);
    }

    /**
     * 用户账号离线
     *
     * @param tokenId 令牌ID
     * @return Reply
     */
    @Override
    public Reply deleteToken(String tokenId) {
        core.deleteToken(tokenId);

        return ReplyHelper.success();
    }

    /**
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public Reply getNavigators(LoginInfo info) {
        if (!core.containsApp(info.getTenantId(), info.getAppId())) {
            return ReplyHelper.invalidParam();
        }

        return ReplyHelper.success(core.getNavigators(info));
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    @Override
    public Reply getModuleFunctions(LoginInfo info, String moduleId) {
        return ReplyHelper.success(core.getModuleFunctions(info, moduleId));
    }
}
