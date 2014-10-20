package mctcp.tester.client;

import mctcp.tester.CommandId;

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
