package io.pivotal.gemfire_addon.tools;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;

public class Loader {
	public static void main(String []args){
		ClientCache cache = null;
		try {
			cache = new ClientCacheFactory().addPoolLocator("localhost", 10000).create();
			Region<Object,Object> testRegion = cache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("Test");
			
			for(int i=0; i< 1000; ++i){
				testRegion.put(Integer.valueOf(i), Integer.valueOf(i));
			}
			
		} catch(Exception x){
			x.printStackTrace(System.err);
		} finally {
			if (cache != null) cache.close();
		}
	}
}
