package com.notification.platform.channel.exception;

public abstract class ChannelDeliveryException extends RuntimeException {
    public ChannelDeliveryException(String message) {
        super(message);
    }

    public ChannelDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract boolean isRetryable();
}
