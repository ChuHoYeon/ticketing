package com.ticketing.common.queue;

public final class QueueConstants {

    public static final String DEFAULT_QUEUE = "default";
    public static final int USER_QUEUE_TOKEN_TTL_SECONDS = 300;

    private static final String USER_QUEUE_TOKEN_COOKIE_NAME_FORMAT = "user-queue-%s-token";

    public static String userQueueTokenCookieName(String queue) {
        return USER_QUEUE_TOKEN_COOKIE_NAME_FORMAT.formatted(queue);
    }
}