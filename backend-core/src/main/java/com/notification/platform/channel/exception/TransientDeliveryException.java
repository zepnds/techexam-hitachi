package com.notification.platform.channel.exception;

public class TransientDeliveryException extends ChannelDeliveryException {
    public TransientDeliveryException(String message) {
        super(message);
    }

    public TransientDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
