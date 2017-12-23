package osp.Devices;
import osp.IFLModules.*;
import osp.FileSys.OpenFile;
import osp.Threads.ThreadCB;
import osp.Memory.PageTableEntry;
import osp.Hardware.HClock;

/** 
   This class contains all the information necessary to carry out
   an I/O request.

    @OSPProject Devices
*/
public class IORB extends IflIORB
{
    /**
       The IORB constructor.
       Must have
       
	   super(thread,page,blockNumber,deviceID,ioType,openFile);

       as its first statement.

       @OSPProject Devices
    */
    public IORB(ThreadCB thread, PageTableEntry page, 
		int blockNumber, int deviceID, 
		int ioType, OpenFile openFile) 
		{
         super(thread, page, blockNumber, deviceID, ioType, openFile);
		 Device.createMap.put(getID(),HClock.get());
        }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
