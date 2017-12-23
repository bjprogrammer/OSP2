OSP-2 Project - "Devices"
Group-4

The Devices project implements certain functions of the device driver and of the basic I/O supervisor. Handles I/O requests for secondary storage devices such as disk 
drives. 

We implemented two disk scheduling algorithms: FIFO and SSTF.
Calculated total no. of head movements and response time of each iorb is appended in log file.

We performed two performance analysis in this module-
a.)Comparison of SSTF and FIFO in terms of total no. of head movements and average reponse time
b.)Analysis of collected response time of each IORB for three devices: device 0, device 1 and device 2 for each algorithm

We have appended results of these experiments in our report.