package application.fsm;

public class GetDataMessage {
	private String query;

	public GetDataMessage(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}
}
