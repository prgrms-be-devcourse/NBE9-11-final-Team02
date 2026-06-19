package com.back.sportteam.infra.redis.queue;

public final class WaitingQueueKeys {

    private static final String PREFIX = "reservation:waiting";

    private WaitingQueueKeys() {
    }

    public static String queue(String facilitySlotId) {
        return PREFIX + ":slot:" + facilitySlotId;
    }

    public static String token(String token) {
        return PREFIX + ":token:" + token;
    }

    public static String userToken(String facilitySlotId, String userId) {
        return PREFIX + ":slot:" + facilitySlotId + ":user:" + userId;
    }
}
