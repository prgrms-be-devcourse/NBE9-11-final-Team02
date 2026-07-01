package com.back.sportteam.seed.support;

/**
 * 픽스처 고정 UUID. 시연 중 누구나 동일한 데이터를 재현할 수 있도록 하드코딩한다.
 * existsById 체크와 함께 멱등성을 이중으로 보장한다.
 */
public final class SeedIds {

    private SeedIds() {
    }

    // --- 픽스처 유저 6명 (닉네임 / 설계 라벨) ---
    public static final String USER_MANAGER = "f0000000-0000-0000-0000-000000000001";     // 박매니저 / 매니저
    public static final String USER_HOST = "f0000000-0000-0000-0000-000000000002";        // 김방장 / 방장
    public static final String USER_MIDDLE = "f0000000-0000-0000-0000-000000000003";      // 이중수 / 중수
    public static final String USER_BEGINNER = "f0000000-0000-0000-0000-000000000004";    // 최초보 / 초보
    public static final String USER_RISING = "f0000000-0000-0000-0000-000000000005";      // 정성장 / 실력상승
    public static final String USER_DECLINED = "f0000000-0000-0000-0000-000000000006";    // 한고수 / 실력하락

    // --- 픽스처 시설(매니저 1호점) ---
    public static final String FACILITY_BRANCH1 = "f0000000-0000-0000-0000-0000000000a1";

    // --- 픽스처 매칭 4개 ---
    public static final String MATCH_GANGNAM = "f0000000-0000-0000-0000-0000000000b1";     // 강남 퇴근후풋살
    public static final String MATCH_ALMOST_FULL = "f0000000-0000-0000-0000-0000000000b2"; // 마감임박 인기매칭
    public static final String MATCH_CLOSED = "f0000000-0000-0000-0000-0000000000b3";      // 모집마감된 매칭
    public static final String MATCH_FULL = "f0000000-0000-0000-0000-0000000000b4";        // 정원가득찬 매칭
}
