package com.back.sportteam.domain.settlement.policy;

import java.math.BigDecimal;

/**
 * 정산 정책 조회 추상화.
 * 현재는 설정 파일 기반(ConfigBasedSettlementPolicy)이지만, 추후 운영자가 직접 변경하는
 * 요구사항이 생기면 DB 기반 구현체로 교체할 수 있도록 서비스가 인터페이스에만 의존하게 한다.
 */
public interface SettlementPolicy {

    BigDecimal getPlatformFeeRate();
}
