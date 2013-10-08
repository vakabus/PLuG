package ch.usi.dag.disl.test2.suite.afterinit.junit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ch.usi.dag.disl.test2.utils.*;

@RunWith(JUnit4.class)
public class AfterInitTest {

	ClientServerRunner r = new ClientServerRunner(this.getClass());
	
	@Test
	public void test() 
			throws Exception {		
		r.start();			
		r.waitFor();
		r.assertIsFinished();
		if(Boolean.parseBoolean(System.getProperty("disl.test.verbose"))) { r.destroyIfRunningAndFlushOutputs(); }
		r.assertIsSuccessfull();
		r.assertClientOut("client.out.resource");
		r.assertClientErrNull();
		r.assertServerOutNull();
		r.assertServerErrNull();
	}	
	
	@After
	public void cleanup() {
		r.destroy();
	}
}
