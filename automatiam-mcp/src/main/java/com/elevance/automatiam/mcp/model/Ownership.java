package com.elevance.automatiam.mcp.model;

import java.util.Optional;

/**
 * Ownership metadata, returned only where the caller is authorized to see it. A […]
 * blanked when policy withholds it, so a consumer cannot mistake redaction for […]
 *
 * @para  ownerId          authoritative owner identifier
 * @param ownerDisplayName human-readable owner name
 * @param ownerGroup       owning group or team, where the source records one
 */
public record Ownership(String ownerId, String ownerDisplayName, Optional<String> ownerGroup) {

    public Ownership {
        ownerGroup = ownerGroup == null ? Optional.empty() : ownerGroup;
    }
}
