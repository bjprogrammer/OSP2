package osp.Tasks;

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;
import java.util.ArrayList;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
    private ArrayList<OpenFile> openFileList;
	private ArrayList<ThreadCB> threadsList;
	private ArrayList<PortCB> portsList;

	/**
       The task constructor. Must have

       	   super();

       as its first statement.

       @OSPProject Tasks
    */
    public TaskCB()
    {
        super();
		openFileList = new ArrayList<OpenFile>();
		threadsList = new ArrayList<ThreadCB>();
		portsList = new ArrayList<PortCB>();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {
        
    }

    /** 
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
	(in bytes) of the addressable virtual memory.

	@return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
        TaskCB taskCB = new TaskCB();
		PageTable pageTable = new PageTable(taskCB);
		
        taskCB.setCreationTime(HClock.get());
		taskCB.setStatus(TaskLive);
		taskCB.setPageTable(pageTable);
		taskCB.setPriority(5);
		
		String sFilePath = SwapDeviceMountPoint + taskCB.getID(); /* generate swap file path*/
		int sFileSize = (int) Math.pow(2, MMU.getVirtualAddressBits()); /* Calculate the file size*/
		FileSys.create(sFilePath, sFileSize);
		OpenFile sFile = OpenFile.open(sFilePath, taskCB);

		/*handling of file creation error*/
		if (sFile == null) 
		{
			ThreadCB.dispatch();
			return null;
		}
		taskCB.setSwapFile(sFile);/*Set the Swap file to the task */
		ThreadCB.create(taskCB);

		return taskCB;
    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
	
       @OSPProject Tasks
    */
    public void do_kill()
    {
        for (int count = 0; count< threadsList.size(); ) 
		{
			threadsList.get(count).kill();
		}

				
		for (int count = 0; count<portsList.size(); ) 
		{
					portsList.get(count).destroy(); 
		}

		for (int i = openFileList.size() - 1; i >= 0; i--) 
		{
			if (openFileList.get(i) != null) 
			openFileList.get(i).close(); 
		}
		
		setStatus(TaskTerm);
        getPageTable().deallocateMemory();		
		FileSys.delete(SwapDeviceMountPoint + this.getID());	
    }

    /** 
	Returns a count of the number of threads in this task. 
	
	@OSPProject Tasks
    */
    public int do_getThreadCount()
    {
        return threadsList.size();
    }

    /**
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
        if (do_getThreadCount() < ThreadCB.MaxThreadsPerTask) 
		{
			this.threadsList.add(thread);
			return SUCCESS;
		}
		else
			return FAILURE;
    }

    /**
       Removes the specified thread from this task. 		

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
      if (threadsList.contains(thread))
	  {
		    threadsList.remove(thread);
			return SUCCESS;
	  }
	  else
			return FAILURE;
    }

    /**
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
        return portsList.size();
    }

    /**
       Add the port to the list of ports owned by this task.
	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
        if (do_getPortCount() < PortCB.MaxPortsPerTask)
		{
			portsList.add(newPort);
			return SUCCESS;
		}
		else
			return FAILURE;
    }

    /**
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
        if (portsList.contains(oldPort)) 
		{
			portsList.remove(oldPort);
			return SUCCESS;
		}
		else
		    return FAILURE;
    }

    /**
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
        openFileList.add(file);
    }

    /** 
	Remove file from the task's open files table.

	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
        if (openFileList.contains(file)) 
		{
			openFileList.remove(file);
			return SUCCESS;
		}
		else
			return FAILURE;
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {
        // your code goes here

    }

    /**
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
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
