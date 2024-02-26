package com.jasonbertolo.urlshortener.api.repository;

import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface ShortUrlRepository extends ReactiveMongoRepository<ShortUrl, String> {

    Mono<ShortUrl> findByIdAndCreatedByAndExpiresAtAfter(String id, String createdBy, Instant expiredAt);

    Mono<ShortUrl> findByKeyAndExpiresAtAfter(String key, Instant expiredAt);

    Flux<ShortUrl> findByCreatedByAndExpiresAtAfterOrderByCreatedAtDesc(String createdBy, Instant expiredAt, Pageable pageable);
    Mono<Long> countByCreatedByAndExpiresAtAfter(String createdBy, Instant expiredAt);

    Flux<ShortUrl> findByExpiresAtBefore(Instant instant);
}
