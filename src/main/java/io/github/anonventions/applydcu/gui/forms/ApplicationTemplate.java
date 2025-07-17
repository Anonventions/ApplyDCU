package io.github.anonventions.applydcu.gui.forms;

import java.util.List;

/**
 * Represents an application form template
 */
public class ApplicationTemplate {
    private final String id;
    private final String name;
    private final String description;
    private final List<FormField> fields;
    private final String permission;
    private final boolean enabled;
    
    public ApplicationTemplate(String id, String name, String description, 
                             List<FormField> fields, String permission, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.fields = fields;
        this.permission = permission;
        this.enabled = enabled;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<FormField> getFields() { return fields; }
    public String getPermission() { return permission; }
    public boolean isEnabled() { return enabled; }
    
    /**
     * Validates a set of answers against this template
     */
    public boolean validateAnswers(List<String> answers) {
        if (answers.size() != fields.size()) {
            return false;
        }
        
        for (int i = 0; i < fields.size(); i++) {
            FormField field = fields.get(i);
            String answer = answers.get(i);
            
            if (!field.validateAnswer(answer)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get questions as string array for legacy compatibility
     */
    public String[] getQuestions() {
        return fields.stream()
                .map(FormField::getQuestion)
                .toArray(String[]::new);
    }
}