package osp.FileSys;


import osp.IFLModules.IflOpenFile;
import osp.IFLModules.SystemEvent;
import osp.FileSys.INode;
import osp.FileSys.MountTable;
import osp.FileSys.DirectoryEntry;
import osp.FileSys.FileSys;
import osp.IFLModules.Event;
import osp.Threads.ThreadCB;
import osp.Utilities.GenericList;


import osp.Tasks.TaskCB;
import osp.Memory.PageTableEntry;

import osp.Devices.Device;
import osp.Devices.IORB;
/**
 * Implements the open file data structure. Read(), write(), and close() use
 * this class as a file handle.
 * 
 * @OSPProject FileSys
 * 
 */
public class OpenFile extends IflOpenFile {
	static GenericList masterFileList;

	

	/**
	 * Creates new instances of OpenFile. This constructor must have
	 * 
	 * super(inode, task);
	 * 
	 * as its first statement.
	 * 
	 * @param inode
	 *            inode associated with the open file
	 * @param task
	 *            owner task
	 * 
	 * @OSPProject FileSys
	 */
	public OpenFile(INode iNode, TaskCB taskCB) {
		super(iNode, taskCB);
	}
	static void init() {
		masterFileList = new GenericList();
	}

	/**
	 * Opens an existing file for reading and writing.
	 * 
	 * Opens the file with the specified pathname if it exists. Increments the
	 * count of open files for inode, creates an <code>OpenFile</code> object
	 * and returns it or null if the file for some reason cannot be opened,
	 * e.g., if it does not exist. The system call must also insert this
	 * <code>OpenFile</code> object into the list of files opened by the task
	 * that owns the thread that opened the file. This is done using
	 * <code>addFile()</code>, which is a method of TaskCB.
	 * 
	 * Mount points cannot be open.
	 * 
	 * @param filename
	 *            name of file to open.
	 * @param task
	 *            to which the file should belong
	 * @return an OpenFile object or null in case of failure.
	 * 
	 * @OSPProject FileSys
	 */



public int do_write(int n, PageTableEntry pageTableEntry, ThreadCB threadCB) {
		int phyAddr;
		int n3;
		if (!masterFileList.contains((Object) this)) {
			return FAILURE;
		}
		if (n < 0) {
			return FAILURE;
		}
		SystemEvent systemEvent = new SystemEvent("FileWrite");
		threadCB.suspend((Event) systemEvent);
		INode iNode = this.getINode();
		int devId = iNode.getDeviceID();
		if (n + 1 - iNode.getBlockCount() > INode.iflGetNumberOfFreeBlocks(devId)) {
			systemEvent.notifyThreads();
			return FAILURE;
		}
		if (pageTableEntry == null) {
			systemEvent.notifyThreads();
			return FAILURE;
		}
		int allocation = Math.max(n + 1, this.getINode().getBlockCount());
		for (phyAddr = iNode.getBlockCount() + 1; phyAddr <= allocation; ++phyAddr) {
			n3 = iNode.allocateFreeBlock();
			if (n3 != -1)
				continue;
		}
		phyAddr = iNode.getPhysicalAddress(n);
		IORB iORB = new IORB(threadCB, pageTableEntry, phyAddr, devId, FileWrite, this);
		if (iORB != null) {
			int n6 = Device.get((int) devId).enqueueIORB(iORB);
			if (n6 == SUCCESS) {
				threadCB.suspend((Event) iORB);
			}
			if (threadCB.getStatus() == 22) {
				return FAILURE;
			}
		}
		systemEvent.notifyThreads();
		return SUCCESS;
	}

