package com.notification.platform.channel.impl;

import com.notification.platform.channel.AbstractMockChannelGateway;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class SmsGatewayService extends AbstractMockChannelGateway {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }

    @Override
    protected String resolveDestination(Notification notification, UserProfile profile) {
        if (notification.getRecipientTarget() != null && !notification.getRecipientTarget().isBlank()) {
            return notification.getRecipientTarget();
        }
        if (profile != null && profile.getPhoneNumber() != null) {
            return profile.getPhoneNumber();
        }
        return "+639170000000";
    }
}
