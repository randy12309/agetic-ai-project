package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ManagerOrganization;

import reactor.core.publisher.Mono;

/**
 * Retrieves a manager's organization from the identity management service.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * {@code POST /api/v1/identity/{id}/organization), which takes an {@code Identi […]
 * and confirmation of the {@code managerStatus) encoding - it is a {@code Boole […]
 * {@code IdentityEntity} but a {@code String} on {@code IdentityDto] via {@code […]
 * so the wire value needs checking rather than assuming "true"/"false".
 *
 * <p>Implementations return a count, never the roster. The endpoint returns the […]
 * list; discarding it here is deliberate, not an oversight.
 */
public interface ManagerOrganizationClient {

    Mono<ManagerOrganization> organizationFor(String managerDomainId);
}
