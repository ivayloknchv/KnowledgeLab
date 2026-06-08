package com.knowledge.lab.api.service;

import com.knowledge.lab.api.config.JWTConfig;
import com.knowledge.lab.api.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Manages refresh tokens entirely in Redis.
 *
 * Key layout:
 *   refresh_token:{tokenId}  =>  RefreshToken object  (TTL = token lifetime)
 *   user_tokens:{userId}     =>  Set<tokenId>          (TTL = max token lifetime)
 *
 * The user_tokens set members are plain token-id strings so we can remove
 * individual members without needing the full object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String TOKEN_PREFIX       = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    // Separate template keyed on String so the user-set stores plain IDs
    private final RedisTemplate<String, Object> redisTemplate;
    private final JWTConfig                     props;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public RefreshToken create(String userId, String email) {
        var token = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .expiresAt(Instant.now().plusMillis(props.getRefreshTokenExpiryMs()))
                .build();

        Duration ttl = Duration.ofMillis(props.getRefreshTokenExpiryMs());

        // Store the full token object
        redisTemplate.opsForValue().set(tokenKey(token.getId()), token, ttl);

        // FIX: add the tokenId string as the SET member (was missing the value argument)
        String userSetKey = userTokensKey(userId);
        redisTemplate.opsForSet().add(userSetKey, token.getId());
        // Keep the user-set alive at least as long as the newest token
        redisTemplate.expire(userSetKey, ttl);

        log.debug("Created refresh token {} for user {}", token.getId(), userId);
        return token;
    }

    public RefreshToken findById(String tokenId) {
        Object raw = redisTemplate.opsForValue().get(tokenKey(tokenId));

        if (raw == null) {
            throw new IllegalArgumentException("Refresh token not found or expired");
        }

        RefreshToken token = (RefreshToken) raw;

        // Double-check wall-clock expiry (Redis TTL is authoritative, but be safe)
        if (token.isExpired()) {
            deleteFromIndex(tokenId, token.getUserId());
            redisTemplate.delete(tokenKey(tokenId));
            throw new IllegalArgumentException("Refresh token has expired");
        }

        return token;
    }

    /**
     * Rotation strategy: invalidate the old token and issue a fresh one.
     * Prevents replay attacks if a stolen token is submitted twice.
     */
    public RefreshToken rotate(String oldTokenId) {
        RefreshToken old = findById(oldTokenId);
        delete(oldTokenId, old.getUserId());
        return create(old.getUserId(), old.getEmail());
    }

    public void revokeToken(String tokenId) {
        Object raw = redisTemplate.opsForValue().get(tokenKey(tokenId));
        if (raw instanceof RefreshToken token) {
            delete(tokenId, token.getUserId());
            log.debug("Revoked refresh token {}", tokenId);
        }
    }

    public void revokeAllForUser(String userId) {
        Set<Object> members = redisTemplate.opsForSet().members(userTokensKey(userId));
        if (members != null) {
            members.forEach(id -> redisTemplate.delete(tokenKey(id.toString())));
        }
        redisTemplate.delete(userTokensKey(userId));
        log.info("Revoked all refresh tokens for user {}", userId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void delete(String tokenId, String userId) {
        redisTemplate.delete(tokenKey(tokenId));
        deleteFromIndex(tokenId, userId);
    }

    private void deleteFromIndex(String tokenId, String userId) {
        redisTemplate.opsForSet().remove(userTokensKey(userId), tokenId);
    }

    private String tokenKey(String tokenId) { return TOKEN_PREFIX + tokenId; }
    private String userTokensKey(String uid) { return USER_TOKENS_PREFIX + uid; }
}
