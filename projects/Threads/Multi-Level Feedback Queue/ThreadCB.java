package osp.Threads;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        super();
    }
    
    static ArrayList<ThreadCB> hpriorityQueue,lpriorityQueue;
    static long ThreadCount,ThreadFinish,lastCPUBurst,lastDispatch,resetcount;
    static Map<Integer, Long> startMap;
    static Map<Integer, Long> endMap;
    static Map<Integer, Long> finishMap;
    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        hpriorityQueue = new ArrayList<ThreadCB>();
		lpriorityQueue = new ArrayList<ThreadCB>();
        ThreadCount = 0;
        ThreadFinish=0;
		lastCPUBurst=0;
		lastDispatch=0;
		resetcount=0;
        startMap=new HashMap<Integer, Long>();
        endMap=new HashMap<Integer, Long>();
        finishMap= new HashMap<Integer, Long>();
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        if(task == null || task.getThreadCount() == MaxThreadsPerTask)
        {
            dispatch();
			return null;
        }
        
        ThreadCB newthread = new ThreadCB();
        startMap.put(newthread.getID(), newthread.getCreationTime());
        ThreadCount++;
        newthread.setTask(task);
        newthread.setStatus(ThreadReady);
        
        if(task.addThread(newthread) != SUCCESS)
        {
            dispatch();
			return null;
        }
        
        hpriorityQueue.add(newthread);
        dispatch();
        
        return newthread;
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        TaskCB task = getTask();
        
        if(getStatus() == ThreadRunning)
        {
            task.setCurrentThread(null);
            MMU.setPTBR(null);
        }
        else if(getStatus() >= ThreadWaiting)
        {
            for (int i = 0; i < Device.getTableSize(); i++)
            {
                Device.get(i).cancelPendingIO(this);
            }
        }
        else
        {                                              // Thread status -ThreadReady
            if(hpriorityQueue.contains(this)) 
			{
                hpriorityQueue.remove(this);
            }
            else if(lpriorityQueue.contains(this)) 
			{
                lpriorityQueue.remove(this);
            }                               
        }
        
        setStatus(ThreadKill);
        ResourceCB.giveupResources(this);
        
        task.removeThread(this);
        if(task.getThreadCount() == 0)
        {
            task.kill();
        }
        
        if(!finishMap.containsKey(getID()))
        {
			Long TATTime,stTime,fsTime;
            ThreadFinish++;
            finishMap.put(getID(),HClock.get());
			
			stTime = startMap.get(getID());
			fsTime = finishMap.get(getID());
			TATTime=fsTime-stTime;
			MyOut.print(getID(),"Thread ID : "+getID());
            MyOut.print(TATTime,"Turn around time of thread : "+TATTime);
			
            ThroughputData(this);
        }
        
        dispatch();
    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        if(getStatus() == ThreadRunning)
        {
            setStatus(ThreadWaiting);
            MMU.setPTBR(null);
            getTask().setCurrentThread(null);
        }
        else if(getStatus() >= ThreadWaiting)
        {
            setStatus(getStatus() + 1);
        }
        else
        {
            setStatus(ThreadWaiting);          // Thread Status-ThreadReady
        }
        if(!event.contains(this))
            event.addThread(this);
        if(lastCPUBurst < 30)
		{
		 lastCPUBurst = (HClock.get() - lastDispatch) + lastCPUBurst;
		}
		else 
		{
		 lastCPUBurst = (HClock.get() - lastDispatch);	
		}
        dispatch();

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        if(getStatus() == ThreadRunning)
        {
            return;
        }
        else if(getStatus() == ThreadWaiting)
        {
            setStatus(ThreadReady);
            if( (0 <= lastCPUBurst && lastCPUBurst < 30))
		    {
                hpriorityQueue.add(this);
            }
            else 
			{
                lpriorityQueue.add(this);
            }        
        }
        else if(getStatus() != ThreadReady)
        {
            setStatus(getStatus() - 1);
        }
        
        dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
        ThreadCB oldThread,newThread;
        
        try
        {
            oldThread = MMU.getPTBR().getTask().getCurrentThread();
            oldThread.getTask().setCurrentThread(null);
            MMU.setPTBR(null);
            oldThread.setStatus(ThreadReady);
			
            if( (0<=oldThread.lastCPUBurst && oldThread.lastCPUBurst < 30)) 
			{
                hpriorityQueue.add(oldThread);
            }
            else 
			{
                lpriorityQueue.add(oldThread);
            }
            if(oldThread.lastCPUBurst<30)
		    {
		      oldThread.lastCPUBurst = (HClock.get() - oldThread.lastDispatch) + oldThread.lastCPUBurst;
		    }
		    else
		    {
		      oldThread.lastCPUBurst = (HClock.get() - oldThread.lastDispatch);	
		    }
        }
        catch(NullPointerException e)
        {
			MMU.setPTBR(null);
		}
        
		  if(Math.floor(HClock.get()/1000) > resetcount)
		  {
			resetcount= (long)Math.floor(HClock.get()/1000);
			
			while(! lpriorityQueue.isEmpty()) 
			{
                ThreadCB removed = lpriorityQueue.remove(0);
                hpriorityQueue.add(removed);
				// MyOut.print("hello","hello");
            }
           }
		
        if(! hpriorityQueue.isEmpty())
        {
              newThread = hpriorityQueue.remove(0);
              newThread.setStatus(ThreadRunning);
              MMU.setPTBR(newThread.getTask().getPageTable());
              newThread.getTask().setCurrentThread(newThread);
			  ResponseTimeData(newThread);
			  HTimer.set(30);
              newThread.lastDispatch = HClock.get();
			  return SUCCESS;
        }
        else if(! lpriorityQueue.isEmpty())
        {
            newThread = lpriorityQueue.remove(0);
            newThread.setStatus(ThreadRunning);
            MMU.setPTBR(newThread.getTask().getPageTable());
            newThread.getTask().setCurrentThread(newThread);
			ResponseTimeData(newThread);
			HTimer.set(60);
            newThread.lastDispatch = HClock.get();
			return SUCCESS;
        }
        else
		{
			return FAILURE;
		}
    }

    private static void ResponseTimeData(ThreadCB thread)
    {
        Long stTime, endTime, rsTime;
        double throughPut;
        
        if(!endMap.containsKey(thread.getID()))
        {
            endMap.put(thread.getID(), HClock.get());
            stTime = startMap.get(thread.getID());
            endTime = endMap.get(thread.getID());
            rsTime = endTime-stTime;
            
            MyOut.print(thread.getID(),"Thread ID : "+thread.getID());
            MyOut.print(rsTime,"Response time of thread : "+rsTime);
        }
    }
    
    static double throughPut;
    private static void ThroughputData(ThreadCB thread)
    {
         if(HClock.get() == 0)
         {
            throughPut = 0;
         }
         else
         {
            throughPut = (double)ThreadFinish*1000.0/(double)HClock.get();
         }
         MyOut.print(throughPut,"Throughput : "+throughPut);
    }
    
    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
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
