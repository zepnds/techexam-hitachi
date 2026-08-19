package com.notification.platform.channel.impl;

import com.notification.platform.channel.AbstractMockChannelGateway;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class SlackGatewayService extends AbstractMockChannelGateway {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SLACK;
    }

    @Override
    protected String resolveDestination(Notification notification, UserProfile profile) {
        return "#notifications-" + notification.getUserId();
    }
}
