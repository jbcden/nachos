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
  private Condition2 messageCond;
  private Condition2 speakCond;
  private Condition2 listenCond;
  private int speakersWaiting; 
  private int listenersWaiting; 
  private int message;
  private int written;

  /**
   * Allocate a new communicator.
   */
  public Communicator() {
    Lock lock = new Lock();
    this.lock = lock;
    this.messageCond = new Condition2(lock);
    this.speakCond = new Condition2(lock);
    this.listenCond = new Condition2(lock);
    this.speakersWaiting = 0;
    this.listenersWaiting = 0;
    this.message = -1;
    this.written = 1; //indicates we should write the message now
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

    if (speakersWaiting > 0) { // only one speaker should be setting message at a time
      /* System.out.println("SO MANY SLEEPS"); */
      messageCond.sleep();
      listenCond.wake();
    }
    /* System.out.println("SETTING MESSAGE HERE"); */
    message = word; // store the message we want to pass
    written = 1;

    if (listenersWaiting > 0) { // We can pair this thread up with a listening thread
      /* System.out.println("SpeakTest: Speaking the word " + word); */
      /* System.out.println("WAKE LISTENER"); */
      listenCond.wake(); // wake up a listening thread
      listenersWaiting--;
    } else { // No threads wanting to listen, so wait for one
      speakersWaiting++; // There will be always at least the current thread waiting to speak
      /* System.out.println("ABOUT TO SLEEP SPEAKER"); */
      speakCond.sleep(); // sleep so that a listener thread can take control
    }
    /* System.out.println("END OF SPEAKER"); */

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

    if ( (speakersWaiting > 0 && listenersWaiting > 0) || written != 1) { //let waiting listeners/speakers do their thing
      listenCond.sleep();
    }

    /* System.out.println("THAT?"); */
    messageCond.wake();
    /* System.out.println("THIS?"); */
    written = 0;

    if (speakersWaiting > 0) { // We can pair this thread up with a speaking thread
      /* System.out.println("WAKE SPEAKER"); */
      speakCond.wake(); // wake up a speaking thread
      speakersWaiting--;
    } else { // No threads wanting to speak, so wait for one
      /* System.out.println("SLEEP LISTENER"); */
      listenersWaiting++; // There will be always at least the current thread waiting to listen
      listenCond.sleep(); // sleep so that a speaker thread can take control
    }

    /* System.out.println("END LISTENER"); */

    lock.release();
    return message;
  }


  /****************************** Test classes/methods ******************************/

  private static class SpeakTest implements Runnable {
    SpeakTest(Communicator speaker, int word) {
      this.speaker = speaker;
      this.word = word;
    }

    public void run() {
      //printThread("SpeakTest.run()");
      speaker.speak(word);
      System.out.println("SpeakTest: Speaking the word " + word);
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
    int speakers = 4;
    int listeners = 5;

    KThread[] speakersArray = new KThread[speakers];
    KThread[] listenersArray = new KThread[listeners];

    for (int i = 0; i<speakers; i++) {
      speakersArray[i] = new KThread(new SpeakTest(comm, i)).setName("speak_thread_"+i);
    }

    for (int j = 0; j<listeners; j++) {
      listenersArray[j] = new KThread(new ListenTest(comm)).setName("listener_thread_"+j);
    }

    for (int i = 0; i<speakers; i++) {
      speakersArray[i].fork();
    }

    for (int j = 0; j<listeners; j++) {
      listenersArray[j].fork();
    }

    for (int i = 0; i<listeners+speakers; i++) {
      KThread.currentThread().yield();
      KThread.currentThread().yield();
    }
  }

  public static void selfTest2() {
    Communicator comm = new Communicator();  
    KThread speak_thread = new KThread(new SpeakTest(comm, 9001)).setName("speak_thread");
    KThread listen_thread = new KThread(new ListenTest(comm)).setName("listen_thread");
    KThread listen_thread2 = new KThread(new ListenTest(comm)).setName("listen_thread2");
    KThread listen_thread3 = new KThread(new ListenTest(comm)).setName("listen_thread3");

    // This seems to work fine
    /* speak_thread.fork(); */
    /* listen_thread.fork(); */

    // However, if I do
    listen_thread.fork();
    listen_thread2.fork();
    listen_thread3.fork();
    speak_thread.fork();
    // the listening thread never wakes back up :(


    KThread.currentThread().yield();
    KThread.currentThread().yield();
    KThread.currentThread().yield();
    KThread.currentThread().yield();
    KThread.currentThread().yield();
    KThread.currentThread().yield();
    KThread.currentThread().yield();
  }

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
