# OSP2
OSP2 is a simulator(written in java) that gives the illusion of a computer system with a dynamically evolving collection of user processes to be multi-programmed.

OSP2 consists of number of modules, each of which performs a basic operating systems service.Each module has its reference implementation. 
User task is to rewrite one module code and run it along with other modules reference code.The simulator “understands” its interaction with the other modules in that it can often detect an erroneous response by a module to a simulated event. In such cases, the simulator will gracefully terminate execution of the program by delivering a meaningful error message to the user, indicating where the error might be found. This facility serves both as a debugging tool for the student and as teaching tool for the instructor, as it ensures that student programs acceptable to the simulator are virtually bug-free

We have implemented following seven modules in our OSP2 project - 
• Management of tasks 
• Management and scheduling of threads
• Virtual memory management
• File system 
• Scheduling of disks(Device) 
• Resource Management 
• Inter-process communication(Port) 

We have performed detailed analysis by varying different parameters of a particular module in each run. Please go through report to find these observations.
