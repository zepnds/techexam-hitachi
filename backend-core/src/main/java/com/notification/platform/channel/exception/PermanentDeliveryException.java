package com.notification.platform.channel.exception;

public class PermanentDeliveryException extends ChannelDeliveryException {
    public PermanentDeliveryException(String message) {
        super(message);
    }

    public PermanentDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean isRetryable() {
        return false;
    }
}
