package io.github.nnegi88.errormonitor.infrastructure.notification.slack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Slack message model using Block Kit format.
 * Designed specifically for Slack webhook payloads.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackMessage {
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("blocks")
    private List<Block> blocks;
    
    public SlackMessage() {
    }
    
    public SlackMessage(String text, List<Block> blocks) {
        this.text = text;
        this.blocks = blocks;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public List<Block> getBlocks() {
        return blocks;
    }
    
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String text;
        private List<Block> blocks;
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder blocks(List<Block> blocks) {
            this.blocks = blocks;
            return this;
        }
        
        public SlackMessage build() {
            return new SlackMessage(text, blocks);
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Block {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private Text text;
        
        @JsonProperty("fields")
        private List<Text> fields;
        
        public Block() {
        }
        
        public Block(String type, Text text, List<Text> fields) {
            this.type = type;
            this.text = text;
            this.fields = fields;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Text getText() {
            return text;
        }
        
        public void setText(Text text) {
            this.text = text;
        }
        
        public List<Text> getFields() {
            return fields;
        }
        
        public void setFields(List<Text> fields) {
            this.fields = fields;
        }
        
        public static Block header(String text) {
            return new Block("header", Text.plainText(text), null);
        }
        
        public static Block section(String text) {
            return new Block("section", Text.markdown(text), null);
        }
        
        public static Block section(String text, List<Text> fields) {
            return new Block("section", text != null ? Text.markdown(text) : null, fields);
        }
        
        public static Block divider() {
            return new Block("divider", null, null);
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Text {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("emoji")
        private Boolean emoji;
        
        public Text() {
        }
        
        public Text(String type, String text) {
            this.type = type;
            this.text = text;
            this.emoji = null;
        }
        
        public Text(String type, String text, Boolean emoji) {
            this.type = type;
            this.text = text;
            this.emoji = emoji;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public Boolean getEmoji() {
            return emoji;
        }
        
        public void setEmoji(Boolean emoji) {
            this.emoji = emoji;
        }
        
        public static Text plainText(String text) {
            return new Text("plain_text", text, true);
        }
        
        public static Text markdown(String text) {
            return new Text("mrkdwn", text);
        }
    }
}