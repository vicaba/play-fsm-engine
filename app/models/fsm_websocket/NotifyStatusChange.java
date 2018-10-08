package models.fsm_websocket;

public class NotifyStatusChange {
	private String message;

	public NotifyStatusChange(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
