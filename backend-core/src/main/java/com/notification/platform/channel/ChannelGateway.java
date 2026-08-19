package com.notification.platform.channel;

import com.notification.platform.channel.model.DeliveryResult;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;

public interface ChannelGateway {

  
    ChannelType getChannelType();

   
    DeliveryResult send(Notification notification, UserProfile profile);
}
