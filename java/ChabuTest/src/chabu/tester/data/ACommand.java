package chabu.tester.data;


public abstract class ACommand {
	
	protected ACommand(CommandId commandId) {
		this.commandId = commandId;
	}
	
	public final CommandId commandId;
}
