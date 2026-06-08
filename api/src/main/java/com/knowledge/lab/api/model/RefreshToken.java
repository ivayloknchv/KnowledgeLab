package com.knowledge.lab.api.model;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Stored in Redis as:  refresh_token:{tokenId}     =>  this object
 * Also indexed by:     refresh_token:user:{userId} =>  Set<tokenId>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken implements Serializable {

    private String id;
    private String userId;
    private String email;
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
