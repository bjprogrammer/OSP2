package osp.Memory;

import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/public class PageFaultHandler extends IflPageFaultHandler

{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
		if(page.isValid())
    	{
    		return FAILURE;
    	}
        else
		{
			int result = lru(thread,page);
			MMU.pagefaultcount++;
			MyOut.print(MMU.pagefaultcount,"Page fault occurred. Current page fault count is "+ MMU.pagefaultcount);
			return result;
		}
    }

public static int lru(ThreadCB thread,PageTableEntry page)
{
    	FrameTableEntry newFrame = null;
		FrameTableEntry Framecomparator = null;
		int j= -1, i=0;
		while(i < MMU.getFrameTableSize())
    	{
    		newFrame = MMU.getFrame(i);
    		if((newFrame.isReserved()) || (newFrame.getLockCount()> 0))
    		{
				j++;
			}
			i++;
		}
		if(j == MMU.getFrameTableSize())
		{
			return NotEnoughMemory;
		}
		
    	for(int y = 0; y < MMU.getFrameTableSize(); y++)
    	{
    		newFrame = MMU.getFrame(y);
    		if((newFrame.getPage() == null) && (!newFrame.isReserved()) && (newFrame.getLockCount() <= 0))
    		{
				Event pfevent = reserveThread(thread, page, newFrame);
				
				page.setFrame(newFrame);
    	        swapIn(thread, page);
    	
    	        if(thread.getStatus() == ThreadKill)
    	        {
    		        swapInCleanup(pfevent,page,newFrame);
					
    		        ThreadCB.dispatch();
    		        return FAILURE;
    	        } 

    	        newFrame.setPage(page);
    	        page.setValid(true);
    	        
				MMU.LRUreferencestring.add(newFrame);
				releaseThread(pfevent, page, newFrame, thread);
				
				ThreadCB.dispatch();
    	        return SUCCESS;
    		}
    	}
		
		MMU.p = MMU.LRUreferencestring.size()-1;
		for(int k = 0; k< MMU.getFrameTableSize(); k++)
    	{
    		newFrame = MMU.getFrame(k);
    		if((newFrame.getPage() != null) && (!newFrame.isReserved()) && (newFrame.getLockCount() <= 0))
    		{
				innerloop:
				for(int x= MMU.LRUreferencestring.size()-1; x>= 0; x--)	
		        {
		           	Framecomparator = MMU.LRUreferencestring.get(x);
					if(newFrame.equals(Framecomparator))
					{
						if(x<MMU.p)
						{
							MMU.p=x;
						}
						break innerloop;
					}
				}
			}
		}
		
			newFrame = MMU.LRUreferencestring.get(MMU.p);
			if((newFrame.getPage() != null) && (!newFrame.isReserved()) && (newFrame.getLockCount() <= 0))
			{ 
				Event pfevent = reserveThread(thread, page, newFrame);
				
				PageTableEntry oldPage = newFrame.getPage();
			    if(newFrame.isDirty())
    		    {
    			 swapOut(thread, oldPage);
    			
    			 if(thread.getStatus() == ThreadKill)
    			 {
					swapOutCleanup(pfevent,page);
					
    				ThreadCB.dispatch();
    				return FAILURE;
    			 }
    			 newFrame.setDirty(false);
    		   }
    		   newFrame.setReferenced(false);
    		   newFrame.setPage(null);
    		   oldPage.setValid(false);
    		   oldPage.setFrame(null);
		  
		       page.setFrame(newFrame);
    	       swapIn(thread, page);
    	       if(thread.getStatus() == ThreadKill)
    	       {
    		        swapInCleanup(pfevent,page,newFrame);
					
    		        ThreadCB.dispatch();
    		        return FAILURE;
    	       } 

    	       newFrame.setPage(page);
    	       page.setValid(true);
    	       
			   MMU.LRUreferencestring.add(newFrame);
    	       releaseThread(pfevent, page, newFrame, thread);
				
			   ThreadCB.dispatch();
    	       return SUCCESS;
		    }
			
			ThreadCB.dispatch();
			return NotEnoughMemory;
}
   												
	public static Event reserveThread(ThreadCB thread, PageTableEntry page, FrameTableEntry newFrame)
    {
		Event pfevent = new SystemEvent("Kernel mode switching-PageFaultHappened");
    	thread.suspend(pfevent);
    	page.setValidatingThread(thread);
    	newFrame.setReserved(thread.getTask());
		return pfevent;
	}
	
	public static void releaseThread(Event pfevent, PageTableEntry page, FrameTableEntry newFrame,ThreadCB thread)
    {
		if(newFrame.getReserved() == thread.getTask())
    	{
    	    newFrame.setUnreserved(thread.getTask());    		
    	}
		page.setValidatingThread(null);
    	page.notifyThreads();
    	pfevent.notifyThreads();
	}
	
    public static void swapIn(ThreadCB thread, PageTableEntry page)
    {
    	TaskCB newTask = page.getTask();
    	newTask.getSwapFile().read(page.getID(), page, thread);
    }
	
    public static void swapOut(ThreadCB thread, PageTableEntry oldPage)
    {
    	TaskCB newTask = oldPage.getTask();
    	newTask.getSwapFile().write(oldPage.getID(), oldPage, thread);
    }
	
	public static void swapInCleanup(Event pfevent, PageTableEntry page,FrameTableEntry newFrame)
    {
		page.setValidatingThread(null);
		page.setFrame(null);
	    page.notifyThreads();
        pfevent.notifyThreads();
		newFrame.setPage(null);
	}
	
	public static void swapOutCleanup(Event pfevent, PageTableEntry page)
    {
		page.setValidatingThread(null); 
    	page.notifyThreads();
    	pfevent.notifyThreads();
	}
    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
