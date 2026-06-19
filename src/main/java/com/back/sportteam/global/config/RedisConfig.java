package com.back.sportteam.global.config;

import com.back.sportteam.domain.match.subscriber.MatchStatusSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private static final String MATCH_STATUS_CHANNEL_PATTERN = "match:status:*";

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter matchStatusListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                matchStatusListenerAdapter,
                new PatternTopic(MATCH_STATUS_CHANNEL_PATTERN)
        );
        return container;
    }

    @Bean
    public MessageListenerAdapter matchStatusListenerAdapter(MatchStatusSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}