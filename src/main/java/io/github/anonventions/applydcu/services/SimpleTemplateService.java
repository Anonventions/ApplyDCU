package io.github.anonventions.applydcu.services;

import io.github.anonventions.applydcu.api.TemplateService;
import io.github.anonventions.applydcu.gui.forms.ApplicationTemplate;
import io.github.anonventions.applydcu.gui.forms.FormField;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simple template service implementation
 */
public class SimpleTemplateService implements TemplateService {
    
    private final ApplicationTemplate[] defaultTemplates;
    
    public SimpleTemplateService() {
        this.defaultTemplates = createDefaultTemplates();
    }
    
    @Override
    public CompletableFuture<ApplicationTemplate> getTemplate(String templateId) {
        return CompletableFuture.supplyAsync(() -> {
            for (ApplicationTemplate template : defaultTemplates) {
                if (template.getId().equals(templateId)) {
                    return template;
                }
            }
            return null;
        });
    }
    
    @Override
    public CompletableFuture<ApplicationTemplate[]> getAvailableTemplates() {
        return CompletableFuture.completedFuture(defaultTemplates);
    }
    
    @Override
    public CompletableFuture<Boolean> createTemplate(ApplicationTemplate template) {
        // For now, return false as we're using static templates
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public CompletableFuture<Boolean> updateTemplate(ApplicationTemplate template) {
        // For now, return false as we're using static templates
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public CompletableFuture<Boolean> deleteTemplate(String templateId) {
        // For now, return false as we're using static templates
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public CompletableFuture<ApplicationTemplate> getTemplateForRole(String role) {
        return getTemplate(role);
    }
    
    private ApplicationTemplate[] createDefaultTemplates() {
        return new ApplicationTemplate[] {
            new ApplicationTemplate(
                "mod",
                "Moderator Application",
                "Apply to become a server moderator",
                List.of(
                    new FormField("nickname", "&eIn-game nickname? &7[Bot-Proof]", FormField.FieldType.TEXT, true),
                    new FormField("age", "&eHow old are you?", FormField.FieldType.NUMBER, true),
                    new FormField("timezone", "&eWhat is your timezone?", FormField.FieldType.TEXT, true),
                    new FormField("experience", "&eWhat would you rate your plugin knowledge as?", FormField.FieldType.CHOICE, true, 
                        null, null, new String[]{"Beginner", "Intermediate", "Advanced", "Expert"}),
                    new FormField("motivation", "&eWhy do you want to be a mod?", FormField.FieldType.LONG_TEXT, true),
                    new FormField("commitment", "&eHow many hours can you commit weekly?", FormField.FieldType.NUMBER, true)
                ),
                "group.mod",
                true
            ),
            
            new ApplicationTemplate(
                "builder",
                "Builder Application", 
                "Apply to become a server builder",
                List.of(
                    new FormField("nickname", "&eIn-game nickname? &7[Bot-Proof]", FormField.FieldType.TEXT, true),
                    new FormField("experience", "&eWhat is your building experience?", FormField.FieldType.LONG_TEXT, true),
                    new FormField("tools", "&eAre you familiar with WorldEdit and VoxelSniper?", FormField.FieldType.CHOICE, true,
                        null, null, new String[]{"Yes, both", "WorldEdit only", "VoxelSniper only", "Neither"}),
                    new FormField("commitment", "&eHow many hours can you commit weekly?", FormField.FieldType.NUMBER, true),
                    new FormField("previous", "&eHave you been a builder previously in any other servers?", FormField.FieldType.TEXT, false),
                    new FormField("portfolio", "&eCan you provide screenshots of your builds? &7[Send IMGUR links]", FormField.FieldType.TEXT, false)
                ),
                "group.builder",
                true
            ),
            
            new ApplicationTemplate(
                "lore",
                "Lore Team Application",
                "Apply to join the lore writing team",
                List.of(
                    new FormField("nickname", "&eIn-game nickname? &7[Bot-Proof]", FormField.FieldType.TEXT, true),
                    new FormField("interest", "&eWhy are you interested in lore?", FormField.FieldType.LONG_TEXT, true),
                    new FormField("favorite", "&eWhat is your favorite comic-book in DC Comics?", FormField.FieldType.TEXT, true),
                    new FormField("knowledge", "&eWhat would you rate your knowledge on DC Comics Characters as?", FormField.FieldType.CHOICE, true,
                        null, null, new String[]{"Beginner", "Intermediate", "Advanced", "Expert"}),
                    new FormField("characters", "&eAny specific Characters you're very familiar with?", FormField.FieldType.TEXT, false),
                    new FormField("writing", "&eHow well can you write for events?", FormField.FieldType.CHOICE, true,
                        null, null, new String[]{"Very well", "Somewhat well", "Need practice", "Beginner"}),
                    new FormField("activity", "&eHow active will you be?", FormField.FieldType.TEXT, true),
                    new FormField("timezone", "&eWhat is your timezone?", FormField.FieldType.TEXT, true)
                ),
                "group.lore",
                true
            )
        };
    }
}