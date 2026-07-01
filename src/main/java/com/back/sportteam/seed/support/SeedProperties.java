package com.back.sportteam.seed.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 벌크 시더 규모 설정. 기본은 데모용(155), 부하테스트 시 matches만 늘려 호출한다.
 * 예: SEED_BULK_MATCHES=500 (env) 또는 seed.bulk.matches=500 (yaml)
 */
@ConfigurationProperties(prefix = "seed.bulk")
public record SeedProperties(
        Integer users,
        Integer matches
) {
    public SeedProperties {
        if (users == null) {
            users = 100;
        }
        if (matches == null) {
            matches = 155;
        }
    }
}
