package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.TokenManage;
import com.insight.base.auth.common.dto.LoginDTO;
import com.insight.base.auth.common.dto.WeChatUserInfo;
import com.insight.util.Redis;
import com.insight.util.ReplyHelper;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.Reply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 身份验证服务接口实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final Core core;

    @Autowired
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

        TokenManage tokenManage = core.getUserInfo(userId);
        if (tokenManage == null) {
            Redis.deleteKey(account);
            return ReplyHelper.fail("发生了一点小意外,请提交一次");
        }

        // 生成Code
        Object code = core.generateCode(tokenManage, account, type);
        if ("短信发送失败".equals(code)) {
            return ReplyHelper.fail(code.toString());
        }
        return ReplyHelper.success(code);
    }

    /**
     * 获取Token数据
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getToken(LoginDTO login) {
        return null;
    }

    /**
     * 通过微信授权码获取访问令牌
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithWeChat(LoginDTO login) {
        return null;
    }

    /**
     * 通过微信用户信息获取访问令牌
     *
     * @param info 用户信息对象实体
     * @return Reply
     */
    @Override
    public Reply getTokenWithUserInfo(WeChatUserInfo info) {
        return null;
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
     * 刷新访问令牌过期时间
     *
     * @param token 刷新令牌字符串
     * @return Reply
     */
    @Override
    public Reply refreshToken(AccessToken token) {
        return null;
    }

    /**
     * 用户账号离线
     *
     * @param tokenId  令牌ID
     * @param deviceId 设备ID
     * @return Reply
     */
    @Override
    public Reply deleteToken(String tokenId, String deviceId) {
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
