package io.github.nnegi88.errormonitor.notification.teams;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class TeamsMessage {
    @JsonProperty("@type")
    private String type = "MessageCard";
    
    @JsonProperty("@context")
    private String context = "https://schema.org/extensions";
    
    private String summary;
    private String themeColor;
    private List<Section> sections = new ArrayList<>();
    private List<Action> potentialAction = new ArrayList<>();
    
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
    
    public List<Section> getSections() {
        return sections;
    }
    
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }
    
    public List<Action> getPotentialAction() {
        return potentialAction;
    }
    
    public void setPotentialAction(List<Action> potentialAction) {
        this.potentialAction = potentialAction;
    }
    
    public static TeamsMessageBuilder builder() {
        return new TeamsMessageBuilder();
    }
    
    public static class TeamsMessageBuilder {
        private final TeamsMessage message = new TeamsMessage();
        
        public TeamsMessageBuilder title(String title) {
            message.setSummary(title);
            return this;
        }
        
        public TeamsMessageBuilder themeColor(String color) {
            message.setThemeColor(color);
            return this;
        }
        
        public TeamsMessageBuilder addSection(Section section) {
            message.getSections().add(section);
            return this;
        }
        
        public TeamsMessageBuilder addFact(String name, String value) {
            if (message.getSections().isEmpty()) {
                message.getSections().add(new Section());
            }
            Section lastSection = message.getSections().get(message.getSections().size() - 1);
            if (lastSection.getFacts() == null) {
                lastSection.setFacts(new ArrayList<>());
            }
            Fact fact = new Fact();
            fact.setName(name);
            fact.setValue(value);
            lastSection.getFacts().add(fact);
            return this;
        }
        
        public TeamsMessageBuilder addAction(Action action) {
            message.getPotentialAction().add(action);
            return this;
        }
        
        public TeamsMessage build() {
            return message;
        }
    }
    
    public static class Section {
        private String activityTitle;
        private String activitySubtitle;
        private String text;
        private List<Fact> facts;
        
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
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public List<Fact> getFacts() {
            return facts;
        }
        
        public void setFacts(List<Fact> facts) {
            this.facts = facts;
        }
    }
    
    public static class Fact {
        private String name;
        private String value;
        
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
    
    public static class Action {
        @JsonProperty("@type")
        private String type;
        
        private String name;
        private List<Target> targets;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<Target> getTargets() {
            return targets;
        }
        
        public void setTargets(List<Target> targets) {
            this.targets = targets;
        }
        
        public static Action openUri(String name, String uri) {
            Action action = new Action();
            action.setType("OpenUri");
            action.setName(name);
            Target target = new Target();
            target.setOs("default");
            target.setUri(uri);
            action.setTargets(List.of(target));
            return action;
        }
    }
    
    public static class Target {
        private String os;
        private String uri;
        
        public String getOs() {
            return os;
        }
        
        public void setOs(String os) {
            this.os = os;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}