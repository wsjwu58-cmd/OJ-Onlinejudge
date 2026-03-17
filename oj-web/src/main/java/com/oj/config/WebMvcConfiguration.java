package com.oj.config;

import com.oj.interceptor.JwtTokenAdminInterceptor;
import com.oj.interceptor.JwtTokenUserInterceptor;
import com.oj.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;


import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/user/login");
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/userLogin/login");

    }


    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("刷题项目接口文档")
                        .description("刷题项目接口文档")
                        .version("2.0"));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.sky.controller.admin")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .packagesToScan("com.sky.controller.User")
                .build();
    }

    /**
     * 设置静态资源映射（Knife4j文档页面）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    //SpringMVC消息转化器
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("消息转换器");
        //创建消息转换器
        MappingJackson2HttpMessageConverter converter=new MappingJackson2HttpMessageConverter();
        //为消息转换器设置一个对象处理器
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转换器加入容器中
        converters.add(0,converter);

    }
    @Bean
    public RedisVectorStore vectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedis = new JedisPooled(
                new HostAndPort("192.168.141.128", 6378),
                DefaultJedisClientConfig.builder()
                        .password("qwer1234")
                        .database(0)
                        .build()
        );

        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName("knowledge_index")
                .prefix("doc:")
                .initializeSchema(true)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
