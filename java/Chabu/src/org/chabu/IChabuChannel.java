package org.chabu;


public interface IChabuChannel {

	void evUserXmitRequest();

	void evUserRecvRequest();

	IChabuChannelUser getUser();
}
