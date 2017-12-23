package osp.FileSys;

import osp.IFLModules.*;
import java.util.*;

import osp.Devices.*;

/**
 * This class implements a mount table -- an array of directory names associated
 * with logical devices.
 * 
 * Note that all methods in this class are static, so the mount table has to be
 * implemented as a static data structure.
 * 
 * @OSPProject FileSys
 */

public class MountTable extends IflMountTable {

	/**
	 * Returns the Id of the device where <code>pathname</code> resides.
	 * 
	 * @param pathname
	 *            a file or directory name to find the deviceID for
	 * @return deviceID of the device, where this file or dir resides, NONE if
	 *         the file device is not found. Since there is a root directory
	 *         with mount point that consists just of a DirSeparator, this
	 *         should only happen if the name does not start with the directory
	 *         separator symbol. This method checks the mount table and returns
	 *         the Id of the device that hosts the file with the given pathname.
	 * @OSPProject FileSys
	 */

	private static int getMatches(Vector<String> pathHier, Vector<String> mountHier) {
		int match = 0;
		for (int i = 0; i < mountHier.size(); i++) {
			String dirName = mountHier.get(i);
			if (pathHier.size() <= i) {
				return 0;
			} else if (!pathHier.get(i).equals(dirName)) {
				return 0;
			}
			match++;
		}
		return match;
	}

	public static int do_getDeviceID(String pathname) {
		Vector<String> pathHier = getPathHierarchy("root/" + pathname);
		// Compare mount point path with the pathname
		int tableSize = Device.getTableSize();
		int bestMatches = 0;
		int suggestMount = 0;
		for (int i = 0; i < tableSize; i++) {
			String mountPoint = getMountPoint(i);
			Vector<String> mountHier = getPathHierarchy("root/" + mountPoint);
			int matches = getMatches(pathHier, mountHier);
			if (matches > bestMatches) {
				bestMatches = matches;
				suggestMount = i;
			}
		}
		return suggestMount;

	}

	/**
	 * Returns true, if dirname is a mount point; false otherwise.
	 * 
	 * @param dirname
	 *            This method tells if dirname is a mountpoint of one of the
	 *            devices. It uses the method getMountPoint() internally.
	 * @OSPProject FileSys
	 */
	public static boolean do_isMountPoint(String dirname) {
		// your code goes here

		int tableSize = Device.getTableSize();
		for (int i = 0; i < tableSize; i++) {
			String mountPoint = IflMountTable.getMountPoint(i);
			if (mountPoint.contains(dirname)) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Get a list of string that hierarchically describe the pathname
	 */

	/**
	 * This method compares a mount point and a path, then determine how much
	 * they matches.
	 *
	 * @param pathHier
	 *            the hierarchical description of a path
	 * @param mountHier
	 *            the hierarchical description of a mount point
	 * @return the depth of identical path between them. 0 means not match. For
	 *         example, path /foo/bar/abc and mount /foo/bar will produce 2;
	 *         while the same path and mount /foo/bar/ab will pro- duce 0
	 *         because they do not match.
	 */
	private static Vector<String> getPathHierarchy(String pathname) {
		// Get hierarchical names of the path
		String[] pathSplit = pathname.split("/");
		Vector<String> hierarchy = new Vector<String>();
		for (String dirName : pathSplit) {
			if (dirName != null && dirName.length() > 0) {
				hierarchy.add(dirName);
			}
		}
		return hierarchy;
	}

}
