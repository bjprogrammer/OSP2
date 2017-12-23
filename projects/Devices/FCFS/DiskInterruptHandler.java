package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
        IORB iorbevent = (IORB) InterruptVector.getEvent();
        OpenFile file = iorbevent.getOpenFile();
        ThreadCB thread = iorbevent.getThread();
		TaskCB task = thread.getTask();
        PageTableEntry PTE = iorbevent.getPage();
        FrameTableEntry FTE = PTE.getFrame();
        int deviceID = iorbevent.getDeviceID();
		
        file.decrementIORBCount();
        
        if(file.closePending && file.getIORBCount() == 0)
		{
			file.close();	
		}
		
		PTE.unlock();
		
		if(task.getStatus() != TaskTerm)
        {
        	if(iorbevent.getDeviceID() != SwapDeviceID && thread.getStatus() != ThreadCB.ThreadKill)
            {
            	FTE.setReferenced(true);
            	if(iorbevent.getIOType() == FileRead)
            	{
            		FTE.setDirty(true);
            	}
            }
        	else 
            {
            	FTE.setDirty(false);
            }
        }
        
        if(task.getStatus() == TaskTerm && FTE.isReserved())
        {
        	FTE.setUnreserved(task);
        }
        
        iorbevent.notifyThreads();
        
        Device.get(deviceID).setBusy(false);
		
        IORB device = Device.get(deviceID).dequeueIORB();
        if (device != null) 
    	{
    		Device.get(deviceID).startIO(device);
    	}

        ThreadCB.dispatch();
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
