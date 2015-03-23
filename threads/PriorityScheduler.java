package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet; // These might be made redundant
import java.util.HashSet; // by java.util.PriorityQueue
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
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
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
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

  private static class PriorityTest implements Runnable {
    PriorityTest(PriorityQueue q) {
      this.q = q;
    }

    public void run() {
      Machine.interrupt().disable();
      System.out.println("APPLES");
      q.print();
    }

    private PriorityQueue q;
  }


  public static void selfTest() {
    boolean intStatus = Machine.interrupt().disable();
    PriorityQueue waitQueue = (PriorityQueue) new PriorityScheduler().newThreadQueue(false);
    KThread x = new KThread(new PriorityTest(waitQueue)).setName("x");

    waitQueue.waitForAccess(x);
    x.fork();
    ThreadedKernel.scheduler.setPriority(x, 5);
    KThread.currentThread().yield();
    System.out.println("PANDAS");

    Machine.interrupt().restore(intStatus);
  }


    protected class ThreadComparator implements Comparator<ThreadState> { // see ThreadState class at line ~200

	ThreadComparator() {

	}

	// We want to be able to dequeue the thread with the greatest priority;
	// if waitQueue.poll() dequeues the thread with the *smallest* priority,
	// swap the outputs of the comparator
	public int compare(ThreadState s1, ThreadState s2) {
		if (s1.getPriority() < s2.getPriority()) {
			return -1;
		} else if (s1.getPriority() > s2.getPriority()) {
			return 1;
		} else {
			if (s1.getWaitTime() < s2.getWaitTime()) {
				return -1;
			} else if (s1.getWaitTime() > s2.getWaitTime()) {
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
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me
	/* Lame attempt at an implementation - J. G-D. 3/16/15 */

	    ThreadState state = waitQueue.poll(); 
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
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // implement me
    return waitQueue.peek();
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
      System.out.print("wait queue size: " + waitQueue.size());
	    // implement me (if you want)
      for(Iterator i=waitQueue.iterator(); i.hasNext(); ) {
        System.out.print((ThreadState) i.next() + " ");
      }
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
	 * @param	thread	the thread this state belongs to.
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
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
	    return priority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    
	    // implement me
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
	    this.waitTime = Machine.timer().getTime(); // store the time we started waiting
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

    }
}
