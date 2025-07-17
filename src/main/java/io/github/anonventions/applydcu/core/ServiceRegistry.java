package io.github.anonventions.applydcu.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Simple dependency injection container for ApplyDCU v2.0
 */
public class ServiceRegistry {
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();
    
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();
    
    private ServiceRegistry() {}
    
    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a service instance
     */
    public <T> void registerService(Class<T> serviceClass, T instance) {
        services.put(serviceClass, instance);
    }
    
    /**
     * Register a service factory
     */
    public <T> void registerFactory(Class<T> serviceClass, Supplier<T> factory) {
        factories.put(serviceClass, factory);
    }
    
    /**
     * Get a service instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        Object service = services.get(serviceClass);
        if (service != null) {
            return (T) service;
        }
        
        Supplier<?> factory = factories.get(serviceClass);
        if (factory != null) {
            T instance = (T) factory.get();
            services.put(serviceClass, instance);
            return instance;
        }
        
        throw new IllegalArgumentException("Service not registered: " + serviceClass.getSimpleName());
    }
    
    /**
     * Check if a service is registered
     */
    public boolean hasService(Class<?> serviceClass) {
        return services.containsKey(serviceClass) || factories.containsKey(serviceClass);
    }
    
    /**
     * Clear all services (for testing)
     */
    public void clear() {
        services.clear();
        factories.clear();
    }
}