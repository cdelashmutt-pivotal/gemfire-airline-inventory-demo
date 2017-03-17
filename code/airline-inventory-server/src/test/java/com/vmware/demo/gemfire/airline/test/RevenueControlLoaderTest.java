package com.vmware.demo.gemfire.airline.test;

import java.math.BigDecimal;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;

import io.pivotal.pde.sample.airline.domain.RevenueControl;

/**
 * These tests are meant to be run against a separate GemFire cluster - they 
 * are not guaranteed to work against an embedded cluster
 * 
 * @author wmay
 *
 */

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/test-context.xml")
public class RevenueControlLoaderTest {
	
	private static Logger log = LoggerFactory.getLogger(RevenueControlLoaderTest.class);
	
	@Autowired
	private ClientCache cache;
	
	private Region<Integer, RevenueControl> revenueControlRegion;
	
	@Test
	public void testRevenueControlLoader(){
		revenueControlRegion = cache.getRegion("RevenueControl");
		
		Set<Integer> keys = revenueControlRegion.keySetOnServer();
		for(Integer k : keys) revenueControlRegion.remove(k);
		log.info("removed all (" + keys.size() + ") rev. control records");
		
		RevenueControl rc = revenueControlRegion.get(Integer.valueOf(111));
		Assert.assertNotNull(rc);
		log.info("retrieved RevenueControl: " + rc);
		
		Assert.assertEquals(111, rc.getFlightNumber());
		Assert.assertEquals(new BigDecimal(150), rc.getZeroSeatPrice());
		Assert.assertEquals(new BigDecimal(1), rc.getSlope());
		
	}
	
}
