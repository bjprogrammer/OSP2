package osp.FileSys;

import osp.FileSys.MountTable;
import osp.IFLModules.IflINode;
import java.util.Vector;

import osp.Utilities.GenericList;
import osp.Devices.Device;

/**
 * Implements the Unix-like INode data structure, which keeps information about
 * the storage of the file, the number of hard links to it, the number of times
 * it is open, etc. Does NOT keep the name of the file: there can be several
 * hard links to the same INode.
 * 
 * A pathname identifies the file uniquely, but a file can have any number of
 * names. In fact, a file is represented by its inode (index node), which
 * contains information about the blocks allocated to the file. Pathnames are
 * associated with inodes through directory entries, but inodes themselves
 * contain no information about the names of the corresponding files. To
 * associate another name with a given file, a thread can create a hard link to
 * a file, which creates another association between a pathname and an inode.
 * 
 * 
 * openCount: The count of active open file handles associated with the inode.
 * It is obtained using getOpenCount() and changed via incrementOpenCount() and
 * decrementOpenCount().
 * 
 * blockCount: The number of blocks allocated to the file (the file size). This
 * item is obtained using getBlockCount() and set using setBlockCount(). device
 * ID: The device Id of the inode. It is obtained via the method getDeviceID().
 * 
 * @OSPProject FileSys
 */
public class INode extends IflINode {

	private Vector vectorList = new Vector();
	private static boolean[][] blockMappings;
	private static int[] freeBlocks;
	private static GenericList masterList;

	static void init() {
		int n;
		masterList = new GenericList();
		blockMappings = new boolean[Device.getTableSize()][];
		for (n = 0; n < blockMappings.length; ++n) {
			if (MountTable.getMountPoint((int) n) == null)
				continue;
			INode.blockMappings[n] = new boolean[Device.get((int) n).getNumberOfBlocks()];
		}
		freeBlocks = new int[Device.getTableSize()];
		for (n = 0; n < Device.getTableSize(); ++n) {
			INode.freeBlocks[n] = Device.get((int) n).getNumberOfBlocks();
		}
	}

	/**
	 * Creates a new inode on a specified device. This constructor must have
	 * 
	 * super(deviceId);
	 * 
	 * as its first statement.
	 * 
	 * @param deviceID
	 *            device number to create the inode on
	 * 
	 * @OSPProject FileSys
	 */
	public static int iflGetNumberOfFreeBlocks(int n) {
		return freeBlocks[n];
	}

	static void addInode(INode iNode) {
		masterList.append((Object) iNode);
	}

	public INode(int n) {
		super(n);
	}

	static void removeInode(INode iNode) {
		masterList.remove((Object) iNode);
	}

	/**
	 * Allocates a free block to inode and returns the block number of that
	 * block. Marks that block as used.
	 * 
	 * @return integer in the range from 0 to numberOfBlocks-1 if a free block
	 *         was found on the inode's device, NONE otherwise. When applied to
	 *         an inode object, allocates a free block to that inode and returns
	 *         the block number of that block. Marks the block as. Make sure
	 *         that the INode block count is set correctly (see the method
	 *         setBlockCount()). Returns NONE if the device has no free blocks.
	 * @OSPProject FileSys
	 */
	public int do_allocateFreeBlock() {
		int n = this.getDeviceID();
		if (n < 0 || n >= Device.getTableSize()) {
			return -1;
		}
		for (int i = 0; i < Device.get((int) n).getNumberOfBlocks(); ++i) {
			if (blockMappings[n][i])
				continue;
			INode.blockMappings[n][i] = true;
			Integer n2 = new Integer(i);
			this.vectorList.addElement(n2);
			this.setBlockCount(this.getBlockCount() + 1);
			int[] arrn = freeBlocks;
			int n3 = n;
			arrn[n3] = arrn[n3] - 1;
			return i;
		}
		return -1;
	}

	/**
	 * Tests whether a block on a device is free.
	 * 
	 * @param deviceID
	 *            device to test the block on
	 * @param block
	 *            number of block to test
	 * @return true if block is free, false if used or deviceID is invalid
	 * 
	 * @OSPProject FileSys
	 */
	public static boolean do_isFreeBlock(int n, int n2) {
		if (n2 < 0 || n2 >= Device.getTableSize()) {
			return false;
		}
		return !blockMappings[n2][n];
	}

	/**
	 * Release all blocks allocated to the given inode (i.e., make them free).
	 * 
	 * @param inode
	 *            to de-allocate free block on Releases all disk blocks occupied
	 *            by the inode. Make sure that the INode block count is set
	 *            correctly (setBlockCount()). Opposite of allocate:- 1) find
	 *            the block count. set to 0 - using setBlockCount() 2) mark all
	 *            the blocks as used. aQ as true 3) anything else???
	 * @OSPProject FileSys
	 */
	public void do_releaseBlocks() {
		Vector vector = this.vectorList;
		int n = this.getDeviceID();
		if (n < 0 || n >= Device.getTableSize()) {
			return;
		}
		if (vector == null) {
			return;
		}
		while (vector.size() > 0) {
			try {
				int n2 = (Integer) vector.elementAt(0);
				vector.removeElementAt(0);
				INode.blockMappings[n][n2] = false;
				int[] arrn = freeBlocks;
				int n3 = n;
				arrn[n3] = arrn[n3] + 1;
			} catch (Exception ex) {
			}
		}
		this.setBlockCount(0);
	}

	int getPhysicalAddress(int n) {
		return (Integer) this.vectorList.elementAt(n);
	}

}