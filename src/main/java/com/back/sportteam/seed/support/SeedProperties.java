package com.back.sportteam.seed.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시더 설정. seed.bulk.* 와 seed.fixture.* 를 함께 바인딩한다.
 * 예: SEED_BULK_MATCHES=500 (env) 또는 seed.bulk.matches=500 (yaml)
 *     SEED_FIXTURE_PASSWORD=... (env) — 픽스처 계정 비밀번호, 코드에 하드코딩 금지
 */
@ConfigurationProperties(prefix = "seed")
public record SeedProperties(
        Bulk bulk,
        Fixture fixture
) {
    public SeedProperties {
        if (bulk == null) bulk = new Bulk(null, null);
        if (fixture == null) fixture = new Fixture(null);
    }

    public record Bulk(Integer users, Integer matches) {
        public Bulk {
            if (users == null) users = 100;
            if (matches == null) matches = 155;
        }
    }

    public record Fixture(String password) {}
}
