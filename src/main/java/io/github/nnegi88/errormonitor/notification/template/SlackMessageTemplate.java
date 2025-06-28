package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.slack.SlackMessage;

public interface SlackMessageTemplate {
    SlackMessage buildMessage(ErrorEvent event);
}