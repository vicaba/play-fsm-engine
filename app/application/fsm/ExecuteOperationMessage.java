package application.fsm;

public class ExecuteOperationMessage {
	private String operation;

	public ExecuteOperationMessage(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return operation;
	}
}
