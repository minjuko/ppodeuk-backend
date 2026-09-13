package bdbe.bdbd._core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {

    @Value("${cloud.aws.proxy.host}")
    private String proxyHost;

    @Value("${cloud.aws.proxy.port}")
    private int proxyPort;

    @Bean
    @Profile("prod")
    public RestTemplate restTemplateForProd() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (StringUtils.hasText(proxyHost) && proxyPort > 0) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
        }

        return new RestTemplate(requestFactory);
    }

    @Bean
    @Profile("!prod")
    public RestTemplate restTemplateForNonProd() {
        return new RestTemplate();
    }
}
