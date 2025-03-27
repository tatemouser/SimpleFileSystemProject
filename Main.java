import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Create the file system
        FileSystem fs = new FileSystem();
        
        // Use CountDownLatch to synchronize threads
        CountDownLatch latch = new CountDownLatch(1);
        
        // Thread for Process 1
        Thread p1 = new Thread(() -> {
            try {
                System.out.println("Process 1 Starting:");
                
                // 1. Create file1
                fs.create("file1", 2);
                
                // 2. Write to file1
                String content1 = "This is content for file1 in Process 1";
                fs.open(1, "file1");
                fs.write(1, "file1", content1.getBytes());
                // 3. Close file1
                fs.close(1, "file1");
                
                // 4. Create file2
                fs.create("file2", 3);
                
                // 5. Write to file2
                String content2 = "This is content for file2 in Process 1";
                fs.open(1, "file2");
                fs.write(1, "file2", content2.getBytes());
                // 6. Close file2
                fs.close(1, "file2");
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread for Process 2
        Thread p2 = new Thread(() -> {
            try {
                // Wait for Process 1 to complete
                latch.await();
                
                System.out.println("\nProcess 2 Starting:");
                
                // 7. Open file1
                fs.open(2, "file1");
                
                // 8. Read file1, print to screen
                byte[] readData1 = fs.read(2, "file1");
                System.out.println("Process 2 reading file1: " + new String(readData1));
                
                // 9. Close file1
                fs.close(2, "file1");
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread for Process 3
        Thread p3 = new Thread(() -> {
            try {
                // Wait for Process 1 to complete
                latch.await();
                
                System.out.println("\nProcess 3 Starting:");
                
                // 10. Open file2
                fs.open(3, "file2");
                
                // 11. Read file2, print to screen
                byte[] readData2 = fs.read(3, "file2");
                System.out.println("Process 3 reading file2: " + new String(readData2));
                
                // 12. Close file2
                fs.close(3, "file2");
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Start threads
        p1.start();
        p2.start();
        p3.start();
        
        // Wait for all threads to complete
        p1.join();
        p2.join();
        p3.join();
    }
}