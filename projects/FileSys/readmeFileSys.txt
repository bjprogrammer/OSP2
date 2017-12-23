FileSys: The File System 
Group-4

This module provides  logical layer of Input output infrastructure in OS.Implements the ﬁle system including basic ﬁle operations and directory structures

It includes five classes: 
INode: tracks the space allocation to files
MountTable: Maps physical devices to files
OpenFile: Provides functionality to manipulate open file handles (including the read() and write())
DirectoryEntry: this has directory structures
FileSys: Facilitates set of operations, such as create() and delete(), on non-open files. 
