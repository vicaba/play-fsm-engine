package models.info_messages;

public class InfoMessage {
	private String type;
	private String message;

	public InfoMessage(String type, String message) {
		this.type = type;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
