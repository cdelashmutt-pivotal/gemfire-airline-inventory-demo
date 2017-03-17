package com.vmware.demo.gemfire.airline.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
//import io.pivotal.pde.sample.airline.domain.pdx.Flight;

public class FlightTest {
	
//	@Test
//	public void flightDelta() throws IOException {
//		
//		// need a GemFire cache to exist
//		Cache c = new CacheFactory().create();
//		
//		Flight originalFlight = new Flight();
//		
//		originalFlight.setArrivalTime("1100");
//		originalFlight.setCapacity(130);
//		originalFlight.setDepartureDate("20130421");
//		originalFlight.setDepartureTime("0900");
//		originalFlight.setDestination("LAX");
//		originalFlight.setEquipmentCode("MD5");
//		originalFlight.setFlightNumber(222);
//		originalFlight.setOrigin("SFO");
//		originalFlight.setSeatsSold(2);
//		originalFlight.setTimeZone("PST");		
//		
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		DataOutput out = new DataOutputStream(bos);
//		
//		originalFlight.toDelta(out);
//		bos.close();
//		
//		DataInput in = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
//		
//		Flight copy = new Flight();
//		copy.fromDelta(in);
//		
//		Assert.assertTrue(originalFlight.equals(copy));
//		
//		bos = new ByteArrayOutputStream();
//		out = new DataOutputStream(bos);
//		
//		originalFlight.toDelta(out);
//		bos.close();
//		
//		in = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
//		
//		copy.fromDelta(in);
//
//		Assert.assertTrue(originalFlight.equals(copy));		
//	}
	
}
