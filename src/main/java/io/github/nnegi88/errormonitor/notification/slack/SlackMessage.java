package io.github.nnegi88.errormonitor.notification.slack;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SlackMessage {
    private String text;
    private String channel;
    private String username;
    
    @JsonProperty("icon_emoji")
    private String iconEmoji;
    
    private List<Block> blocks = new ArrayList<>();
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getIconEmoji() {
        return iconEmoji;
    }
    
    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }
    
    public List<Block> getBlocks() {
        return blocks;
    }
    
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }
    
    public static SlackMessageBuilder builder() {
        return new SlackMessageBuilder();
    }
    
    public static class SlackMessageBuilder {
        private final SlackMessage message = new SlackMessage();
        
        public SlackMessageBuilder text(String text) {
            message.setText(text);
            return this;
        }
        
        public SlackMessageBuilder channel(String channel) {
            message.setChannel(channel);
            return this;
        }
        
        public SlackMessageBuilder username(String username) {
            message.setUsername(username);
            return this;
        }
        
        public SlackMessageBuilder iconEmoji(String iconEmoji) {
            message.setIconEmoji(iconEmoji);
            return this;
        }
        
        public SlackMessageBuilder addBlock(Block block) {
            message.getBlocks().add(block);
            return this;
        }
        
        public SlackMessage build() {
            return message;
        }
    }
    
    public static class Block {
        private String type;
        private Text text;
        private List<Field> fields;
        
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
        
        public List<Field> getFields() {
            return fields;
        }
        
        public void setFields(List<Field> fields) {
            this.fields = fields;
        }
        
        public static Block header(String text) {
            Block block = new Block();
            block.setType("header");
            Text textObj = new Text();
            textObj.setType("plain_text");
            textObj.setText(text);
            block.setText(textObj);
            return block;
        }
        
        public static Block section(Text text, List<Field> fields) {
            Block block = new Block();
            block.setType("section");
            block.setText(text);
            block.setFields(fields);
            return block;
        }
    }
    
    public static class Text {
        private String type;
        private String text;
        
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
        
        public static Text markdown(String text) {
            Text t = new Text();
            t.setType("mrkdwn");
            t.setText(text);
            return t;
        }
    }
    
    public static class Field {
        private String type;
        private String text;
        
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
        
        public static Field markdown(String text) {
            Field field = new Field();
            field.setType("mrkdwn");
            field.setText(text);
            return field;
        }
    }
}