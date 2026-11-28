package com.elevance.automatiam.mcp;

import com.elevance.automatiam.mcp.config.AuthenticationProperties;
import com.elevance.automatiam.mcp.config.MockProperties;
import com.elevance.automatiam.mcp.config.OneIamProperties;
import com.elevance.automatiam.mcp.config.OpaProperties;
import com.elevance.automatiam.mcp.config.ToolProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AuthenticationProperties.class,
        OpaProperties.class,
        OneIamProperties.class,
        MockProperties.class,
        ToolProperties.class})
public class AutomatIamMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomatIamMcpApplication.class, args);
    }
}
