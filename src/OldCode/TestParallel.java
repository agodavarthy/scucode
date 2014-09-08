package OldCode;
import java.util.PriorityQueue;
import java.util.Queue;


public class TestParallel extends Thread {
	int id;
	int outVal;
	public TestParallel(int n, int outVal){
		id = n;
		outVal = outVal;
	}
	public void run(){
		try {
			System.out.println("Starting Thread " + id);
			Thread.sleep(6000);
			System.out.println("Completing Thread " + id);
			outVal = id * 2;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public static void main(String argv[]){
		Queue<String> queue = new PriorityQueue<String>();
		int expectedResults = 10;
		for(int i = 0; i < 10; i++){
			int outVal = 0;
			TestParallel t = new TestParallel(i + 1, outVal);
			t.start();
			
		}
        int receivedResults = 0;
        while (receivedResults < expectedResults) {
            if (!queue.isEmpty()) {
                System.out.println(queue.poll());
                receivedResults++;
            }
        }
	}
}
