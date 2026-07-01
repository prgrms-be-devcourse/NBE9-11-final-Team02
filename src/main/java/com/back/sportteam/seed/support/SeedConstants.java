package com.back.sportteam.seed.support;

/**
 * 시더 전역 상수. 재현성을 위해 고정 시드를 사용하고,
 * 기존 운영 데이터와 충돌하지 않도록 고유 prefix를 둔다.
 */
public final class SeedConstants {

    private SeedConstants() {
    }

    /** 모든 랜덤 생성의 고정 시드. 같은 입력이면 항상 같은 결과를 보장한다. */
    public static final long RANDOM_SEED = 20260630L;

    /**
     * 시더 기준 "오늘" = 실행일. 날짜는 절대값이 아니라 이 값 기준 상대값으로 생성한다.
     * 팀원이 각자 서버를 아무 날에 띄워도 모집중/완료/정산대기 데모가 동일하게 동작하도록 하기 위함.
     * (정산 대기 J5는 matchDate == today 여야 하며, 정산 배치는 matchDate < today만 처리한다)
     */
    public static java.time.LocalDate today() {
        return java.time.LocalDate.now(com.back.sportteam.global.util.TimeUtils.SERVICE_ZONE);
    }

    /** 데이터 기간: [오늘 - 2개월, 오늘 + 1개월]. 과거=완료/취소, 미래=모집중/확정. */
    public static java.time.LocalDate periodStart() {
        return today().minusMonths(2);
    }

    public static java.time.LocalDate periodEnd() {
        return today().plusMonths(1);
    }

    /** 플랫폼 수수료율(application.yaml platform-fee-rate 기본값과 일치). */
    public static final java.math.BigDecimal PLATFORM_FEE_RATE = new java.math.BigDecimal("0.07");

    /** 기존 데이터와 충돌 회피용 prefix. */
    public static final String BULK_EMAIL_DOMAIN = "@seed.sportteam.local";
    public static final String MERCHANT_UID_PREFIX = "SEED-";

    /** seed_run 마커 식별자. 스펙이 바뀌면 version을 올려 재실행을 허용한다. */
    public static final String TYPE_FIXTURE = "FIXTURE";
    public static final String TYPE_BULK = "BULK";
    public static final int VERSION = 1;
}
