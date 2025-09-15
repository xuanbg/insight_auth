package com.insight.base.auth.common.xkw;

import com.insight.base.auth.common.dto.CallbackDto;
import com.insight.utils.EnvUtil;
import com.insight.utils.Json;
import com.insight.utils.Util;
import com.insight.utils.http.HttpClient;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.redis.HashOps;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark 学科网Auth模块通用工具类
 */
public class AuthUtil {
    private static final String appKey;
    private static final String appSecret;
    private static final String authUrl;
    private static final String redirectUri;

    static {
        appKey = EnvUtil.getValue("xkw.appKey");
        appSecret = EnvUtil.getValue("xkw.appSecret");
        authUrl = EnvUtil.getValue("xkw.authUrl");
        redirectUri = EnvUtil.getValue("xkw.redirectUri");
    }

    /**
     * 获取组卷网URL
     *
     * @param dto 回调参数
     * @return 组卷网URL
     */
    public static String getZjseUrl(CallbackDto dto) {
        var params = new TreeMap<String, Object>();
        params.put("_m", "https://bk.huixue-bao.com");
        params.put("_n", redirectUri);
        params.put("_callbackmode", 1);
        params.put("_pmm", 1);
        params.put("_openid", dto.getAuthId());

        var url = dto.getService();
        if ("https://zjse.xkw.com/".equals(url)) {
            var subjectId = dto.getSubjectId();
            if (subjectId != null) {
                var path = HashOps.get("xkw:path", subjectId);
                if (Util.isNotEmpty(path)) {
                    url = url + path + "/zj0";
                }
            }
        }

        return HttpClient.buildUrl(url, params);
    }

    /**
     * 获取学科网授权URL
     *
     * @param dto 回调参数
     * @return 授权URL
     */
    public static String getAuthUrl(CallbackDto dto) {
        var subjectId = dto.getSubjectId();
        var redirectUrl = subjectId == null ? redirectUri : redirectUri + "?subject_id=" + subjectId;
        dto.setAppSecret(appSecret);

        var params = new TreeMap<String, Object>();
        params.put("client_id", appKey);
        params.put("extra", dto.getExtra());
        params.put("open_id", dto.getOpenId());
        params.put("redirect_uri", redirectUrl);
        params.put("service", dto.getService());
        params.put("timespan", dto.getTimespan());

        var signature = generateSignature(params);
        params.put("signature", signature);

        var url = authUrl + "/oauth2/authorize";
        return HttpClient.buildUrl(url, params);
    }

    /**
     * 获取学科网OpenId
     *
     * @param code 授权码
     * @return OpenId
     */
    public static String getOpenId(String code) {
        var token = getAccessToken(code);
        return getProfile(token);
    }

    /**
     * 获取学科网AccessToken
     *
     * @param code 授权码
     * @return AccessToken
     */
    private static String getAccessToken(String code) {
        var params = new TreeMap<String, Object>();
        params.put("client_id", appKey);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);

        var signature = generateSignature(params);
        params.put("signature", signature);

        var url = HttpClient.buildUrl(authUrl + "/oauth2/accessToken", params);
        var data = HttpClient.get(url);

        var token = Json.toBean(data, AccessToken.class);
        if (token == null) {
            throw new BusinessException("获取AccessToken失败");
        } else if (Util.isEmpty(token.getToken())) {
            throw new BusinessException("获取AccessToken失败: " + token.getError());
        }

        return token.getToken();
    }

    /**
     * 获取授权用户信息
     *
     * @param token 授权令牌
     * @return OpenId
     */
    private static String getProfile(String token) {
        var url = authUrl + "/oauth2/profile?access_token=" + token;
        var data = HttpClient.get(url);

        var profile = Json.toBean(data, Profile.class);
        if (profile == null) {
            throw new BusinessException("获取授权用户信息失败");
        } else if (Util.isEmpty(profile.getOpenId())) {
            throw new BusinessException("获取授权用户信息失败: " + profile.getError());
        }

        return profile.getOpenId();
    }

    /**
     * 根据SortMap类型参数和secret获取签名
     *
     * @param param SortMap类型参数
     * @return 签名
     */
    private static String generateSignature(SortedMap<String, Object> param) {
        var sb = new StringBuilder();
        for (var entry : param.entrySet()) {
            sb.append(entry.getValue());
        }

        sb.append(appSecret);
        return Util.md5(sb.toString());
    }
}
