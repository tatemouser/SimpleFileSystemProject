// Data structures for the file system
import java.util.ArrayList;

// 1. Volume Control Block
class VolumeControlBlock {
    private int numBlocks = 512;       // Given: disk has 512 blocks
    private int blockSize = 2048;      // Given: 2K (2048 bytes) per block
    private int freeBlockCount;        // Number of free blocks available
    private boolean[] freeBlockMap;    // Bitmap to track free blocks

    public VolumeControlBlock() {
        freeBlockCount = numBlocks - 1; // Reserve block 0 for volume control
        freeBlockMap = new boolean[numBlocks];
        
        // Initialize all blocks as free except block 0 (used for VCB)
        freeBlockMap[0] = true; // Mark as used
        for (int i = 1; i < numBlocks; i++) {
            freeBlockMap[i] = false; // Mark as free
        }
    }
    
    // Allocate contiguous blocks for a file
    public int allocateBlocks(int numBlocksNeeded) {
        if (numBlocksNeeded > freeBlockCount) {
            return -1; // Not enough space
        }
        
        // Find a contiguous segment of free blocks
        for (int start = 1; start <= numBlocks - numBlocksNeeded; start++) {
            boolean canAllocate = true;
            
            // Check if the next numBlocksNeeded blocks are free
            for (int i = 0; i < numBlocksNeeded; i++) {
                if (freeBlockMap[start + i]) {
                    canAllocate = false;
                    break;
                }
            }
            
            if (canAllocate) {
                // Allocate these blocks
                for (int i = 0; i < numBlocksNeeded; i++) {
                    freeBlockMap[start + i] = true;
                }
                freeBlockCount -= numBlocksNeeded;
                return start; // Return starting block number
            }
        }
        
        return -1; // Could not find contiguous blocks
    }
    
    // Free blocks when a file is deleted
    public void freeBlocks(int startBlock, int numBlocksToFree) {
        for (int i = 0; i < numBlocksToFree; i++) {
            if (startBlock + i < numBlocks) {
                freeBlockMap[startBlock + i] = false;
            }
        }
        freeBlockCount += numBlocksToFree;
    }
    
    public int getFreeBlockCount() {
        return freeBlockCount;
    }
}

// 2. File Control Block (FCB)
class FCB {
    private int fileSize;      // Size in number of blocks
    private int startBlock;    // Pointer to first data block
    
    public FCB(int size, int startBlock) {
        this.fileSize = size;
        this.startBlock = startBlock;
    }
    
    public int getFileSize() {
        return fileSize;
    }
    
    public int getStartBlock() {
        return startBlock;
    }
}

// 3. Directory Entry
class DirectoryEntry {
    private String fileName;
    private int startBlock;
    private int fileSize;
    
    public DirectoryEntry(String name, int startBlock, int size) {
        this.fileName = name;
        this.startBlock = startBlock;
        this.fileSize = size;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public int getStartBlock() {
        return startBlock;
    }
    
    public int getFileSize() {
        return fileSize;
    }
}

// 4. System-wide Open File Table Entry
class SystemFileTableEntry {
    private String fileName;
    private FCB fcb;
    private int openCount;  // Number of processes that have this file open
    
    public SystemFileTableEntry(String name, FCB fcb) {
        this.fileName = name;
        this.fcb = fcb;
        this.openCount = 1;  // Initially opened by one process
    }
    
    public void incrementOpenCount() {
        openCount++;
    }
    
    public int decrementOpenCount() {
        return --openCount;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public FCB getFCB() {
        return fcb;
    }
    
    public int getOpenCount() {
        return openCount;
    }
}

// 5. Per-process Open File Table Entry
class ProcessFileTableEntry {
    private String fileName;
    private int fileHandle;  // Index in the system-wide open file table
    
