package com.elevance.automatiam.mcp.config;

import com.elevance.automatiam.mcp.clients.oneiam.ApplicationLookupClient;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementDetailClient;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementMetadataClient;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementSearchClient;
import com.elevance.automatiam.mcp.clients.oneiam.IdentityAccessClient;
import com.elevance.automatiam.mcp.clients.oneiam.ManagerMetricsClient;
import com.elevance.automatiam.mcp.clients.oneiam.ManagerOrganizationClient;
import com.elevance.automatiam.mcp.clients.oneiam.ServiceIdentityClient;
import com.elevance.automatiam.mcp.tools.ToolExecutor;
import com.elevance.automatiam.mcp.tools.application.LookupApplicationTool;
import com.elevance.automatiam.mcp.tools.entitlement.GetEntitlementDetailTool;
import com.elevance.automatiam.mcp.tools.entitlement.GetEntitlementMetadataTool;
import com.elevance.automatiam.mcp.tools.entitlement.GetManagerMetricsTool;
import com.elevance.automatiam.mcp.tools.entitlement.SearchEntitlementsTool;
import com.elevance.automatiam.mcp.tools.identity.GetIdentityAccessTool;
import com.elevance.automatiam.mcp.tools.serviceid.GetServiceIdMetadataTool;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/** Publish each OneIAM tool when all of its upstream adapters are available. */
@AutoConfiguration
public class OneIamToolsAutoConfiguration {

    @Bean
    @ConditionalOnBean(ApplicationLookupClient.class)
    LookupApplicationTool lookupApplicationTool(
            ToolExecutor executor, ApplicationLookupClient applicationLookupClient) {
        return new LookupApplicationTool(executor, applicationLookupClient);
    }

    @Bean
    @ConditionalOnBean(EntitlementDetailClient.class)
    GetEntitlementDetailTool getEntitlementDetailTool(
            ToolExecutor executor, EntitlementDetailClient entitlementDetailClient) {
        return new GetEntitlementDetailTool(executor, entitlementDetailClient);
    }

    @Bean
    @ConditionalOnBean(EntitlementMetadataClient.class)
    GetEntitlementMetadataTool getEntitlementMetadataTool(
            ToolExecutor executor, EntitlementMetadataClient entitlementMetadataClient) {
        return new GetEntitlementMetadataTool(executor, entitlementMetadataClient);
    }

    @Bean
    @ConditionalOnBean({ManagerMetricsClient.class, ManagerOrganizationClient.class})
    GetManagerMetricsTool getManagerMetricsTool(
            ToolExecutor executor, ManagerMetricsClient managerMetricsClient,
            ManagerOrganizationClient managerOrganizationClient) {
        return new GetManagerMetricsTool(executor, managerMetricsClient, managerOrganizationClient);
    }

    @Bean
    @ConditionalOnBean(EntitlementSearchClient.class)
    SearchEntitlementsTool searchEntitlementsTool(
            ToolExecutor executor, EntitlementSearchClient entitlementSearchClient) {
        return new SearchEntitlementsTool(executor, entitlementSearchClient);
    }

    @Bean
    @ConditionalOnBean(IdentityAccessClient.class)
    GetIdentityAccessTool getIdentityAccessTool(
            ToolExecutor executor, IdentityAccessClient identityAccessClient) {
        return new GetIdentityAccessTool(executor, identityAccessClient);
    }

    @Bean
    @ConditionalOnBean(ServiceIdentityClient.class)
    GetServiceIdMetadataTool getServiceIdMetadataTool(
            ToolExecutor executor, ServiceIdentityClient serviceIdentityClient) {
        return new GetServiceIdMetadataTool(executor, serviceIdentityClient);
    }
}
