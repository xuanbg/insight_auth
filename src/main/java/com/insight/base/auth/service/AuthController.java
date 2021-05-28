package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.LoginDto;
import com.insight.utils.Json;
import com.insight.utils.ReplyHelper;
import com.insight.utils.pojo.AccessToken;
import com.insight.utils.pojo.LoginInfo;
import com.insight.utils.pojo.Reply;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 租户服务控制器
 */
@CrossOrigin
@RestController
@RequestMapping("/base/auth")
public class AuthController {
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
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash
     * @return Reply
     */
    @GetMapping("/v1.0/tokens")
    public Reply getSubmitToken(@RequestParam String key) {
        return service.getSubmitToken(key);
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
        return service.getCode(account, type);
    }

    /**
     * 获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens")
    public Reply getToken(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String account = login.getAccount();
        if (account == null || account.isEmpty()) {
            return ReplyHelper.invalidParam("登录账号不能为空");
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
    @PostMapping("/v1.0/tokens/withWechatCode")
    public Reply getTokenWithWeChat(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String appId = login.getWeChatAppId();
        if (appId == null || appId.isEmpty()) {
            return ReplyHelper.invalidParam("weChatAppId不能为空");
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
    @PostMapping("/v1.0/tokens/withWechatUnionId")
    public Reply getTokenWithUserInfo(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String appId = login.getWeChatAppId();
        if (appId == null || appId.isEmpty()) {
            return ReplyHelper.invalidParam("weChatAppId不能为空");
        }

        return service.getTokenWithUserInfo(login);
    }

    /**
     * 验证Token
     *
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/status")
    public Reply verifyToken() {
        return ReplyHelper.success();
    }

    /**
     * 获取用户授权码
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/permits")
    public Reply getPermits(@RequestHeader("loginInfo") String loginInfo) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getPermits(info);
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
        AccessToken refreshToken = Json.toAccessToken(token);
        if (refreshToken == null) {
            return ReplyHelper.fail("未提供RefreshToken");
        }

        return service.refreshToken(fingerprint, refreshToken);
    }

    /**
     * 用户账号离线
     *
     * @param token 访问令牌字符串
     * @return Reply
     */
    @DeleteMapping("/v1.0/tokens")
    public Reply deleteToken(@RequestHeader(value = "Authorization") String token) {
        AccessToken accessToken = Json.toAccessToken(token);

        return service.deleteToken(accessToken.getId());
    }

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @GetMapping("/v1.0/{appId}/tenants")
    public Reply getTenants(@PathVariable Long appId, @RequestParam String account) {

        return service.getTenants(appId, account);
    }

    /**
     * 获取用户导航栏
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/navigators")
    public Reply getNavigators(@RequestHeader("loginInfo") String loginInfo) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getNavigators(info);
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param loginInfo 用户信息
     * @param moduleId  功能模块ID
     * @return Reply
     */
    @GetMapping("/v1.0/navigators/{id}/functions")
    public Reply getModuleFunctions(@RequestHeader("loginInfo") String loginInfo, @PathVariable("id") Long moduleId) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getModuleFunctions(info, moduleId);
    }
}
