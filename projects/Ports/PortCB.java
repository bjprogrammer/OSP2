package osp.Ports;

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.Utilities.*;

/**
   The studends module for dealing with ports. The methods 
   that have to be implemented are do_create(), 
   do_destroy(), do_send(Message msg), do_receive(). 


   @OSPProject Ports
*/

public class PortCB extends IflPortCB
{
    /**
       Creates a new port. This constructor must have

	   super();

       as its first statement.

       @OSPProject Ports
    */
    public PortCB()
    {
       super();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Ports
    */
    public static void init()
    {}

    /** 
        Sets the properties of a new port, passed as an argument. 
        Creates new message buffer, sets up the owner and adds the port to 
        the task's port list. The owner is not allowed to have more 
        than the maximum number of ports, MaxPortsPerTask.

        @OSPProject Ports
    */
	private int portBuffer;
	
    public static PortCB do_create()
    {
      PortCB port = new PortCB();
	  
      TaskCB task = MMU.getPTBR().getTask();
      if (task.getPortCount() >= MaxPortsPerTask) 
	  {
          return null;
      }
	  else
	  {
	      task.addPort(port);
		  port.portBuffer = 0;
          port.setTask(task);
          port.setStatus(PortLive);
         
          return port;
	  }
    }

    /** Destroys the specified port, and unblocks all threads suspended 
        on this port. Delete all messages. Removes the port from 
        the owners port list.
        @OSPProject Ports
    */
    public void do_destroy()
    {
      setStatus(PortDestroyed);
	  notifyThreads();
	  portBuffer = 0;
      getTask().removePort(this) ;
      setTask(null);
    }

    /**
       Sends the message to the specified port. If the message doesn't fit,
       keep suspending the current thread until the message fits, or the
       port is killed. If the message fits, add it to the buffer. If 
       receiving threads are blocked on this port, resume them all.

       @param msg the message to send.

       @OSPProject Ports
    */
    public int do_send(Message msg)
    {
      if (msg == null || (msg.getLength() > PortBufferLength))
      {       
        return FAILURE;
      }
    
      SystemEvent systemEvent = new SystemEvent("Sender threads waiting for destination port to free space in messagebuffer");
	  ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread();
      thread.suspend(systemEvent);
	  
      while ((portBuffer + msg.getLength() > PortBufferLength))
      {
        if (thread.getStatus() == ThreadKill)
        {
          removeThread(thread);
          return FAILURE;
        }
		else if (getStatus() != PortLive)
        {
		  systemEvent.notifyThreads();
          return FAILURE;
        }
        thread.suspend(this);
      }
	  
      if (thread.getStatus() == ThreadKill)
      {
          removeThread(thread);
          return FAILURE;
      }
      else if (getStatus() != PortLive)
      {
		  systemEvent.notifyThreads();
          return FAILURE;
      } 
	  else
	  {
        appendMessage(msg);
		if (portBuffer == 0) 
		{
          notifyThreads();
        } 
	    portBuffer += msg.getLength();
        systemEvent.notifyThreads();
        return SUCCESS;
	  }
    }
    /** Receive a message from the port. Only the owner is allowed to do this.
        If there is no message in the buffer, keep suspending the current 
	thread until there is a message, or the port is killed. If there
	is a message in the buffer, remove it from the buffer. If 
	sending threads are blocked on this port, resume them all.
	Returning null means FAILURE.

        @OSPProject Ports
    */
    public Message do_receive() 
    {
	  TaskCB task = MMU.getPTBR().getTask();
      if (getTask() != task) 
	  {
        return null;
      }
	  
      SystemEvent systemEvent = new SystemEvent("Receiver threads waiting(competing) to receive message in message buffer of destination port");
	  ThreadCB thread = task.getCurrentThread();
      thread.suspend(systemEvent);
	  
      while (getStatus() == PortLive && isEmpty() )
      {
        if (thread.getStatus() == ThreadKill)
        {
          removeThread(thread);
          return null;
        }
         else if (getStatus() != PortLive)
        {
	    	systemEvent.notifyThreads();
            return null;
        }
	  
        thread.suspend(this);
      }
	  
      if (thread.getStatus() == ThreadKill)
      {
        removeThread(thread);
        return null;
      }
      else if (getStatus() != PortLive)
      {
		systemEvent.notifyThreads();
        return null;
      }
	  else
      {
        Message message = removeMessage();
		this.portBuffer -= message.getLength();
        notifyThreads();
        systemEvent.notifyThreads();
        return message;
	  }
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Ports
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Ports
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
