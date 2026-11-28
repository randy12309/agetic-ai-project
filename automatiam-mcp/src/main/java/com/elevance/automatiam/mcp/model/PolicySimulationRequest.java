package com.elevance.automatiam.mcp.model;

import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;

import java.util.Map;
import java.util.Optional;

/**
 * A proposed IAM decision to be evaluated without performing it.
 *
 * @param policyId    an approved policy identifier. Rego source is not accepted […]
 *                    no field that could carry it
 * @param action      the proposed action, from the approved set
 * @param resourceType kind of resource the action targets
 * @param resourceId  the resource the action targets
 * @param subjectId   the domain ID of the identity the action concerns, taken f […]
 *                    token, not from a tool argument
 * @param attributes  approved evaluation attributes. Filtered to the permitted […]
 *                    policy sees them, so a caller cannot smuggle in a role or […]
 */
public record PolicySimulationRequest(
        String policyId,
        String action,
        TargetResourceType resourceType,
        String resourceId,
        Optional<String> subjectId,
        Map<String, String> attributes) {

    public PolicySimulationRequest {
        if (policyId == null || policyId.isBlank()) {
            throw new InvalidToolArgumentException("policyId", "must not be blank");
        }
        if (action == null || action.isBlank()) {
            throw new InvalidToolArgumentException("action", "must not be blank");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new InvalidToolArgumentException("resourceId", "must not be blank");
        }
        subjectId = subjectId == null ? Optional.empty() : subjectId;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
