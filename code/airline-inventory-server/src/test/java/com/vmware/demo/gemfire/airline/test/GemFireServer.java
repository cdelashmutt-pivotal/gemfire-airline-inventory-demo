package com.vmware.demo.gemfire.airline.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;

public class GemFireServer {
	public static void main(String []args){
		Cache cache = null;
		try {
			System.setProperty("REV_CTL_FILE", "config/revctls.txt");
			cache = new CacheFactory().create();
			System.out.println("GemFire started - press enter to exit");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			reader.readLine();
		} catch(Exception x){
			x.printStackTrace(System.err);
		} finally {
			if (cache != null) cache.close();
		}
	}
}
