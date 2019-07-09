package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.LoginDTO;
import com.insight.util.Json;
import com.insight.util.ReplyHelper;
import com.insight.util.Util;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.Reply;
import com.insight.util.service.BaseController;
import org.springframework.web.bind.annotation.*;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 租户服务控制器
 */
@CrossOrigin
@RestController
@RequestMapping("/base/auth")
public class AuthController extends BaseController {
    private final AuthService service;

    /**
     * 构造方法
     *
     * @param service 自动注入的AuthService
     */
    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * 获取Code
     *
     * @param account 用户登录账号
     * @param type    登录类型(0:密码登录、1:验证码登录)
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/codes")
    public Reply getCode(@RequestParam String account, @RequestParam(defaultValue = "0") int type) {
        // 3秒内限请求1次,每用户每日限获取Code次数200次
        String key = Util.md5("getCode" + account + type);
        boolean limited = super.isLimited(key, 3, 86400, 200);
        if (limited) {
            return ReplyHelper.fail("每日获取Code次数上限为200次，请合理利用");
        }

        return service.getCode(account, type);
    }

    /**
     * 获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @GetMapping("/v1.0/tokens")
    public Reply getToken(@RequestHeader("fingerprint") String fingerprint, LoginDTO login) {
        // 3秒内限请求1次,每用户每日限获取Token次数200次
        String key = Util.md5("getToken" + fingerprint);
        boolean limited = super.isLimited(key, 3, 86400, 200);
        if (limited) {
            return ReplyHelper.fail("每日获取Code次数上限为200次，请合理利用");
        }

        login.setFingerprint(fingerprint);
        String appId = login.getAppId();
        if (appId == null || appId.isEmpty()) {
            return ReplyHelper.invalidParam("appId不能为空");
        }

        return service.getToken(login);
    }

    /**
     * 通过微信授权码获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/withWechatCode")
    public Reply getTokenWithWeChat(@RequestHeader("fingerprint") String fingerprint, LoginDTO login) {
        login.setFingerprint(fingerprint);
        String appId = login.getAppId();
        if (appId == null || appId.isEmpty()) {
            return ReplyHelper.invalidParam("appId不能为空");
        }

        return service.getTokenWithWeChat(login);
    }

    /**
     * 通过微信UnionId获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/withWechatUnionId")
    public Reply getTokenWithUserInfo(@RequestHeader("fingerprint") String fingerprint, LoginDTO login) {
        login.setFingerprint(fingerprint);
        String appId = login.getAppId();
        if (appId == null || appId.isEmpty()) {
            return ReplyHelper.invalidParam("appId不能为空");
        }

        return service.getTokenWithUserInfo(login);
    }

    /**
     * 验证Token
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌字符串
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/verify")
    public Reply verifyToken(@RequestHeader("fingerprint") String fingerprint, @RequestHeader("Authorization") String token) {
        AccessToken accessToken = Json.toAccessToken(token);
        if (accessToken == null) {
            return ReplyHelper.invalidToken();
        }

        String hash = Util.md5(token + fingerprint);

        return service.verifyToken(hash, accessToken);
    }

    /**
     * 刷新Token，延长过期时间至2小时后
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌字符串
     * @return Reply
     */
    @PutMapping("/v1.0/tokens")
    public Reply refreshToken(@RequestHeader("fingerprint") String fingerprint, @RequestHeader("Authorization") String token) {
        // 3秒内限请求1次,每用户每日刷新Token次数60次
        String key = Util.md5("refreshToken" + fingerprint);
        boolean limited = super.isLimited(key, 3, 86400, 60);
        if (limited) {
            return ReplyHelper.fail("每日刷新Token次数上限为60次，请合理利用");
        }

        AccessToken refreshToken = Json.toAccessToken(token);
        if (refreshToken == null) {
            return ReplyHelper.invalidToken();
        }

        return service.refreshToken(fingerprint, refreshToken);
    }

    /**
     * 用户账号离线
     *
     * @param fingerprint 用户特征串
     * @param token       访问令牌字符串
     * @return Reply
     */
    @DeleteMapping("/v1.0/tokens")
    public Reply deleteToken(@RequestHeader("fingerprint") String fingerprint, @RequestHeader(value = "Authorization") String token) {
        AccessToken accessToken = Json.toAccessToken(token);
        if (accessToken == null) {
            return ReplyHelper.invalidToken();
        }

        String hash = Util.md5(token + fingerprint);

        return service.deleteToken(hash, accessToken);
    }
}
