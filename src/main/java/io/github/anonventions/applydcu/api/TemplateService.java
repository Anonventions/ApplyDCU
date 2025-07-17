package io.github.anonventions.applydcu.api;

import io.github.anonventions.applydcu.gui.forms.ApplicationTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * Service for managing application templates
 */
public interface TemplateService {
    
    /**
     * Get template by ID
     */
    CompletableFuture<ApplicationTemplate> getTemplate(String templateId);
    
    /**
     * Get all available templates
     */
    CompletableFuture<ApplicationTemplate[]> getAvailableTemplates();
    
    /**
     * Create a new template
     */
    CompletableFuture<Boolean> createTemplate(ApplicationTemplate template);
    
    /**
     * Update an existing template
     */
    CompletableFuture<Boolean> updateTemplate(ApplicationTemplate template);
    
    /**
     * Delete a template
     */
    CompletableFuture<Boolean> deleteTemplate(String templateId);
    
    /**
     * Get template for a specific role (for legacy compatibility)
     */
    CompletableFuture<ApplicationTemplate> getTemplateForRole(String role);
}