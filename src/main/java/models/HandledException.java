package models;

public class HandledException extends Exception {

    @Override
    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }

    private String message;
    private ErrorType type;
    public HandledException(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }
}
