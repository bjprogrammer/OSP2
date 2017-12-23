package osp.Resources;

import java.util.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Memory.*;

/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/
public class ResourceCB extends IflResourceCB
{
    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
	
	private static Hashtable<ThreadCB, RRB> resourceRequestTable;
	private static RRB emptyRRB;
	private static int size = 0;
    public ResourceCB(int qty)
    {
       super(qty);
    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
	
    public static void init()
    {
       resourceRequestTable = new Hashtable<>();
	   size = ResourceTable.getSize();
	   emptyRRB = new RRB(null, null, 0);
    }

    /**
       Tries to acquire the given quantity of this resource.
       Uses deadlock avoidance or detection depending on the
       strategy in use, as determined by ResourceCB.getDeadlockMethod().

       @param quantity
       @return The RRB corresponding to the request.
       If the request is invalid (quantity+allocated>total) then return null.

       @OSPProject Resources
    */
   	public RRB do_acquire(int quantity) {

		/*
		 * First, the current task can be found from the page table base
		 * register, or PTBR
		 */
		TaskCB currentTask = MMU.getPTBR().getTask();
		ThreadCB currentThread = currentTask.getCurrentThread();

		// If asking more than can be given
		if (quantity + getAllocated(currentThread) > getTotal()) {
			return null;
		}

		// if not present in hashtable then, add thread to the hashtable
		if (!resourceRequestTable.containsKey(currentThread))
			resourceRequestTable.put(currentThread, emptyRRB);

		RRB rrb = new RRB(currentThread, this, quantity);

		// To find out which mode is in Detection or Avoidance
		if (getDeadlockMethod() == Detection) {

			if (quantity <= getAvailable())
				rrb.grant();

			else {
				if (currentThread.getStatus() != ThreadWaiting) {
					rrb.setStatus(Suspended);
					currentThread.suspend(rrb);
				}

				if (!resourceRequestTable.containsValue(rrb))
					resourceRequestTable.put(currentThread, rrb);

			}

		}
		if (getDeadlockMethod() == Avoidance) {
			// Implement Banker's algorithm
			if (bankersAlgorithm(rrb)) {
				rrb.grant();
			}
			/*
			 * When a thread is suspended inside do acquire(), its execution is
			 * paused until the request is granted (possibly as a result of a
			 * release() operation on the same resource or of giveupResources()
			 * operation, which is invoked when a thread is killed)
			 */
			if (rrb.getStatus() == Suspended
					&& (!resourceRequestTable.containsValue(rrb)))
				resourceRequestTable.put(currentThread, rrb);

		}

		return rrb;
	}

	public boolean bankersAlgorithm(RRB rrb) {

		ResourceCB resource = rrb.getResource();
		ThreadCB currentThread = rrb.getThread();
		int quantity = rrb.getQuantity();

		// if asking more than can be allocated then has to be denied
		if (resource.getAllocated(currentThread) + quantity > resource
				.getMaxClaim(currentThread)) {
			rrb.setStatus(Denied);
			return false;
		}
		// if can be allocated later then put on suspended
		if (quantity > resource.getAvailable()) {
			if ((currentThread.getStatus() != ThreadWaiting)
					) {

				currentThread.suspend(rrb);

			}
			rrb.setStatus(Suspended);
			return false;
		}

		rrb.setStatus(Granted);
		return true;
	}

    /**
       Performs deadlock detection.
       @return A vector of ThreadCB objects found to be in a deadlock.

       @OSPProject Resources
    */
    public static Vector do_deadlockDetection()
    {
       int[] available = new int[size];

		for (int i = 0; i < size; i++) {
			available[i] = ResourceTable.getResourceCB(i).getAvailable();
		}

		// now check with what we have been tracking

		Hashtable<ThreadCB, Boolean> localHashTable = new Hashtable<>();

		Enumeration<ThreadCB> enumeration = resourceRequestTable.keys();
		while (enumeration.hasMoreElements()) {
			// mark each false at the beginning
			ThreadCB threadCB = enumeration.nextElement();
			localHashTable.put(threadCB, new Boolean(false));

			for (int i = 0; i < size; i++) {
				// changes for request matrix creation
				ResourceCB res = ResourceTable.getResourceCB(i);
				if (res.getAllocated(threadCB) != 0) {
					localHashTable.put(threadCB, new Boolean(true));
					break;
				}
			}
		}

		// Now we keep on checking for the allocated ones or marked ones
		boolean isDeadlock = true;
		while (true) {
			isDeadlock = true;
			enumeration = resourceRequestTable.keys();
			while (enumeration.hasMoreElements()) {
				boolean askingMore = true;

				ThreadCB thread = enumeration.nextElement();

				if (localHashTable.get(thread)) { // if this thread from
													// tracking is also
					// the marked ones
					askingMore = false;
					int qty = resourceRequestTable.get(thread).getQuantity();
					if (qty != 0) {
						ResourceCB r = resourceRequestTable.get(thread)
								.getResource();
						// asking more than available then
						if (qty > available[r.getID()]) {
							askingMore = true;
						}
					}
					// if asking less than available and does not cause deadlock
					if (!askingMore) {
						for (int j = 0; j < size; j++) {
							available[j] += ResourceTable.getResourceCB(j)
									.getAllocated(thread);
						}

						localHashTable.put(thread, new Boolean(false));
						isDeadlock = false;
					}
				}
			}

			// if deadlock found
			if (isDeadlock) {
				break;
			}
		}

		/*
		 * The result returned by this method should be a vector of ThreadCB
		 * objects that were found to be involved in a deadlock.
		 */
		Vector<ThreadCB> results = new Vector<>();

		Enumeration<ThreadCB> tmpEnumeration = localHashTable.keys();
		while (tmpEnumeration.hasMoreElements()) {
			ThreadCB thread = tmpEnumeration.nextElement();
			if (localHashTable.get(thread))
				results.addElement(thread);
		}

		if (results.isEmpty())
			return null;

		for (int j = 0; j < results.size(); j++) {
			ThreadCB threadCB = results.get(j);
			threadCB.kill();

		}
		RRB rrb = null;
		while ((rrb = grantMethod()) != null) {
			rrb.grant();
			resourceRequestTable.put(rrb.getThread(), emptyRRB);
		}

		return results;

    }
  
  
    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
       for (int i = 0; i < size; i++) {
			ResourceCB resourceCB = ResourceTable.getResourceCB(i);
			if (resourceCB.getAllocated(thread) != 0) {
				resourceCB.setAvailable(resourceCB.getAvailable()
						+ resourceCB.getAllocated(thread));
			}
			resourceCB.setAllocated(thread, 0);
		}
		resourceRequestTable.remove(thread);
		RRB rrb = null;
		while ((rrb = grantMethod()) != null) {
			if (rrb.getThread().getStatus() != ThreadKill && (rrb.getThread() != thread) ) {

				rrb.grant();
				resourceRequestTable.put(rrb.getThread(), emptyRRB);
			}
			resourceRequestTable.put(rrb.getThread(), emptyRRB);

		}
    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)
    {
        TaskCB taskCB = MMU.getPTBR().getTask();
		ThreadCB threadCB = taskCB.getCurrentThread();

		// occupied by the thread resource
		int currentAllocated = getAllocated(threadCB);

		// if quantity asked to release is greater than currently occupied
		if (quantity > currentAllocated) {
			quantity = currentAllocated;
		}

		setAllocated(threadCB, currentAllocated - quantity);
		setAvailable(getAvailable() + quantity);

		RRB rrb = null;

		while ((rrb = grantMethod()) != null) {
			if (threadCB.getStatus() != ThreadKill) {
				rrb.grant();
				resourceRequestTable.put(rrb.getThread(), emptyRRB);
			}
			resourceRequestTable.put(rrb.getThread(), emptyRRB);
		}

      }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
	
  
 public static RRB grantMethod() {
		Enumeration<ThreadCB> keys = resourceRequestTable.keys();
		while (keys.hasMoreElements()) {

			ThreadCB key = keys.nextElement();
			RRB rrb = resourceRequestTable.get(key);

			if (rrb.getThread() != null) {
				if (getDeadlockMethod() == Avoidance) {

				    rrb.setStatus(Suspended);
					return rrb;
				}
				if ((getDeadlockMethod() == Suspended)
						&& (rrb.getQuantity() <= rrb.getResource()
								.getAvailable())) {
					return rrb;
				}
			}
		}
		return null;
	}
   
   
    public static void atError()
    {
        // your code goes here

    }
  
    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
