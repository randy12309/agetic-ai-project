package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ApplicationStatus;
import com.elevance.automatiam.mcp.model.DataClassification;
import com.elevance.automatiam.mcp.model.EntitlementDetail;
import com.elevance.automatiam.mcp.model.EntitlementOwnership;
import com.elevance.automatiam.mcp.model.ManagerMetrics;
import com.elevance.automatiam.mcp.model.ApplicationSummary;
import com.elevance.automatiam.mcp.model.AssignmentStatus;
import com.elevance.automatiam.mcp.model.EntitlementAssignment;
import com.elevance.automatiam.mcp.model.EntitlementMetadata;
import com.elevance.automatiam.mcp.model.EntitlementStatus;
import com.elevance.automatiam.mcp.model.EntitlementSummary;
import com.elevance.automatiam.mcp.model.Ownership;
import com.elevance.automatiam.mcp.model.ServiceIdentityMetadata;
import com.elevance.automatiam.mcp.model.ServiceIdentityStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fabricated IAM data for local development and tests.
 *
 * <p>Every identifier here is invented. Nothing in this file corresponds to a r […]
 * application, entitlement or service identity, and nothing in it should be cop […]
 * that runs against a real environment.
 */
final class SyntheticIamData {

    // Domain-ID shaped (two letters, five digits), matching the Elevance Health […]
    // format the identity capabilities expect. Both are invented.
    static final String SELF_DOMAIN_ID = "AB12345";
    static final String OTHER_DOMAIN_ID = "CD67890";

    private SyntheticIamData() {
    }

    static final List<ApplicationSummary> APPLICATIONS = List.of(
            new ApplicationSummary("APP-1001", "Claims Workbench", ApplicationStatus.ACTIVE,
                    Optional.of(new Ownership("EF11111", "Dana Okafor", Optional.empty())),
                    Map.of("tier", "2", "dataClassification", "internal")),
            new ApplicationSummary("APP-1002", "Provider Directory", ApplicationStatus.ACTIVE,
                    Optional.of(new Ownership("GH22222", "Sam Beckett", Optional.empty())),
                    Map.of("tier", "1", "dataClassification", "confidential")),
            new ApplicationSummary("APP-1003", "Legacy Billing", ApplicationStatus.INACTIVE,
                    Optional.of(new Ownership("JK33333", "Rae Lindqvist", Optional.empty())),
                    Map.of("tier", "3", "dataClassification", "internal")));

    static final List<EntitlementMetadata> ENTITLEMENTS = List.of(
            entitlement("ENT-2001", "claims.reader", "Read-only access to claims records",
                    "APP-1001", "Claims Workbench", EntitlementStatus.ACTIVE, "EF11111", "Dana Okafor"),
            entitlement("ENT-2002", "claims.adjuster", "Adjust and resubmit claims",
                    "APP-1001", "Claims Workbench", EntitlementStatus.ACTIVE, "EF11111", "Dana Okafor"),
            entitlement("ENT-2003", "provider.reader", "Read provider directory records",
                    "APP-1002", "Provider Directory", EntitlementStatus.ACTIVE, "GH22222", "Sam Beckett"),
            entitlement("ENT-2004", "provider.steward", "Maintain provider directory records",
                    "APP-1002", "Provider Directory", EntitlementStatus.INACTIVE, "GH22222", "Sam Beckett"),
            entitlement("ENT-2005", "billing.legacy.reader", "Read legacy billing extracts",
                    "APP-1003", "Legacy Billing", EntitlementStatus.INACTIVE, "JK33333", "Rae Lindqvist"));

    static final List<ServiceIdentityMetadata> SERVICE_IDENTITIES = List.of(
            new ServiceIdentityMetadata("SVC-3001", "claims-batch-runner", "APP-1001",
                    "Claims Workbench", ServiceIdentityStatus.ACTIVE,
                    Optional.of(new Ownership("EF11111", "Dana Okafor", Optional.empty())),
                    Map.of("environment", "prod", "rotationPolicy", "90d")),
            new ServiceIdentityMetadata("SVC-3002", "provider-sync", "APP-1002",
                    "Provider Directory", ServiceIdentityStatus.INACTIVE,
                    Optional.of(new Ownership("GH22222", "Sam Beckett", Optional.empty())),
                    Map.of("environment", "test", "rotationPolicy", "30d")));

