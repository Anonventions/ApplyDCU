package io.github.anonventions.applydcu.core;

import io.github.anonventions.applydcu.api.ApplicationService;
import io.github.anonventions.applydcu.api.ConfigurationService;
import io.github.anonventions.applydcu.config.EnhancedConfigurationService;
import io.github.anonventions.applydcu.services.EnhancedApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the service registry and basic service functionality
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
        ConfigurationService configService = new EnhancedConfigurationService(tempDir);
        serviceRegistry.registerService(ConfigurationService.class, configService);
        
        assertTrue(serviceRegistry.hasService(ConfigurationService.class));
        assertSame(configService, serviceRegistry.getService(ConfigurationService.class));
    }
    
    @Test
    void testFactoryRegistration() {
        // Test factory registration
        serviceRegistry.registerFactory(ApplicationService.class, 
            () -> new EnhancedApplicationService(tempDir));
        
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
        ConfigurationService configService = new EnhancedConfigurationService(tempDir);
        
        // Test basic methods don't throw exceptions
        assertDoesNotThrow(() -> {
            configService.getAvailableRoles();
            configService.isValidRole("test");
            configService.getRolePermission("test");
        });
    }
    
    @Test
    void testApplicationServiceBasics() {
        ApplicationService appService = new EnhancedApplicationService(tempDir);
        
        // Test basic methods don't throw exceptions
        assertDoesNotThrow(() -> {
            appService.getPendingApplications().join();
        });
    }
}