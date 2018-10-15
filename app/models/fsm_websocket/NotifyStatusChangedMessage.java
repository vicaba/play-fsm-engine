package models.fsm_websocket;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

public class NotifyStatusChangedMessage {
	private String type;
	private String message;

	public NotifyStatusChangedMessage(String type, String message) {
		this.type = type;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public JsonNode toJson() {
		return Json.toJson(this);
	}
}
