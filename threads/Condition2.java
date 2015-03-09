package nachos.threads;

import nachos.machine.*;

/**
 * CSCI 341 Lab 1, Part 2
 * 
 * Joey Gonzales-Dones, Benjamin McGarvey, Jacob Chae
 * 
 * We affirm that we have adhered to the Honor Code in this assignment.
 * -J. G-D., B. M., J. C.
 * 
 */

// Part 2: 20 pts, 20 lines, due March 2nd

// Implement condition variables directly, by using interrupt enable and
// disable to provide atomicity. We have provided a sample implementation that uses semaphores; your job is to provide
// an equivalent implementation without directly using semaphores (you may of course still use locks, even though they
// indirectly use semaphores). Once you are done, you will have two alternative implementations that provide the exact
// same functionality. Your second implementation of condition variables must reside in class nachos.threads.Condition2.
// You will probably find it helpful to look at both nachos.threads.Condition and nachos.threads.Semaphore.

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * (Note: boolean intStatus = Machine.interrupt().disable(); and Machine.interrupt().restore(intStatus);
 *  will probably be useful)
 * 

 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 { // Operations: sleep(), wake(), and wakeAll()

  /* Class variables */
  private Lock conditionLock; // Operations: acquire(), release(), isHeldByCurrentThread()
  private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);

  /**
   * Allocate a new condition variable.
   *
   * @param	conditionLock	the lock associated with this condition
   *				variable. The current thread must hold this
   *				lock whenever it uses <tt>sleep()</tt>,
   *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
   */
  public Condition2(Lock conditionLock) {
    this.conditionLock = conditionLock;
  }

  /**
   * Atomically release the associated lock and go to sleep on this condition
   * variable until another thread wakes it using <tt>wake()</tt>. The
   * current thread must hold the associated lock. The thread will
   * automatically reacquire the lock before <tt>sleep()</tt> returns.
   */
  public void sleep() { // waitQueue.waitForAccess(KThread.currentThread());
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    boolean intStatus = Machine.interrupt().disable();

    conditionLock.release(); 
    waitQueue.waitForAccess(KThread.currentThread());
    KThread.sleep();

    Machine.interrupt().restore(intStatus);
    conditionLock.acquire();
  }


  /**
   * Wake up at most one thread sleeping on this condition variable. The
   * current thread must hold the associated lock.
   */
  public void wake() {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    boolean intStatus = Machine.interrupt().disable();

    KThread next = waitQueue.nextThread();
    if (next != null) {
      next.ready();
    }

    Machine.interrupt().restore(intStatus);
  }

  /**
   * Wake up all threads sleeping on this condition variable. The current
   * thread must hold the associated lock.
   */
  public void wakeAll() {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    boolean intStatus = Machine.interrupt().disable();

    KThread next;
    while ((next = waitQueue.nextThread()) != null) {
      next.ready();
    }

    Machine.interrupt().restore(intStatus);
  }
}
