package osp.Ports;

import osp.Utilities.*;
import osp.IFLModules.*;

/**
   Class Message used by PortCB. No actual data is stored in a message -- 
   just an ID number and the message length.

   @OSPProject Ports
*/
public class Message extends IflMessage
{
    /**
       Constructor setting message length.
       Must contain the following as its first statement:

	   super(length);

       @param length of the message

       @OSPProject Ports
    */
    public Message(int length)
    {
       super(length);
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
