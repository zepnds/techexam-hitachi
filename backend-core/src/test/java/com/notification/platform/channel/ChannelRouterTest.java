package com.notification.platform.channel;

import com.notification.platform.channel.impl.EmailGatewayService;
import com.notification.platform.channel.impl.PushGatewayService;
import com.notification.platform.channel.impl.SlackGatewayService;
import com.notification.platform.channel.impl.SmsGatewayService;
import com.notification.platform.domain.model.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelRouterTest {

    private ChannelRouter channelRouter;

    @BeforeEach
    void setUp() {
        EmailGatewayService email = new EmailGatewayService();
        SmsGatewayService sms = new SmsGatewayService();
        PushGatewayService push = new PushGatewayService();
        SlackGatewayService slack = new SlackGatewayService();

        channelRouter = new ChannelRouter(List.of(email, sms, push, slack));
    }

    @Test
    @DisplayName("Should route to appropriate gateway for each channel type")
    void testRouteToSupportedChannels() {
        assertThat(channelRouter.getGateway(ChannelType.EMAIL)).isInstanceOf(EmailGatewayService.class);
        assertThat(channelRouter.getGateway(ChannelType.SMS)).isInstanceOf(SmsGatewayService.class);
        assertThat(channelRouter.getGateway(ChannelType.PUSH)).isInstanceOf(PushGatewayService.class);
        assertThat(channelRouter.getGateway(ChannelType.SLACK)).isInstanceOf(SlackGatewayService.class);
    }

    @Test
    @DisplayName("Should return true for supported channels")
    void testSupportsChannel() {
        assertThat(channelRouter.supportsChannel(ChannelType.EMAIL)).isTrue();
        assertThat(channelRouter.supportsChannel(ChannelType.SMS)).isTrue();
        assertThat(channelRouter.supportsChannel(ChannelType.PUSH)).isTrue();
        assertThat(channelRouter.supportsChannel(ChannelType.SLACK)).isTrue();
    }
}