    public ProcessFileTableEntry(String name, int handle) {
        this.fileName = name;
        this.fileHandle = handle;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public int getFileHandle() {
        return fileHandle;
    }
}

// Main File System class
public class FileSystem {
    private VolumeControlBlock vcb;
    private ArrayList<DirectoryEntry> directory;
    private ArrayList<SystemFileTableEntry> systemOpenFileTable;
    private ArrayList<ArrayList<ProcessFileTableEntry>> processOpenFileTables;
    
    // Simulated disk memory (1MB = 1024KB = 1024 * 1024 bytes)
    private byte[] disk;
    
    public FileSystem() {
        vcb = new VolumeControlBlock();
        directory = new ArrayList<>();
        systemOpenFileTable = new ArrayList<>();
        processOpenFileTables = new ArrayList<>();
        
        // Initialize disk (1MB)
        disk = new byte[1024 * 1024];
        
        // Add an empty process file table for the first process
        processOpenFileTables.add(new ArrayList<>());
    }
    
    // Create a new process (for simulation)
    public int createProcess() {
        processOpenFileTables.add(new ArrayList<>());
        return processOpenFileTables.size() - 1;
    }
    
    // Part 1: Implement basic file operations
    
    // 1. Create a file with specified size (in blocks)
    public boolean create(String fileName, int size) {
        // Check if file already exists
        for (DirectoryEntry entry : directory) {
            if (entry.getFileName().equals(fileName)) {
                System.out.println("File " + fileName + " already exists.");
                return false;
            }
        }
        
        // Allocate blocks for the file
        int startBlock = vcb.allocateBlocks(size);
        if (startBlock == -1) {
            System.out.println("Not enough space for file " + fileName);
            return false;
        }
        
        // Add to directory
        directory.add(new DirectoryEntry(fileName, startBlock, size));
        System.out.println("File " + fileName + " created with " + size + " blocks starting at block " + startBlock);
        return true;
    }
    
    // 2. Open a file and update tables
    public int open(int processId, String fileName) {
        // Check if the file exists in directory
        DirectoryEntry fileEntry = null;
        for (DirectoryEntry entry : directory) {
            if (entry.getFileName().equals(fileName)) {
                fileEntry = entry;
                break;
            }
        }
        
        if (fileEntry == null) {
            System.out.println("File " + fileName + " not found.");
            return -1;
        }
        
        // Check if file is already open in system-wide table
        int systemTableIndex = -1;
        for (int i = 0; i < systemOpenFileTable.size(); i++) {
            if (systemOpenFileTable.get(i).getFileName().equals(fileName)) {
                systemTableIndex = i;
                systemOpenFileTable.get(i).incrementOpenCount();
                break;
            }
        }
        
        // If not open, add to system-wide table
        if (systemTableIndex == -1) {
            FCB fcb = new FCB(fileEntry.getFileSize(), fileEntry.getStartBlock());
            systemOpenFileTable.add(new SystemFileTableEntry(fileName, fcb));
            systemTableIndex = systemOpenFileTable.size() - 1;
        }
        
        // Add to process's open file table
        ArrayList<ProcessFileTableEntry> processTable = processOpenFileTables.get(processId);
        
        // Check if the file is already open by this process
        for (ProcessFileTableEntry entry : processTable) {
            if (entry.getFileName().equals(fileName)) {
                System.out.println("File " + fileName + " is already open by process " + processId);
                return entry.getFileHandle();
            }
        }
        
        // Add to process's table
        processTable.add(new ProcessFileTableEntry(fileName, systemTableIndex));
        
        System.out.println("File " + fileName + " opened by process " + processId);
        return systemTableIndex;
    }
    
