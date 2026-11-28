# DEVELOPMENT SAMPLE. The authoritative policy lives in the automatiam-policies […]
# delivered to the sidecar as a signed bundle; this file exists so the server ca […]
# against a real OPA locally. MockToolPolicyClient mirrors it again in Java for […]
# without a sidecar - where the two drift, this file is right.
#
# Authorization keys off Okta group membership, matching the rest of the OneIAM […]
# matched by PREFIX, never by exact name: estate groups carry an environment suffix […]
# -UATROLE, -PRODROLE), and a rule written against one environment's spelling wo […]
# matching after promotion - failing open is impossible here, but failing closed […]
# its own incident.
package automatiam.tools

import rego.v1

policy_version := "sample-0"

full_admin_prefix := "ONEIAM-FULLADMIN"

readonly_admin_prefix := "ONEIAM-READONLYADMIN"

entitlement_gov_prefix := "ONEIAM-ENTITLEMENTGOV"

has_group_prefix(prefix) if {
    some group in input.groups
    startswith(group, prefix)
}

ownership_permitted if has_group_prefix(full_admin_prefix)

ownership_permitted if has_group_prefix(entitlement_gov_prefix)

identity_read_permitted if has_group_prefix(full_admin_prefix)

identity_read_permitted if has_group_prefix(readonly_admin_prefix)

# Tool-level authorization. The server treats a missing allow as unavailability, […]
# so every path below sets it explicitly.
#
# The version is a literal here, not 'policy_version': a default rule's value mu […]
# referencing a rule from one is a parse error rather than a runtime surprise. K […]
default authz := {
    "allow": false,
    "reason_code": "default_deny",
    "policy_version": "sample-0",
    "conditions": [],
}

# 1. Catalogue reads. Ownership is withheld unless the caller holds a governance […]
#    so the default answer discloses the least.
authz := {
    "allow": true,
    "reason_code": "catalogue_read",
    "policy_version": policy_version,
    "conditions": ownership_conditions,
} if {
    input.targetType in {"ENTITLEMENT", "APPLICATION", "SERVICE_IDENTITY", "POLICY"}
}

ownership_conditions := [] if ownership_permitted

ownership_conditions := ["withhold_ownership"] if not ownership_permitted

# 2. A caller reading their own access. The server computed selfService by compa […]
#    domain ID to the requested one; it is not a caller-supplied flag.
authz := {
    "allow": true,
    "reason_code": "self_service",
    "policy_version": policy_version,
    "conditions": [],
} if {
    input.targetType == "IDENTITY"
    input.selfService == true
}

# 3. Reading another employee's access. Needs an admin group, and the permit is […]
#    the invocation being audited.
authz := {
    "allow": true,
    "reason_code": "delegated_identity_read",
    "policy_version": policy_version,
    "conditions": ["audit_required"],
} if {
    input.targetType == "IDENTITY"
    input.selfService == false
    identity_read_permitted
}

# 4. Reading another employee's access without such a group.
authz := {
    "allow": false,
    "reason_code": "cross_identity_access_denied",
    "policy_version": policy_version,
    "conditions": [],
} if {
    input.targetType == "IDENTITY"
    input.selfService == false
    not identity_read_permitted
}

# ---------------------------------------------------------------------------
# Advisory simulation. Answers "what would policy decide"; grants nothing.
# An unrecognised policy identifier is INDETERMINATE rather than DENY: "no such […]
# "policy says no" are different answers, and a caller needs to be able to tell […]
# ---------------------------------------------------------------------------

approved_policies := {"application.ownership", "entitlement.eligibility"}

# Literal version, for the same reason as the authz default above.
default simulate := {
    "effect": "INDETERMINATE",
    "policy_version": "sample-0",
    "reason_codes": ["unknown_policy"],
    "conditions": [],
    "explanation": "No approved policy matches that identifier.",
}

simulate := {
    "effect": "DENY",
    "policy_version": policy_version,
    "reason_codes": ["no_subject_context"],
    "conditions": [],
    "explanation": "The simulation needs a subject, and the token carried none.",
} if {
    input.policyId in approved_policies
    not input.subjectId
}

simulate := {
    "effect": "ALLOW",
    "policy_version": policy_version,
    "reason_codes": ["eligibility_met"],
    "conditions": ["manager_approval_required"],
    "explanation": "Policy would permit the proposed action once the listed conditions are met.",
} if {
    input.policyId in approved_policies
    input.subjectId
}
