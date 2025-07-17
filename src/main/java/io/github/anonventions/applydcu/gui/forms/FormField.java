package io.github.anonventions.applydcu.gui.forms;

/**
 * Represents a form field in an application template
 */
public class FormField {
    private final String id;
    private final String question;
    private final FieldType type;
    private final boolean required;
    private final String validation;
    private final String placeholder;
    private final String[] options; // For choice fields
    
    public FormField(String id, String question, FieldType type, boolean required) {
        this(id, question, type, required, null, null, null);
    }
    
    public FormField(String id, String question, FieldType type, boolean required,
                    String validation, String placeholder, String[] options) {
        this.id = id;
        this.question = question;
        this.type = type;
        this.required = required;
        this.validation = validation;
        this.placeholder = placeholder;
        this.options = options;
    }
    
    // Getters
    public String getId() { return id; }
    public String getQuestion() { return question; }
    public FieldType getType() { return type; }
    public boolean isRequired() { return required; }
    public String getValidation() { return validation; }
    public String getPlaceholder() { return placeholder; }
    public String[] getOptions() { return options; }
    
    /**
     * Validates an answer against this field's requirements
     */
    public boolean validateAnswer(String answer) {
        // Check if required field is empty
        if (required && (answer == null || answer.trim().isEmpty())) {
            return false;
        }
        
        // If not required and empty, it's valid
        if (answer == null || answer.trim().isEmpty()) {
            return true;
        }
        
        // Type-specific validation
        switch (type) {
            case NUMBER:
                try {
                    Integer.parseInt(answer);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            
            case EMAIL:
                return answer.contains("@") && answer.contains(".");
            
            case CHOICE:
                if (options != null) {
                    for (String option : options) {
                        if (option.equals(answer)) {
                            return true;
                        }
                    }
                    return false;
                }
                break;
            
            case TEXT:
            case LONG_TEXT:
            default:
                break;
        }
        
        // Custom validation regex
        if (validation != null && !validation.isEmpty()) {
            return answer.matches(validation);
        }
        
        return true;
    }
    
    public enum FieldType {
        TEXT,
        LONG_TEXT,
        NUMBER,
        EMAIL,
        CHOICE
    }
}