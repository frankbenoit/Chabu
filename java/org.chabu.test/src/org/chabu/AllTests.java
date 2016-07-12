package org.chabu;

import org.chabu.prot.v1.internal.ByteBufferUtilsTest;
import org.chabu.prot.v1.internal.ChabuReceiverStartupTest;
import org.chabu.prot.v1.internal.PriorizerTest;
import org.chabu.prot.v1.internal.UtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	TestSetupConnection.class, 
	TestTransfer.class, 
	PriorizerTest.class, 
	UtilsTest.class,
	ByteBufferUtilsTest.class,
	ChabuReceiverStartupTest.class})
public class AllTests {

}
