package com.elevance.automatiam.mcp.model;

import java.util.Map;
import java.util.Optional;

/**
 * Approved, non-sensitive metadata for a service identity.
 *
 * <p>This type has no field for a password, secret, token, certificate or priva […]
 * gain one. The omission is the control: a caller cannot be handed a credential […]
 * cannot express.
 */
public record ServiceIdentityMetadata(
        String serviceId,
        String serviceName,
        String applicationId,
        String applicationName,
        ServiceIdentityStatus status,
        Optional<Ownership> ownership,
        Map<String, String> attributes) {

    public ServiceIdentityMetadata {
        ownership = ownership == null ? Optional.empty() : ownership;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
