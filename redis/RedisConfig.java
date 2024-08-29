package com.easychat.redis;

import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.config.Config;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig<v> {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:}")
    private String redisHost;

    @Value("${spring.redis.port:}")
    private Integer redisPort;

    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
           Config config = new Config();
            //配置发送地址
           config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
           RedissonClient redissonClient = Redisson.create(config);
           return redissonClient;
        }catch (Exception e){
            logger.error("redis配置错误，请检查redis配置");

        }

        return null;
    }

    @Bean("redisTemplate")
    public RedisTemplate<String,v> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, v> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        //设置key的序列化的方式
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }
}
