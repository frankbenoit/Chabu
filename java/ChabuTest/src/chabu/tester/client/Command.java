package chabu.tester.client;

import chabu.tester.CommandId;

public class Command {
	CommandId commandId;
	long      time;
	int       address;
	int       port;
	int       channelId;
	int       txCount;
	int       rxCount;
	public int rxBufCount;
}
