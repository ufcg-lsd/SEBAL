package org.fogbowcloud.sebal;

import static org.junit.Assert.*;

import org.junit.Test;

public class SebalTest {

	private void setUpF1() {
		
	}
	
	private void setUpF2() {
		
	}
	
	@Test
	public void testF1() {
		setUpF1();
		fail("Not yet implemented");
	}

	@Test
	public void testF2() {
		setUpF1();
		setUpF2();
		fail("Not yet implemented");
	}
	
	@Test
	public void testF1F2() {
		setUpF1();
		testF1();
		setUpF2();
		testF2();
		fail("Not yet implemented");
	}
	
}
