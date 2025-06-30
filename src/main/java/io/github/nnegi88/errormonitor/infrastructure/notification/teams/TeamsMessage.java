package io.github.nnegi88.errormonitor.infrastructure.notification.teams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Microsoft Teams message model using Adaptive Cards format.
 * Designed specifically for Teams webhook payloads.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamsMessage {
    
    @JsonProperty("@type")
    private String type = "MessageCard";
    
    @JsonProperty("@context")
    private String context = "https://schema.org/extensions";
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("themeColor")
    private String themeColor;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("sections")
    private List<Section> sections;
    
    public TeamsMessage() {
    }
    
    public TeamsMessage(String summary, String themeColor, String title, String text, List<Section> sections) {
        this.summary = summary;
        this.themeColor = themeColor;
        this.title = title;
        this.text = text;
        this.sections = sections;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getThemeColor() {
        return themeColor;
    }
    
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public List<Section> getSections() {
        return sections;
    }
    
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String summary;
        private String themeColor;
        private String title;
        private String text;
        private List<Section> sections;
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder themeColor(String themeColor) {
            this.themeColor = themeColor;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder sections(List<Section> sections) {
            this.sections = sections;
            return this;
        }
        
        public TeamsMessage build() {
            return new TeamsMessage(summary, themeColor, title, text, sections);
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Section {
        @JsonProperty("activityTitle")
        private String activityTitle;
        
        @JsonProperty("activitySubtitle")
        private String activitySubtitle;
        
        @JsonProperty("activityText")
        private String activityText;
        
        @JsonProperty("facts")
        private List<Fact> facts;
        
        @JsonProperty("markdown")
        private Boolean markdown;
        
        public Section() {
        }
        
        public Section(String activityTitle, String activitySubtitle, String activityText, List<Fact> facts, Boolean markdown) {
            this.activityTitle = activityTitle;
            this.activitySubtitle = activitySubtitle;
            this.activityText = activityText;
            this.facts = facts;
            this.markdown = markdown;
        }
        
        public String getActivityTitle() {
            return activityTitle;
        }
        
        public void setActivityTitle(String activityTitle) {
            this.activityTitle = activityTitle;
        }
        
        public String getActivitySubtitle() {
            return activitySubtitle;
        }
        
        public void setActivitySubtitle(String activitySubtitle) {
            this.activitySubtitle = activitySubtitle;
        }
        
        public String getActivityText() {
            return activityText;
        }
        
        public void setActivityText(String activityText) {
            this.activityText = activityText;
        }
        
        public List<Fact> getFacts() {
            return facts;
        }
        
        public void setFacts(List<Fact> facts) {
            this.facts = facts;
        }
        
        public Boolean getMarkdown() {
            return markdown;
        }
        
        public void setMarkdown(Boolean markdown) {
            this.markdown = markdown;
        }
        
        public static Section create(String title, String subtitle, String text, List<Fact> facts) {
            return new Section(title, subtitle, text, facts, true);
        }
        
        public static Section create(String title, String text, List<Fact> facts) {
            return new Section(title, null, text, facts, true);
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Fact {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("value")
        private String value;
        
        public Fact() {
        }
        
        public Fact(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
}