	/**
	 * Reads from an opened file.
	 * 
	 * This system call reads an amount of data equal to the size of the memory
	 * page and device block (both sizes are equal in <i>OSP</i> by convention)
	 * from the file at block number <code>fileBlockNumber</code> and writes the
	 * data to the virtual memory page <code>memoryPage</code>.
	 * <code>File</code> must be a valid open file. The file should have more
	 * blocks than <code>fileBlockNumber</code>; <code>fileBlockNumber</code>
	 * must be greater or equal to zero, and the <code>memoryPage</code> must be
	 * not null.
	 * 
	 * An <code>IORB</code> object must be created using a 6-argument
	 * <code>IORB()</code> constructor, the <code>IORB</code> must be enqueued
	 * with a call to <code>Device.get(deviceID).enqueueIORB()</code>.
	 * 
	 * The current thread (<code>MMU.getPTBR().getTask().getCurrentThread())
	must be suspended on the IORB by calling <code>suspend()</code> until the
	 * I/O is finished.
	 * 
	 * If at least one of the listed conditions does not hold, the system call
	 * must not create an IORB object or suspend a thread but simply return
	 * FAILURE.
	 * 
	 * @param fileBlockNumber
	 *            block number within the file
	 * @param memoryPage
	 *            memory page to write to.
	 * @param thread
	 *            thread that invokes the I/O
	 * @return SUCCESS if the parameters are OK and the I/O operation has been
	 *         started. FAILURE if some of the parameter set is wrong.
	 * 
	 * @OSPProject FileSys
	 */
	public int do_read(int n, PageTableEntry pageTableEntry, ThreadCB threadCB) {
		if (!masterFileList.contains((Object) this)) {
			return FAILURE;
		}
		if (n < 0 || n > this.getINode().getBlockCount() - 1) {
			return FAILURE;
		}
		SystemEvent systemEvent = new SystemEvent("FileRead");
		threadCB.suspend((Event) systemEvent);
		INode iNode = this.getINode();
		int devID = iNode.getDeviceID();
		if (pageTableEntry == null) {
			systemEvent.notifyThreads();
			return FAILURE;
		}
		int phyAddr = iNode.getPhysicalAddress(n);
		IORB iORB = new IORB(threadCB, pageTableEntry, phyAddr, devID, FileRead, this);

		if (iORB != null) {
			int n5 = Device.get((int) devID).enqueueIORB(iORB);
			if (n5 == SUCCESS) {
				threadCB.suspend((Event) iORB);
			}
			if (threadCB.getStatus() == 22) {
				return FAILURE;
			}
		}
		systemEvent.notifyThreads();
		return SUCCESS;
	}
	
		public static OpenFile do_open(String string, TaskCB taskCB) {
		String string2 = FileSys.normalize(string);
		if (!FileSys.isInvalidFile(string2)) {
			return null;
		}
		if (MountTable.isMountPoint((String) string2)) {
			return null;
		}
		INode iNode = DirectoryEntry.getINodeOf((String) string2);
		OpenFile openFile = new OpenFile(iNode, taskCB);
		iNode.incrementOpenCount();
		masterFileList.insert((Object) openFile);
		taskCB.addFile(openFile);

		return openFile;
	}
	
	/**
	 * Closes an open file.
	 * 
	 * This system call closes the open file and destroys the
	 * <code>OpenFile</code> object by removing it from the file system internal
	 * data structures. If all directory entries associated with this i-node are
	 * deleted, the i-node must be destroyed and its device blocks must be
	 * marked as free. The file cannot be closed -- and the system call must
	 * return FAILURE -- if there is an unfinished I/O operation going on with
	 * this file. Such situation is indicated by the iorbCount field of the
	 * OpenFile object being non-zero. In this case the system call must only
	 * set the closePending field of the object to <code>true</code>.
	 * 
	 * On successful completion, should set closePending to false and use
	 * <code>removeFile()</code> to remove the file from the corresponding task.
	 * 
	 * @return SUCCESS or FAILURE if either file is not open or outstanding
	 *         IORBs exist.
	 * 
	 * @OSPProject FileSys
	 */
	
	
	
	
	
	public int do_close() {
		if (!masterFileList.contains((Object) this)) {
			return FAILURE;

		}
		if (this.getIORBCount() > 0) {
			this.closePending = true;
			return FAILURE;
		}

		masterFileList.remove((Object) this);
		this.getINode().decrementOpenCount();
		INode iNode = this.getINode();
		if (iNode.getLinkCount() + iNode.getOpenCount() == 0) {
			iNode.releaseBlocks();
			INode.removeInode(iNode);
		}

		this.closePending = false;
		this.getTask().removeFile(this);
		return SUCCESS;
	}
	}

	/**
	 * Writes to an opened file.
	 * 
	 * This system call is very similar to <code>do_read()</code> except that
	 * the data goes in the opposite direction: from memory page to the open
	 * file. If the file has fewer blocks than <code>fileBlockNumber</code>, it
	 * grows unless there is no more space on the device, in which case FAILURE
	 * is returned.
	 * 
	 * Waits for the I/O operation to complete by suspending the thread on IORB.
	 * 
	 * @param fileBlockNumber
	 *            block number to which to write
	 * @param memoryPage
	 *            memory page to read from
	 * @param thread
	 *            thread that invoked this operation
	 * @return SUCCESS if the parameters are OK and the IORB has been
	 *         successfully created and enqueued. FAILURE if some of the
	 *         parameters are wrong.
	 * 
	 * @OSPProject FileSys
	 */
	