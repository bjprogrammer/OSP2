package osp.FileSys;

import java.util.Enumeration;
import java.util.Hashtable;
import osp.IFLModules.IflDirectoryEntry;
import osp.FileSys.INode;

import osp.FileSys.FileSys;

/**
 * Implements the file system directory entry.
 * 
 * @OSPProject FileSys
 */
public class DirectoryEntry extends IflDirectoryEntry {
	private static Hashtable<String, DirectoryEntry> directoryEntries;

	static void init() {
		directoryEntries = new Hashtable<String, DirectoryEntry>();
	}

	public static INode do_getINodeOf(String string) {
		DirectoryEntry directoryEntry = DirectoryEntry.getEntry(string);
		if (directoryEntry == null) {
			return null;
		}
		return directoryEntry.getINode();
	}

	/**
	 * Creates a new directory entry for an already existing inode. This
	 * constructor must have
	 * 
	 * super(pathname,type,inode);
	 * 
	 * as its first statement.
	 * 
	 * @param pathname
	 *            name of file or directory
	 * @param type
	 *            type of directory entry: file or directory
	 * @param inode
	 *            file's inode
	 * 
	 * @OSPProject FileSys
	 */
	public DirectoryEntry(String string, int n, INode iNode) {
		super(string, n, iNode);
	}

	private static DirectoryEntry getEntry(String string) {
		DirectoryEntry directoryEntry = (DirectoryEntry) ((Object) directoryEntries.get(FileSys.pathToDir(string)));
		if (directoryEntry != null) {
			return directoryEntry;
		}
		return (DirectoryEntry) ((Object) directoryEntries.get(FileSys.pathToFile(string)));
	}

	static DirectoryEntry removeEntry(String string) {
		DirectoryEntry directoryEntry = (DirectoryEntry) ((Object) directoryEntries
				.remove(string = FileSys.pathToDir(string)));
		if (directoryEntry != null) {
			return directoryEntry;
		}
		string = FileSys.pathToFile(string);
		return (DirectoryEntry) ((Object) directoryEntries.remove(string));
	}

	static boolean checkIfContains(String string) {
		return directoryEntries.containsKey(string);
	}

	static void addEntry(DirectoryEntry directoryEntry) {
		directoryEntries.put(directoryEntry.getPathname(), directoryEntry);
	}

	static Enumeration<DirectoryEntry> getElements() {
		return directoryEntries.elements();
	}

}