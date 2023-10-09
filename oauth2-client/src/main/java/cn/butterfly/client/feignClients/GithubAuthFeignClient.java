package cn.butterfly.client.feignClients;

import cn.butterfly.client.entity.GithubToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @Author Wuxianda
 * @Date 2023/9/28 15:41
 * @Version 1.0
 */
@FeignClient(name = "github-auth", url = "https://github.com/login/oauth")
public interface GithubAuthFeignClient {

    /**
     * 获取access_token
     * @param clientId
     * @param clientSecret
     * @param code
     * @return
     */
    @GetMapping("/access_token")
    GithubToken getToken(@RequestParam("clientId") String clientId, @RequestParam("clientSecret") String clientSecret, @RequestParam("code") String code);


}