    static final Map<String, List<EntitlementAssignment>> ASSIGNMENTS = Map.of(
            SELF_DOMAIN_ID, List.of(
                    assignment("ENT-2001", "claims.reader", "APP-1001", "Claims Workbench", AssignmentStatus.ACTIVE),
                    assignment("ENT-2003", "provider.reader", "APP-1002", "Provider Directory", AssignmentStatus.ACTIVE),
                    assignment("ENT-2005", "billing.legacy.reader", "APP-1003", "Legacy Billing", AssignmentStatus.ACTIVE)),
            OTHER_DOMAIN_ID, List.of(
                    assignment("ENT-2002", "claims.adjuster", "APP-1001", "Claims Workbench", AssignmentStatus.ACTIVE)));

    private static EntitlementMetadata entitlement(
            String id, String name, String description, String applicationId, String applicationName,
            EntitlementStatus status, String ownerId, String ownerName) {
        return new EntitlementMetadata(id, name, Optional.of(description), applicationId, applicationName,
                status, Optional.of(new Ownership(ownerId, ownerName, Optional.empty())),
                Map.of("riskRating", status == EntitlementStatus.INACTIVE ? "high" : "low"));
    }

    private static EntitlementAssignment assignment(
            String id, String name, String applicationId, String applicationName, AssignmentStatus status) {
        return new EntitlementAssignment(id, name, applicationId, applicationName, status,
                Optional.of(Instant.parse("2025-04-01T09:00:00Z")));
    }

    /** Detail records keyed by entitlement id, shaped like EM's detail screen. […] */
    static final List<EntitlementDetail> DETAILS = List.of(
            detail("ENT-2001", "claims.reader", "Read-only access to claims records",
                    "Claims Workbench APM001", "APM001", EntitlementStatus.ACTIVE, false,
                    Set.of(DataClassification.PHI), 412L, "US-only", "Claims Operations",
                    "EF11111", "Dana Okafor"),
            detail("ENT-2002", "claims.adjuster", "Adjust and resubmit claims",
                    "Claims Workbench APM001", "APM001", EntitlementStatus.ACTIVE, true,
                    Set.of(DataClassification.PHI, DataClassification.PFI), 37L, null,
                    "Claims Adjusters", "EF11111", "Dana Okafor"),
            detail("ENT-2003", "provider.reader", "Read provider directory records",
                    "Provider Directory APM002", "APM002", EntitlementStatus.ACTIVE, false,
                    Set.of(DataClassification.PII), 1_204L, null, "Provider Data",
                    "GH22222", "Sam Beckett"),
            detail("ENT-2004", "provider.steward", "Maintain provider directory records",
                    "Provider Directory APM002", "APM002", EntitlementStatus.INACTIVE, true,
                    Set.of(DataClassification.PII, DataClassification.CPI), 9L, null,
                    "Provider Stewards", "GH22222", "Sam Beckett"),
            // No APM number: this is the shape EM's missingApm metric counts, a […]
            // assistant is expected to explain.
            detail("ENT-2005", "billing.legacy.reader", "Read legacy billing extracts",
                    "Legacy Billing", null, EntitlementStatus.INACTIVE, false,
                    Set.of(), 3L, null, null, "JK33333", "Rae Lindqvist"));

    static final ManagerMetrics MANAGER_METRICS =
            new ManagerMetrics(SELF_DOMAIN_ID, 47L, 6L, 2L, 1L);

    private static EntitlementDetail detail(
            String id, String displayName, String description, String applicationName,
            String apmNumber, EntitlementStatus status, boolean elevatedAccess,
            Set<DataClassification> classifications, Long memberCount, String locationRestrictions,
            String targetedUserGroup, String ownerId, String ownerName) {
        return new EntitlementDetail(
                id, displayName, Optional.of(description), applicationName,
                Optional.ofNullable(apmNumber),
                Optional.of("SailPoint"), Optional.of("prod"),
                status, elevatedAccess, classifications,
                Optional.ofNullable(memberCount),
                EntitlementDetail.normalizeUpstream(locationRestrictions),
                EntitlementDetail.normalizeUpstream(targetedUserGroup),
                Optional.of(new EntitlementOwnership(
                        Optional.of(new Ownership(ownerId, ownerName, Optional.empty())),
                        Optional.of(new Ownership(ownerId, ownerName, Optional.empty())),
                        Optional.empty())));
    }

    static EntitlementSummary toSummary(EntitlementMetadata metadata) {
        return new EntitlementSummary(metadata.entitlementId(), metadata.entitlementName(),
                metadata.applicationId(), metadata.applicationName(), metadata.status(),
                metadata.description());
    }
}
