package com.notification.platform.channel.impl;

import com.notification.platform.channel.AbstractMockChannelGateway;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class PushGatewayService extends AbstractMockChannelGateway {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.PUSH;
    }

    @Override
    protected String resolveDestination(Notification notification, UserProfile profile) {
        return "push-token-" + notification.getUserId();
    }
}
