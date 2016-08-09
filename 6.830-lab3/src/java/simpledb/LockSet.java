package simpledb;

import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
/**
 * LockSet is used to implement locks needed by transactions
 */
public class LockSet {
    private final ConcurrentMap<PageId, Object> lockRegistry;
    private final Map<PageId, ArrayList<TransactionId>> sharedLocks;
    private final Map<PageId, TransactionId> exclusiveLocks;
    private final Map<TransactionId, Set<PageId>> relatedPages;
    public LockSet()
    {
        lockRegistry = new ConcurrentHashMap<PageId, Object>();   
        sharedLocks = new HashMap<PageId, ArrayList<TransactionId>>();   
        exclusiveLocks = new HashMap<PageId, TransactionId>();   
        relatedPages = new HashMap<TransactionId, Set<PageId>>(); 
        
    }

    public static LockSet create()
    {
    	return new LockSet();
    }
    
    private Object getLock(PageId pageId) {
    	lockRegistry.putIfAbsent(pageId, new Object());
		return lockRegistry.get(pageId);
	}
    
    private ArrayList<TransactionId> getSharedLockList(PageId pageId) {
		if(sharedLocks.get(pageId)==null)
			sharedLocks.put(pageId,new ArrayList<TransactionId>());
		return sharedLocks.get(pageId);
	}
    
    private Set<PageId> getLockSet(TransactionId tid) {
    	if(relatedPages.get(tid)==null)
    		relatedPages.put(tid, new HashSet<PageId>());
		return relatedPages.get(tid);
	}
    
    public void acquireLock(TransactionId tid, PageId pid, Permissions perm)
    {
        if(perm==Permissions.READ_ONLY)
        {
            acquireSharedLock(tid,pid);
        }
        else if(perm==Permissions.READ_WRITE)
        {
            acquireExclusiveLock(tid,pid);
        }

    }

    public void releaseLock(TransactionId tid)
    {
    	Set<PageId> pageset = new HashSet<PageId>(getLockSet(tid)); 
    	for(PageId pid: pageset)
    	{
    		releaseLock(tid,pid);
    	}
    }
    
    public void releaseLock(TransactionId tid, PageId pid)
    {
    	Object lock = getLock(pid);
    	while(true)
        {
            synchronized(lock)
            {
            	if(exclusiveLocks.get(pid)==tid)
            		exclusiveLocks.put(pid,null);
            	
          
            	ArrayList<TransactionId> arrTran = getSharedLockList(pid);
            	arrTran.remove(tid);
            	
            	Set<PageId> arrPage = getLockSet(tid);
            	arrPage.remove(pid);
            	return;
            }
        }
    }

    public void acquireSharedLock(TransactionId tid, PageId pid)
    {
    	Object lock = getLock(pid);
        while(true)
        {
            synchronized(lock)
            {
            	if(exclusiveLocks.get(pid)==null | exclusiveLocks.get(pid)==tid)
            	{
            		ArrayList<TransactionId> arrTran = getSharedLockList(pid);
            		arrTran.add(tid);
            		//System.out.println(arrTran);
            	
            		Set<PageId> arrPage = getLockSet(tid);
            		arrPage.add(pid);
            		//System.out.println("end shared");
            		return;
            	}
            }
        }
    }

    public void acquireExclusiveLock(TransactionId tid, PageId pid)
    {
    	Object lock = getLock(pid);
    	while(true)
        {
        	
            synchronized(lock)
            {
            	if(exclusiveLocks.get(pid)==null | exclusiveLocks.get(pid)==tid)
            	{
            		if(getSharedLockList(pid).isEmpty())
            		{
            			exclusiveLocks.put(pid, tid);
            	
            			Set<PageId> arrPage = getLockSet(tid);
            			arrPage.add(pid);
            			//System.out.println("end exclusive");
            			return;
            		}
            		else if(getSharedLockList(pid).size()==1 & getSharedLockList(pid).get(0)==tid)
            		{
            			//update lock
            			getSharedLockList(pid).clear();
            			exclusiveLocks.put(pid, tid);
            			getLockSet(tid).add(pid);
            			return;
            		
            		}
            	}
            }
        }
    }
    
    public boolean holdsLock(TransactionId tid, PageId pid)
    {
    	return getLockSet(tid).contains(pid);
    }
    
}