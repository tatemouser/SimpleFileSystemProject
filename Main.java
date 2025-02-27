import java.util.ArrayList;

// Add this Main class in a separate file named Main.java
public class Main {
    public static void main(String[] args) {
        // Create the file system
        FileSystem fs = new FileSystem();
        
        // Test file operations
        System.out.println("Testing file system operations:");
        
        // Create some files
        fs.create("file1", 2);
        fs.create("file2", 3);
        
        // Process 0 is the default process
        int processId = 0;
        
        // Open files
        int file1Handle = fs.open(processId, "file1");
        int file2Handle = fs.open(processId, "file2");
        
        // Write to files
        String content1 = "This is content for file1";
        String content2 = "This is content for file2 with more data";
        fs.write(processId, "file1", content1.getBytes());
        fs.write(processId, "file2", content2.getBytes());
        
        // Read from files
        byte[] readData1 = fs.read(processId, "file1");
        byte[] readData2 = fs.read(processId, "file2");
        
        System.out.println("Reading file1: " + new String(readData1));
        System.out.println("Reading file2: " + new String(readData2));
        
        // Close files
        fs.close(processId, "file1");
        fs.close(processId, "file2");
        
        System.out.println("File operations completed successfully.");
    }
}