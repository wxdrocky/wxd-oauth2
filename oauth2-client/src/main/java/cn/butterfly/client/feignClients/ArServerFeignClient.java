package cn.butterfly.client.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * @Author Wuxianda
 * @Date 2023/9/28 15:41
 * @Version 1.0
 */
@FeignClient(name = "ar-server", url = "http://localhost:9900/A/ar/api")
public interface ArServerFeignClient {

    @PostMapping("/OA/interface/test")
    Map<String, Object> test(@RequestHeader MultiValueMap<String, String> headers);

    
}
