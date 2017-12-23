package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{
    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
	
	static Map<Integer, Long> createMap;
    static Map<Integer, Long> dequeueMap;
    public Device(int id, int numberOfBlocks)
    {     
      super(id,numberOfBlocks);
	  iorbQueue = new GenericList();
	  createMap=new HashMap<Integer, Long>();
      dequeueMap= new HashMap<Integer, Long>();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
	public static int headmoves;
	public static int lastcylinder;
    public static void init()
    {
        headmoves=0;
		lastcylinder=0;
    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
        iorb.getPage().lock(iorb);
		if(iorb.getThread().getStatus() != ThreadCB.ThreadKill)
    	{
         iorb.getOpenFile().incrementIORBCount();
		}
		int blocksPerTrack = (((Disk) this).getSectorsPerTrack()*((Disk) this).getBytesPerSector())/
    						(int) Math.pow(2, MMU.getVirtualAddressBits() - MMU.getPageAddressBits()); 
    	int cylinder = iorb.getBlockNumber()/ (blocksPerTrack * ((Disk) this).getPlatters());
    	iorb.setCylinder(cylinder);
		
		if(iorb.getThread().getStatus() == ThreadCB.ThreadKill)
    	{
    		return FAILURE;
    	}
    	else
    	{
    		if(isBusy())
    		{
    			((GenericList)iorbQueue).insert(iorb);
		
    		}
    		else
    		{
    			startIO(iorb);
    		}
			return SUCCESS;
		}
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
        if(iorbQueue.isEmpty())
    	{
    		return null;
    	}
    	else
    	{
    		IORB iorb = (IORB) ((GenericList)iorbQueue).removeTail();
			dequeueMap.put(iorb.getID(),HClock.get());
			long responsetime = dequeueMap.get(iorb.getID())-createMap.get(iorb.getID());
			
			headmoves=headmoves+Math.abs(iorb.getCylinder()-lastcylinder);
			MyOut.print(iorb.getDeviceID(),"Device ID-" + iorb.getDeviceID());
			MyOut.print(iorb.getID(),"IORB ID-" + iorb.getID());
			MyOut.print(createMap.get(iorb.getID()),"IORB creation time-" + createMap.get(iorb.getID()));
			MyOut.print(dequeueMap.get(iorb.getID()),"IORB dequeu time-" + dequeueMap.get(iorb.getID()));
			MyOut.print(responsetime, "Response time-" + responsetime);
			MyOut.print(headmoves, "Current no of head moves- " + headmoves);
			lastcylinder=iorb.getCylinder();
    		return iorb;
    	}
        
    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
      if(iorbQueue.isEmpty())
    	{
    		return;
    	}

    	for(int i = iorbQueue.length() - 1; i >= 0; i--) 
    	{
    		IORB iorb = (IORB) ((GenericList) iorbQueue).getAt(i);
    		if(iorb.getThread().equals(thread))
    		{
    			iorb.getPage().unlock();
    			iorb.getOpenFile().decrementIORBCount();
    			if(iorb.getOpenFile().getIORBCount() == 0 && iorb.getOpenFile().closePending)
    			{
    				iorb.getOpenFile().close();	
    			}
				((GenericList) iorbQueue).remove(iorb);
				dequeueMap.put(iorb.getID(),HClock.get());
				long responsetime = dequeueMap.get(iorb.getID())-createMap.get(iorb.getID());
				MyOut.print(iorb.getID(),"IORB ID-" + iorb.getID());
			    MyOut.print(createMap.get(iorb.getID()),"IORB creation time-" + createMap.get(iorb.getID()));
			    MyOut.print(dequeueMap.get(iorb.getID()),"IORB dequeu time-" + dequeueMap.get(iorb.getID()));
			    MyOut.print(responsetime, "IORB Response time-" + responsetime);
    		}
    	}

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
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
