package vn.nitrogen.platform.messaging;

public class InvalidMessageEnvelopeException extends IllegalArgumentException {

    public InvalidMessageEnvelopeException(String message) {
        super(message);
    }
}
