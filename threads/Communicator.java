package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

/*
Part 3: 40 pts, 40 lines, due March 9th
Implement synchronous send and receive of one word messages (also
known as Ada-style rendezvous), using condition variables (don't use semaphores!). Implement the Communicator
class with operations, void speak(int word) and int listen(). speak() atomically waits until listen() is called on the
same Communicator object, and then transfers the word over to listen(). Once the transfer is made, both can
return. Similarly, listen() waits until speak() is called, at which point the transfer is made, and both can return
(listen() returns the word). Your solution should work even if there are multiple speakers and listeners for the
same Communicator (note: this is equivalent to a zero-length bounded buffer; since the buffer has no room, the
producer and consumer must interact directly, requiring that they wait for one another). Each communicator should
only use exactly one lock. If you're using more than one lock, you're making things too complicated.
*/



    /** Class variables **/
    private Lock lock;
    private Condition2 speakCond;
    private Condition2 listenCond;
    private int speakersWaiting; 
    private int listenersWaiting; 
    private int message;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	    Lock lock = new Lock();
    	    this.lock = lock;
    	    this.speakCond = new Condition2(lock);
    	    this.listenCond = new Condition2(lock);
    	    this.speakersWaiting = 0;
    	    this.listenersWaiting = 0;
    	    this.message = -1;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) { 
    	    lock.acquire();
    	    
    	    speakersWaiting++; // There will be always at least the current thread waiting to speak
    	    
    	    if (listenersWaiting > 0) { // We can pair this thread up with a listening thread
    	    	    message = word; // store the message we want to pass
    	    	    listenCond.wake(); // wake up a listening thread
    	    	    /* KThread.currentThread().yield(); // yield to the waiting listener thread */
    	    } else { // No threads wanting to listen, so wait for one
    	    	    System.out.println("foo");
    	    	    message = word; // store the message we want to pass
    	    	    System.out.println("We should switch from the speaking thread to the listening thread in the next line.");
    	    	    speakCond.sleep(); // sleep so that a listener thread can take control
    	    	    System.out.println("We should have switched from the speaking thread to the listening thread in the previous line.");
    	    	    message = word; // store the message we want to pass
    	    	    System.out.println("bar");
    	    }
    	    
    	    //KThread.currentThread().yield(); // Does this go here?
    	    
    	    printThread("speak() just finished running.");
    	    
    	    speakersWaiting--; // The current thread is done speaking
    	   
    	    lock.release();
  
    }
     

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	    lock.acquire();
    	    
    	    int word = -1; // sentinel value for now
    	    listenersWaiting++; // There will be always at least the current thread waiting to listen
    	    
    	    if (speakersWaiting > 0) { // We can pair this thread up with a speaking thread
    	    	    word = message;
    	    	    speakCond.wake(); // wake up a speaking thread
    	    	    /* KThread.currentThread().yield(); // yield to the waiting speaker thread */
    	    } else { // No threads wanting to speak, so wait for one
    	    	    System.out.println("baz");
    	    	    word = message; // retrieve the message we want to pass
    	    	    System.out.println("We should switch from the listening thread to the speaking thread in the next line.");
    	    	    listenCond.sleep(); // sleep so that a speaker thread can take control
    	    	    System.out.println("We should have switched from the listening thread to the speaking thread in the previous line.");
    	    	    word = message; // retrieve the message we want to pass
    	    	    System.out.println("qux");
    	    }
    	    
    	    //KThread.currentThread().yield(); // Does this go here?
    	    
    	    printThread("listen() just finished running.");
    	    
    	    listenersWaiting--; // The current thread is done listening
    	    
    	    lock.release();
    	    return word;   
    }


    /****************************** Test classes/methods ******************************/
    
    private static class SpeakTest implements Runnable {
	SpeakTest(Communicator speaker, int word) {
	    this.speaker = speaker;
	    this.word = word;
	}
	
	public void run() {
	    //printThread("SpeakTest.run()");
	    System.out.println("SpeakTest: Speaking the word " + word);
	    speaker.speak(word);
	}

	private Communicator speaker;
	private int word;
    }


    private static class ListenTest implements Runnable {
	ListenTest(Communicator listener) {
	    this.listener = listener;
	}
	
	public void run() {
	    //printThread("ListenTest.run()");
	    int word = listener.listen();
	    System.out.println("ListenTest: Listened to word: " + word);
	}

	private Communicator listener;
    }


    public static void selfTest() {
    	Communicator comm = new Communicator();  
    	KThread speak_thread = new KThread(new SpeakTest(comm, 9001)).setName("speak_thread");
    	KThread listen_thread = new KThread(new ListenTest(comm)).setName("listen_thread");
    	
    	// This seems to work fine
    	//speak_thread.fork();
	//listen_thread.fork();
	
	// However, if I do
	listen_thread.fork();
	speak_thread.fork();
	// the listening thread never wakes back up :(
	
	
	KThread.currentThread().yield();
    }
    
    
    // example selfTest() method from Piazza
    /*
    public static void selfTest() {
	Communicator cmator1 = new Communicator();
	new KThread(new SpeakTest(cmator1, "speaker1")).setName("s1").fork();
	new KThread(new ListenTest(cmator1, "listener1")).setName("l1").fork();
	new KThread(new ListenTest(cmator1, "listener3")).setName("l3").fork();
	new KThread(new ListenTest(cmator1, "listener4")).setName("l4").fork();
	new ListenTest(cmator1, "listener2").run();
	// call KThread.currentThread().yield() a bunch of times if necessary
    }
    */
    
    
    /* For debugging purposes */
    public static void printThread(KThread thread, String message) {
    	System.out.println("(" + message + ") Thread name/status: < " + thread + " >");
    }
    
    public static void printThread(KThread thread) {
    	System.out.println("Thread name/status: < " + thread + " >");
    }
    
    public static void printThread(String message) {
    	System.out.println("(" + message + ") Thread name/status: < " + KThread.currentThread() + " >");
    }
    
    public static void printThread() {
    	System.out.println("Thread name/status: < " + KThread.currentThread() + " >");
    }


}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////





    /*
public void speak(int word) { 
    	    lock.acquire();
    	    boolean intStatus = Machine.interrupt().disable();
    	    
    	    speakersWaiting++;
    	    message = word; // Does this go up here?
    	    
    	    // If there's already a listener waiting, 
    	    // let a waiting speaker send the message
    	    if (listenersWaiting > 0) {
    	    	  
    	    	    listenersWaiting--;
    	   
    	    	    listenCond.wake();
    	    	    
    	    } else { // no listeners waiting, so wait for one
    	    	    
    	    	    speakCond.sleep();
    	    	    KThread.currentThread().yield(); // yield to the waiting listener thread
    	    	     
    	    }
    	    
    	    Machine.interrupt().restore(intStatus);
    	    lock.release();
    }
*/




    /*
public int listen() {
    	    lock.acquire();
    	    boolean intStatus = Machine.interrupt().disable();   
    	    int word = -1; // sentinel value for now
    	    
    	    listenersWaiting++;
    	    word = message; // Does this go up here?
    	    
    	    // If there's already a speaker waiting, 
    	    // immediately receive its message
    	    if (speakersWaiting > 0) {
    	    	   
    	    	    speakersWaiting--;
    	    	    //word = message;
    	    	    speakCond.wake();
    	    	       
    	    } else { // no speakers waiting, so wait for one
    	    	   
    	    	    listenCond.sleep();
    	    	    KThread.currentThread().yield(); // yield to the waiting speaker thread   
    	    }
    	    
   
    	    Machine.interrupt().restore(intStatus);
    	    lock.release();
    	    return word;
    }
*/



























