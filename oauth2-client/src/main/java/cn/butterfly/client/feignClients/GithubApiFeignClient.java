package cn.butterfly.client.feignClients;

import cn.butterfly.client.entity.GithubUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * @Author Wuxianda
 * @Date 2023/9/28 15:41
 * @Version 1.0
 */
@FeignClient(name = "github-api", url = "https://api.github.com")
public interface GithubApiFeignClient {

    /**
     * 获取github用户信息
     * @param headers
     * @return
     */
    @GetMapping("/user")
    GithubUser getUserInfo(@RequestHeader MultiValueMap<String, String> headers);

    
}
