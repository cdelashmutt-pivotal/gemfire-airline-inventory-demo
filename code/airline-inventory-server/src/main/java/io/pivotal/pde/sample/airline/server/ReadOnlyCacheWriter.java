package io.pivotal.pde.sample.airline.server;

import java.util.Properties;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;

public class ReadOnlyCacheWriter implements CacheWriter<Object, Object>,
		Declarable {

	@Override
	public void close() {
	}

	@Override
	public void init(Properties props) {
	}

	@Override
	public void beforeCreate(EntryEvent<Object, Object> event)
			throws CacheWriterException {
		checkReadOnly();
	}

	@Override
	public void beforeDestroy(EntryEvent<Object, Object> event)
			throws CacheWriterException {
		checkReadOnly();
	}

	@Override
	public void beforeRegionClear(RegionEvent<Object, Object> event)
			throws CacheWriterException {
		checkReadOnly();
	}

	@Override
	public void beforeRegionDestroy(RegionEvent<Object, Object> event)
			throws CacheWriterException {
		checkReadOnly();
	}

	@Override
	public void beforeUpdate(EntryEvent<Object, Object> event)
			throws CacheWriterException {
		checkReadOnly();
	}

	private void checkReadOnly(){
		Region<String,String> systemParameterRegion = CacheFactory.getAnyInstance().getRegion("SystemParameter");
		String serverMode = systemParameterRegion.get(CacheControlFunction.SERVER_MODE_SYSTEM_PARAM);
		if (serverMode != null){
			if (serverMode.equals(CacheControlFunction.SERVER_MODE_RO)) throw new CacheWriterException("Cache is currently in readonly mode");
		}
	}
	
}
