import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

/** A wrapper around a java lock to monitors the locks current owner and provides a toString method for diagnostics */
class ServerLockState implements Lock {
   String lockInfoStr;
   ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   Lock writeLock = lock.writeLock();
   String lockOwnerName;
   String toString() {
      return lockInfoStr + (lockOwnerName == null ? ": free" : ": owned by: " + lockOwnerName);
   }

   void lock() {
      writeLock.lock();
      lockOwnerName = DynUtil.getCurrentThreadString();
   }

   void unlock() {
      writeLock.unlock();
      lockOwnerName = null;
   }

   void lockInterruptibly() throws InterruptedException {
      writeLock.lockInterruptibly();
      lockOwnerName = DynUtil.getCurrentThreadString();
   }

   boolean tryLock() {
      if (writeLock.tryLock()) {
         lockOwnerName = DynUtil.getCurrentThreadString();
         return true;
      }
      return false;
   }

   boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException {
      if (writeLock.tryLock(time, timeUnit)) {
         lockOwnerName = DynUtil.getCurrentThreadString();
         return true;
      }
      return false;
   }

   Condition newCondition() {
      return writeLock.newCondition();
   }
}