    // 3. Close a file and update tables
    public boolean close(int processId, String fileName) {
        // Find file in the process's open file table
        ArrayList<ProcessFileTableEntry> processTable = processOpenFileTables.get(processId);
        int processEntryIndex = -1;
        
        for (int i = 0; i < processTable.size(); i++) {
            if (processTable.get(i).getFileName().equals(fileName)) {
                processEntryIndex = i;
                break;
            }
        }
        
        if (processEntryIndex == -1) {
            System.out.println("File " + fileName + " is not open by process " + processId);
            return false;
        }
        
        // Get system-wide table index
        int systemTableIndex = processTable.get(processEntryIndex).getFileHandle();
        
        // Remove from process's open file table
        processTable.remove(processEntryIndex);
        
        // Decrement open count in system-wide table
        int remainingOpenCount = systemOpenFileTable.get(systemTableIndex).decrementOpenCount();
        
        // If no more processes have the file open, remove from system-wide table
        if (remainingOpenCount == 0) {
            systemOpenFileTable.remove(systemTableIndex);
            
            // Update file handles in all process tables
            for (ArrayList<ProcessFileTableEntry> pTable : processOpenFileTables) {
                for (ProcessFileTableEntry entry : pTable) {
                    if (entry.getFileHandle() > systemTableIndex) {
                        // Adjust handle for entries after the removed one
                        int newHandle = entry.getFileHandle() - 1;
                        entry = new ProcessFileTableEntry(entry.getFileName(), newHandle);
                    }
                }
            }
        }
        
        System.out.println("File " + fileName + " closed by process " + processId);
        return true;
    }
    
    // 4. Read a file
    public byte[] read(int processId, String fileName) {
        // Find file in the process's open file table
        ArrayList<ProcessFileTableEntry> processTable = processOpenFileTables.get(processId);
        int systemTableIndex = -1;
        
        for (ProcessFileTableEntry entry : processTable) {
            if (entry.getFileName().equals(fileName)) {
                systemTableIndex = entry.getFileHandle();
                break;
            }
        }
        
        if (systemTableIndex == -1) {
            System.out.println("File " + fileName + " is not open by process " + processId);
            return null;
        }
        
        // Get FCB from system-wide table
        FCB fcb = systemOpenFileTable.get(systemTableIndex).getFCB();
        int startBlock = fcb.getStartBlock();
        int fileSize = fcb.getFileSize();
        
        // Calculate total file size in bytes
        int fileSizeBytes = fileSize * 2048; // blockSize is 2K
        
        // Read data from disk
        byte[] data = new byte[fileSizeBytes];
        int startByte = startBlock * 2048;
        
        // Copy data from disk to buffer
        System.arraycopy(disk, startByte, data, 0, fileSizeBytes);
        
        System.out.println("Read " + fileSizeBytes + " bytes from file " + fileName);
        return data;
    }
    
    // 5. Write to a file
    public boolean write(int processId, String fileName, byte[] data) {
        // Find file in the process's open file table
        ArrayList<ProcessFileTableEntry> processTable = processOpenFileTables.get(processId);
        int systemTableIndex = -1;
        
        for (ProcessFileTableEntry entry : processTable) {
            if (entry.getFileName().equals(fileName)) {
                systemTableIndex = entry.getFileHandle();
                break;
            }
        }
        
        if (systemTableIndex == -1) {
            System.out.println("File " + fileName + " is not open by process " + processId);
            return false;
        }
        
        // Get FCB from system-wide table
        FCB fcb = systemOpenFileTable.get(systemTableIndex).getFCB();
        int startBlock = fcb.getStartBlock();
        int fileSize = fcb.getFileSize();
        
        // Calculate total file size in bytes
        int fileSizeBytes = fileSize * 2048; // blockSize is 2K
        
        // Check if data fits in the file
        if (data.length > fileSizeBytes) {
            System.out.println("Data is too large for file " + fileName);
            return false;
        }
        
        // Write data to disk
        int startByte = startBlock * 2048;
        
        // Copy data from buffer to disk
        System.arraycopy(data, 0, disk, startByte, data.length);
        
        System.out.println("Wrote " + data.length + " bytes to file " + fileName);
        return true;
    }
}