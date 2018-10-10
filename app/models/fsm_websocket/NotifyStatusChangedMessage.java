package models.fsm_websocket;

public class NotifyStatusChangedMessage {
	private String message;

	public NotifyStatusChangedMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
