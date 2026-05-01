package com.ticketing.common.queue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class QueueTokenUtils {

    public static String generateSHA256Token(String queue, Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = "user-queue-%s-%d".formatted(queue, userId);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte value : encodedHash) {
                hexString.append(String.format("%02x", value));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}