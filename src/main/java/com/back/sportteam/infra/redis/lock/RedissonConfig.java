package com.back.sportteam.infra.redis.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("!test")
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(DataRedisProperties redisProperties) {
        String address = "redis://%s:%d".formatted(redisProperties.getHost(), redisProperties.getPort());
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(resolvePassword(redisProperties.getPassword()))
                .setDatabase(redisProperties.getDatabase());
        return Redisson.create(config);
    }

    private String resolvePassword(String password) {
        if (StringUtils.hasText(password)) {
            return password;
        }
        return null;
    }
}
