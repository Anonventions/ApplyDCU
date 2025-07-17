package io.github.anonventions.applydcu.core;

import io.github.anonventions.applydcu.api.ApplicationService;
import io.github.anonventions.applydcu.api.ConfigurationService;
import io.github.anonventions.applydcu.api.TemplateService;
import io.github.anonventions.applydcu.api.AnalyticsService;
import io.github.anonventions.applydcu.config.SimpleConfigurationService;
import io.github.anonventions.applydcu.config.YamlConfigurationService;
import io.github.anonventions.applydcu.services.SimpleApplicationService;
import io.github.anonventions.applydcu.services.SimpleTemplateService;
import io.github.anonventions.applydcu.analytics.SimpleAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the service registry and basic v2.0 service functionality
 */
class ServiceRegistryTest {
    
    @TempDir
    File tempDir;
    
    private ServiceRegistry serviceRegistry;
    
    @BeforeEach
    void setUp() {
        serviceRegistry = ServiceRegistry.getInstance();
        serviceRegistry.clear(); // Clear any existing services
    }
    
    @Test
    void testServiceRegistration() {
        // Test service registration
        ConfigurationService configService = new SimpleConfigurationService(tempDir);
        serviceRegistry.registerService(ConfigurationService.class, configService);
        
        assertTrue(serviceRegistry.hasService(ConfigurationService.class));
        assertSame(configService, serviceRegistry.getService(ConfigurationService.class));
    }
    
    @Test
    void testFactoryRegistration() {
        // Test factory registration
        serviceRegistry.registerFactory(ApplicationService.class, 
            () -> new SimpleApplicationService(tempDir));
        
        assertTrue(serviceRegistry.hasService(ApplicationService.class));
        
        // Get service twice to ensure it's cached
        ApplicationService service1 = serviceRegistry.getService(ApplicationService.class);
        ApplicationService service2 = serviceRegistry.getService(ApplicationService.class);
        
        assertNotNull(service1);
        assertSame(service1, service2); // Should be the same instance
    }
    
    @Test
    void testServiceNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            serviceRegistry.getService(ApplicationService.class);
        });
    }
    
    @Test
    void testConfigurationServiceBasics() {
        ConfigurationService configService = new SimpleConfigurationService(tempDir);
        
        // Test basic methods don't throw exceptions
        assertDoesNotThrow(() -> {
            String[] roles = configService.getAvailableRoles();
            assertNotNull(roles);
            
            boolean valid = configService.isValidRole("mod");
            assertTrue(valid);
            
            String permission = configService.getRolePermission("mod");
            assertNotNull(permission);
        });
    }
    
    @Test
    void testYamlConfigurationService() {
        ConfigurationService yamlService = new YamlConfigurationService(tempDir);
        
        assertDoesNotThrow(() -> {
            String[] roles = yamlService.getAvailableRoles();
            assertNotNull(roles);
            assertTrue(roles.length > 0);
        });
    }
    
    @Test
    void testApplicationServiceBasics() {
        ApplicationService appService = new SimpleApplicationService(tempDir);
        
        // Test basic methods don't throw exceptions
        assertDoesNotThrow(() -> {
            appService.getPendingApplications().join();
        });
    }
    
    @Test
    void testTemplateService() {
        TemplateService templateService = new SimpleTemplateService();
        
        assertDoesNotThrow(() -> {
            var templates = templateService.getAvailableTemplates().join();
            assertNotNull(templates);
            assertTrue(templates.length > 0);
            
            var modTemplate = templateService.getTemplateForRole("mod").join();
            assertNotNull(modTemplate);
            assertEquals("mod", modTemplate.getId());
        });
    }
    
    @Test
    void testAnalyticsService() {
        AnalyticsService analyticsService = new SimpleAnalyticsService(tempDir);
        
        assertDoesNotThrow(() -> {
            analyticsService.recordApplicationSubmission("mod");
            analyticsService.recordApplicationAcceptance("mod");
            
            var stats = analyticsService.getApplicationStats().join();
            assertNotNull(stats);
            assertTrue(stats.getTotalSubmissions() > 0);
            
            var roleStats = analyticsService.getRoleStats().join();
            assertNotNull(roleStats);
            assertTrue(roleStats.containsKey("mod"));
        });
    }
    
    @Test
    void testFullServiceIntegration() {
        // Register all services
        serviceRegistry.registerService(ConfigurationService.class, new SimpleConfigurationService(tempDir));
        serviceRegistry.registerService(ApplicationService.class, new SimpleApplicationService(tempDir));
        serviceRegistry.registerService(TemplateService.class, new SimpleTemplateService());
        serviceRegistry.registerService(AnalyticsService.class, new SimpleAnalyticsService(tempDir));
        
        // Verify all services are available
        assertTrue(serviceRegistry.hasService(ConfigurationService.class));
        assertTrue(serviceRegistry.hasService(ApplicationService.class));
        assertTrue(serviceRegistry.hasService(TemplateService.class));
        assertTrue(serviceRegistry.hasService(AnalyticsService.class));
        
        // Test service interaction
        ConfigurationService config = serviceRegistry.getService(ConfigurationService.class);
        ApplicationService apps = serviceRegistry.getService(ApplicationService.class);
        AnalyticsService analytics = serviceRegistry.getService(AnalyticsService.class);
        
        String[] roles = config.getAvailableRoles();
        assertTrue(roles.length > 0);
        
        // Simulate an application workflow
        analytics.recordApplicationSubmission("mod");
        analytics.recordApplicationAcceptance("mod");
        
        var stats = analytics.getApplicationStats().join();
        assertTrue(stats.getTotalSubmissions() > 0);
    }
}