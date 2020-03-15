package com.insight.base.auth.common.client;

import com.insight.base.auth.common.config.FeignClientConfig;
import com.insight.util.pojo.NormalMessage;
import com.insight.util.pojo.Reply;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author 宣炳刚
 * @date 2019-08-31
 * @remark 消息中心Feign客户端
 */
@FeignClient(name = "common-message", configuration = FeignClientConfig.class)
public interface MessageClient {

    /**
     * 同步发短信
     *
     * @param message 标准信息DTO
     * @return Reply
     */
    @PostMapping("/common/message/v1.0/messages")
    Reply sendMessage(@RequestBody NormalMessage message);

    /**
     * 验证短信验证码
     *
     * @param key 验证参数,MD5(type + mobile + code)
     * @return Reply
     */
    @GetMapping("/common/message/v1.0/codes/{key}/status?isCheck=false")
    Reply verifySmsCode(@PathVariable String key);
}
