package com.jasonbertolo.urlshortener.api.config;

import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory(@Value("${spring.data.redis.host}") String redisHost,
                                                      @Value("${spring.data.redis.port}") Integer redisPort) {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public ReactiveRedisOperations<String, ShortUrl> reactiveRedisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ShortUrl> serializer =
                new Jackson2JsonRedisSerializer<>(new Jackson2ObjectMapperBuilder().build(), ShortUrl.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, ShortUrl> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, ShortUrl> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
