package com.notification.platform.channel;

import com.notification.platform.domain.model.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChannelRouter {

    private static final Logger log = LoggerFactory.getLogger(ChannelRouter.class);
    private final Map<ChannelType, ChannelGateway> gatewayRegistry = new EnumMap<>(ChannelType.class);

    public ChannelRouter(List<ChannelGateway> gateways) {
        for (ChannelGateway gateway : gateways) {
            gatewayRegistry.put(gateway.getChannelType(), gateway);
            log.info("Registered Channel Gateway for channel: {}", gateway.getChannelType());
        }
    }

    public ChannelGateway getGateway(ChannelType channelType) {
        ChannelGateway gateway = gatewayRegistry.get(channelType);
        if (gateway == null) {
            throw new IllegalArgumentException("No channel gateway registered for channel type: " + channelType);
        }
        return gateway;
    }

    public boolean supportsChannel(ChannelType channelType) {
        return gatewayRegistry.containsKey(channelType);
    }
}
