package nachos.threads;
import nachos.machine.*;
import java.util.TreeSet; // These might be made redundant
import java.util.HashSet; // by java.util.PriorityQueue
import java.util.Stack;
//import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Iterator;
/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
  /**
   * Allocate a new priority scheduler.
   */
  public PriorityScheduler() {
  }
  /**
   * Allocate a new priority thread queue.
   *
   * @param	transferPriority <tt>true</tt> if this queue should
   * transfer priority from waiting threads
   * to the owning thread.
   * @return	a new priority thread queue.
   */
  public ThreadQueue newThreadQueue(boolean transferPriority) {
    return new PriorityQueue(transferPriority);
  }

  public int getPriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
    return getThreadState(thread).getPriority();
  }

  public int getEffectivePriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
    return getThreadState(thread).getEffectivePriority();
  }

  public void setPriority(KThread thread, int priority) {
    Lib.assertTrue(Machine.interrupt().disabled());
    Lib.assertTrue(priority >= priorityMinimum &&
        priority <= priorityMaximum);
    getThreadState(thread).setPriority(priority);
  }

  public boolean increasePriority() {
    boolean intStatus = Machine.interrupt().disable();
    KThread thread = KThread.currentThread();
    int priority = getPriority(thread);
    if (priority == priorityMaximum)
      return false;
    setPriority(thread, priority+1);
    Machine.interrupt().restore(intStatus);
    return true;
  }

  public boolean decreasePriority() {
    boolean intStatus = Machine.interrupt().disable();
    KThread thread = KThread.currentThread();
    int priority = getPriority(thread);
    if (priority == priorityMinimum)
      return false;
    setPriority(thread, priority-1);
    Machine.interrupt().restore(intStatus);
    return true;
  }

  /**
   * The default priority for a new thread. Do not change this value.
   */
  public static final int priorityDefault = 1;

  /**
   * The minimum priority that a thread can have. Do not change this value.
   */
  public static final int priorityMinimum = 0;

  /**
   * The maximum priority that a thread can have. Do not change this value.
   */
  public static final int priorityMaximum = 7;

  /**
   * Return the scheduling state of the specified thread.
   *
   * @param	thread the thread whose scheduling state to return.
   * @return	the scheduling state of the specified thread.
   */
  protected ThreadState getThreadState(KThread thread) {
    if (thread.schedulingState == null)
      thread.schedulingState = new ThreadState(thread);
    return (ThreadState) thread.schedulingState;
  }

  private static class PriorityTest implements Runnable {
    PriorityTest() {
    }
    public void run() {
      Machine.interrupt().disable();
      System.out.println("APPLES");
    }
    private PriorityQueue q;
  }

  // Takes in an int n and a string and prints the string n times.
  private static class FooTest implements Runnable {
    FooTest(int n, String s) {
      this.n = n;
      this.s = s;
    }

    public void run() {
      for (int i = 0; i < n; i++) {
        System.out.println(s);
      }
    }

    private int n;
    private String s;
  }


  public static void selfTest() {
    boolean intStatus = Machine.interrupt().disable();

    // create thread A with priority 2
    // have thread A acquire lock l
    // thread A creates thread B with priority 7,
    // and thread C with priority 5
    // Thread B tries to acquire lock l
    //
    // Thread A should run rather than thread C
    // Make the threads print stuff so we see what order
    // they run in
    //
    // Kthread a = new KThread(new FooTest(3, "aaa").setName("a");
    // ThreadedKernel.scheduler.setPriority(a, 2);
    // a.fork();
    //
    // Lock l = new Lock();
    // a.acquire(l);
    //
    // KThread b = new KThread(new FooTest(3, "bbb").setName("b");
    // KThread c = new KThread(new FooTest(3, "ccc").setName("c");
    // ThreadedKernel.scheduler.setPriority(b, 7);
    // ThreadedKernel.scheduler.setPriority(c, 5);
    // b.fork();
    // c.fork();
    //
    // Scheduler.currentThread().yield();
    //
    KThread a = new KThread(new FooTest(3, "aaa")).setName("a");
    KThread b = new KThread(new FooTest(5, "bbb")).setName("b");
    KThread c = new KThread(new FooTest(8, "ccc")).setName("c");

    ThreadedKernel.scheduler.setPriority(a, 7);
    ThreadedKernel.scheduler.setPriority(b, 2);
    ThreadedKernel.scheduler.setPriority(c, 4);

    // c should have the highest priority

    // c should run first, then b, then a
    // However, none are running right now
    // At least they are being added to the ready queue!

    a.fork();
    b.fork();
    c.fork();

    KThread.currentThread().yield();
    KThread.currentThread().yield();

    //PriorityQueue waitQueue = (PriorityQueue) ThreadedKernel.scheduler.newThreadQueue(false);
    //KThread x = new KThread(new PriorityTest()).setName("x");
    //waitQueue.waitForAccess(x);
    //x.fork();
    //ThreadedKernel.scheduler.setPriority(x, 5);
    //KThread.currentThread().yield();
    //System.out.println("PANDAS");
    Machine.interrupt().restore(intStatus);
  }

  protected class ThreadComparator implements Comparator<ThreadState> { // see ThreadState class at line ~200
    ThreadComparator() {
    }
    // We want to be able to dequeue the thread with the greatest priority;
    // if waitQueue.poll() dequeues the thread with the *smallest* priority,
    // swap the outputs of the comparator
    public int compare(ThreadState s1, ThreadState s2) {
      if (s1.getPriority() > s2.getPriority()) {
        return -1;
      } else if (s1.getPriority() < s2.getPriority()) {
        return 1;
      } else {
        if (s1.getWaitTime() > s2.getWaitTime()) {
          return -1;
        } else if (s1.getWaitTime() < s2.getWaitTime()) {
          return 1;
        } else {
          return 0; // This isn't likely to happen
        }
      }
    }
  }
  /**
   * A <tt>ThreadQueue</tt> that sorts threads by priority.
   */
  /* Hopefully, this won't become too confusing with java.util.PriorityQueue
     also being a thing */
  protected class PriorityQueue extends ThreadQueue {
    // We're forced to refer to it as "java.util.PriorityQueue" to make it compile
    // 11 is the default initial capacity for a java.util.PriorityQueue
    private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(11, new ThreadComparator());
    PriorityQueue(boolean transferPriority) {
      this.transferPriority = transferPriority;
    }
    public void waitForAccess(KThread thread) {
      Lib.assertTrue(Machine.interrupt().disabled());
      getThreadState(thread).waitForAccess(this);
      this.print();
    }
    public void acquire(KThread thread) {
      Lib.assertTrue(Machine.interrupt().disabled());
      getThreadState(thread).acquire(this);
    }
    public KThread nextThread() {
      Lib.assertTrue(Machine.interrupt().disabled());
      // implement me
      /* Lame attempt at an implementation - J. G-D. 3/16/15 */
      ThreadState previous = getThreadState(KThread.currentThread());
      while(!previous.pStack.empty()) {
        previous.pStack.pop();
      }

      ThreadState state = waitQueue.poll();
      this.print();
      // poll() returns null for an empty queue
      if (state == null) {
        return null;
      }
      // state should have the highest-priority thread
      return state.thread;
    }
    /**
     * Return the next thread that <tt>nextThread()</tt> would return,
     * without modifying the state of this queue.
     *
     * @return	the next thread that <tt>nextThread()</tt> would
     * return.
     */
    protected ThreadState pickNextThread() {
      // implement me
      return waitQueue.peek();
    }
    public void print() {
      Lib.assertTrue(Machine.interrupt().disabled());
      System.out.print("wait queue size: " + waitQueue.size());
      //System.out.println(waitQueue.toString());

      java.util.PriorityQueue waitQueueCopy = new java.util.PriorityQueue<ThreadState>(11, waitQueue.comparator());
      Iterator iter = waitQueue.iterator();
      while (iter.hasNext()) {
        waitQueueCopy.add(iter.next());
      }
      while (waitQueueCopy.size() > 0) {
       System.out.print(waitQueueCopy.poll() + " ");
      }
      System.out.println();
      
      // implement me (if you want)
      //for(Iterator i=waitQueue.iterator(); i.hasNext(); ) {
      //	System.out.print((ThreadState) i.next() + " ");
      //}
      //System.out.println("");
    }
    /**
     * <tt>true</tt> if this queue should transfer priority from waiting
     * threads to the owning thread.
     */
    public boolean transferPriority;
  }
  /**
   * The scheduling state of a thread. This should include the thread's
   * priority, its effective priority, any objects it owns, and the queue
   * it's waiting for, if any.
   *
   * @see	nachos.threads.KThread#schedulingState
   */
  protected class ThreadState {
    /**
     * Allocate a new <tt>ThreadState</tt> object and associate it with the
     * specified thread.
     *
     * @param	thread the thread this state belongs to.
     */
    public ThreadState(KThread thread) {
      this.thread = thread;
      this.waitTime = 0;
      setPriority(priorityDefault);
    }
    public long getWaitTime() {
      return waitTime;
    }
    /**
     * Return the priority of the associated thread.
     *
     * @return	the priority of the associated thread.
     */
    public int getPriority() {
      return getEffectivePriority();
      // return priority;
    }
    /**
     * Return the effective priority of the associated thread.
     *
     * @return	the effective priority of the associated thread.
     */
    public int getEffectivePriority() {
      if(pStack.empty()) {
        ThreadState current = PriorityScheduler.currentLockHolder;
        return current.priority;
      } else {
        return pStack.peek();
      }
      // implement me
      // Maintain a stack of priorities (integers)
      // When priority gets donated to this thread,
      // push a new value onto the stack
      //
      // When we're done running the thread at the
      // effective priority, pop the value from the stack
      //
      // Otherwise, just return the top of the stack (?)
      // 
      //return priority;
    }
    /**
     * Set the priority of the associated thread to the specified value.
     *
     * @param	priority the new priority.
     */
    public void setPriority(int priority) {
      if (this.priority == priority) {
        return;
      }
      pStack.push(priority);
      // implement me
    }
    /**
     * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
     * the associated thread) is invoked on the specified priority queue.
     * The associated thread is therefore waiting for access to the
     * resource guarded by <tt>waitQueue</tt>. This method is only called
     * if the associated thread cannot immediately obtain access.
     *
     * @param	waitQueue the queue that the associated thread is
     * now waiting on.
     *
     * @see	nachos.threads.ThreadQueue#waitForAccess
     */
    public void waitForAccess(PriorityQueue waitQueue) {
      // implement me
      this.waitTime = Machine.timer().getTime(); // store the time we started waiting
      ThreadState current = PriorityScheduler.currentLockHolder;
      if(current.priority < this.priority) {
        current.setPriority(this.priority);
      }
      waitQueue.waitQueue.add(this);
    }
    /**
     * Called when the associated thread has acquired access to whatever is
     * guarded by <tt>waitQueue</tt>. This can occur either as a result of
     * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
     * <tt>thread</tt> is the associated thread), or as a result of
     * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
     *
     * @see	nachos.threads.ThreadQueue#acquire
     * @see	nachos.threads.ThreadQueue#nextThread
     */
    public void acquire(PriorityQueue waitQueue) {
      // implement me
      PriorityScheduler.currentLockHolder = this;
    }
    public String toString() {
      return "(" + this.thread.toString() + ", Priority: " + this.priority + ", Wait Time: " + this.waitTime + ")";
    }

    /** The thread with which this object is associated. */	
    protected KThread thread;
    /** The priority of the associated thread. */
    protected int priority;
    /** The time this thread started waiting; suggested on Piazza */
    protected long waitTime;
    // pStack
    public Stack<Integer> pStack;
  }
    public static ThreadState currentLockHolder;
}
