package com.back.sportteam.seed;

/**
 * 시더 단위. 프로파일(seed-fixture / seed-bulk)에 따라 빈이 등록되며,
 * {@link SeedRunner}가 멱등성 마커를 확인한 뒤 실행한다.
 */
public interface SeedTask {

    /** seed_run 마커의 seed_type. */
    String markerType();

    /** 실제 데이터 생성. 멱등성/마커는 SeedRunner가 관리하므로 여기서는 생성 로직만 담는다. */
    void seed();
}
