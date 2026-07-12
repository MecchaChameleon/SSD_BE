package com.jejulocaltime.api.common.util;

import java.math.BigDecimal;

/**
 * DECIMAL(고정소수점) DB 컬럼과 API DTO의 Double 사이를 오가는 변환.
 * latitude/longitude처럼 엔티티는 BigDecimal, DTO는 Double을 쓰는 필드에서 공통으로 사용한다.
 */
public final class NumberConversions {

    private NumberConversions() {
    }

    public static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    public static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
