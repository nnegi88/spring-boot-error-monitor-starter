package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.teams.TeamsMessage;

public interface TeamsMessageTemplate {
    TeamsMessage buildMessage(ErrorEvent event);
}