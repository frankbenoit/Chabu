package org.chabu;

import org.chabu.internal.PriorizerTest;
import org.chabu.internal.UtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	TestSetupConnection.class, 
	TestTransfer.class, 
	PriorizerTest.class, 
	UtilsTest.class })
public class AllTests {

}
