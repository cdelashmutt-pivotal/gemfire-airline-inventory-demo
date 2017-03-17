package io.pivotal.pde.sample.gemfire.util;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;

/**
 * Utility class for wrapping an arbitrary piece of code in 
 * a GemFire Transaction - 
 * 
 * 
 * @author wmay
 *
 */
public abstract class TransactionTemplate {
	public abstract void doInTransaction();
	
	public void execute(){
		CacheTransactionManager tm = CacheFactory.getAnyInstance().getCacheTransactionManager();
		try {
			while(true) {
				try {
					tm.begin();
	
					this.doInTransaction();
					
					tm.commit();
					tm = null; //
					break;
				} catch(CommitConflictException x){
					CacheFactory.getAnyInstance().getLogger().fine("commit conflict - will retry");
				}
			}
		} catch(Exception x){
			if (tm != null) tm.rollback();
			CacheFactory.getAnyInstance().getLogger().error("transaction rolled back due to exception" , x);
		}
	}
}
