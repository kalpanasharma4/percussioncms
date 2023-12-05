/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.queue;

import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.utils.types.PSPair;

/**
 * A generic base class used for managing a queue. The queue can be either in memory (light weight) or
 * persistent (heavy weight) queue.
 * <p>
 * This class is inspired by PSSearchIndexEventQueue. It is possible to reuse this code
 * with PSSearchIndexEventQueue in the future, so that there is no copy and past code.
 * 
 * @author YuBingChen
 *
 * @param <T> the queue set
 */
public abstract class PSAbstractEventQueue<T>
{

   protected abstract String getQueueName();
   protected abstract void preStart();
   protected abstract boolean doRun();
   protected abstract void preShutdown();
   protected abstract PSPair<PSSiteQueue, Integer> getNextEvent();
   
   /**
    * Causes any persisted events to be restored from the repository, and starts
    * the thread that processes queued events.
    *
    * @throws IllegalStateException if the queue has already been started or is
    * shutting down.
    */
   public void start()
   {
      synchronized (m_runMonitor)
      {
         if (m_run) {
            throw new IllegalStateException("Index queue is already running");
         }

         if (m_shutdown) {
            throw new IllegalStateException("Index queue is shutting down");
         }

         preStart();

         m_queueThread = new Thread(getQueueName())
         {
           @Override
           public void run() {
               PSRequestInfoBase.initRequestInfo(null);

               while(!m_shutdown)
               {
                  if (!doRun()) {
                     break;
                  }
               }

              PSRequestInfoBase.resetRequestInfo();

               // we've finished, so notify the shutdown method if it's waiting
               m_run = false;
               synchronized(m_runMonitor)
               {
                  m_runMonitor.notifyAll();
               }
            }
         };

         m_queueThread.setDaemon(true);
         m_queueThread.start();
         m_run = true;
      }
   }

   /**
    * Stops processing queued events and shuts down the queue.  Will not return
    * until it has completed any work in progress.  Calling this multiple times
    * is safe.
    */
   public void shutdown()
   {
      synchronized(m_shutdownMonitor)
      {
         if (m_shutdown) {
            return;
         }

         // set shutdown flag and wait till the queue is shutdown
         m_shutdown = true;
      }

      //PSConsole.printMsg(getQueueName(), "Shutting down index queue");

      // notify thread if it's waiting on the queue
      synchronized(m_queueMonitor)
      {
         m_queueMonitor.notifyAll();
      }

      // now wait for queue processing thread to finish any current work
      synchronized (m_runMonitor)
      {
         while (m_run)
         {
            try
            {
               m_runMonitor.wait(5000L);
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }
         }

         preShutdown();
         m_shutdown = false;
      }
   }

   /**
    * Get next event which is returned by {@link #getNextEvent()}.
    * If there is nothing in the queue (hence the {@link #getNextEvent()} 
    * return <code>null</code>, then this will for a limited time before
    * returning, but will not process any new events that are ready after
    * waiting. In this case, the caller in different thread may call
    * {@link #notifyEventQueue()} to wait up this and return <code>null</code>.
    * 
    * @param timeOut The number of milliseconds to wait before returning. 
    * Pass <code>0</code> to wait indefinitely, negative is the same as <code>0</code>.
    * 
    * @return The next set of events to process, may be <code>null</code> if
    * there are no events in the queue or if we waited.
    * 
    * @throws InterruptedException if interrupted while waiting on the queue
    * monitor.
    */

protected PSPair<PSSiteQueue, Integer> getNextQueueEvent(long timeOut) throws InterruptedException
   {
      timeOut = timeOut <= 0 ? 100 : timeOut;
      PSPair<PSSiteQueue, Integer> eventSet = null;

      synchronized (m_queueMonitor)
      {
         if (!isShutdown())
         {

            while (eventSet == null)
            {
               eventSet = getNextEvent();
               // Wait for a caller (from different thread) to wake up this thread
               // This will be when some events have been added into the queue
               m_queueMonitor.wait(timeOut);
            }
         }
      }

      return eventSet;
   }
   

   protected void notifyEventQueue()
   {
      if (m_run)
      {
         // Notify the queue processing thread if it is waiting for more events
         synchronized (m_queueMonitor)
         {
            m_queueMonitor.notifyAll();
         }
      }
   }
   
   protected boolean isShutdown()
   {
      return m_shutdown;
   }
   
   /**
    * Monitor object to provide synchronization of start and shutdown. Never
    * <code>null</code> or modified.
    */
   private final Object m_runMonitor = new Object();

   /**
    * Monitor object to provide synchronization of queue access. Never
    * <code>null</code> or modified.
    */
   private final Object m_queueMonitor = new Object();

   /**
    * Monitor object to provide synchronized access to the {@link #m_shutdown}
    * flag. Never <code>null</code> or modified.
    */
   private final Object m_shutdownMonitor = new Object();

   /**
    * Indicates if the queue is running. Initially <code>false</code>,
    * set to <code>true</code> by {@link #start()}, set to <code>false</code>
    * by {@link #shutdown()}.  Value should only be modified if synchronized
    * on the {@link #m_runMonitor} object.
    */
   private boolean m_run = false;

   /**
    * Thread to handle processing of events.  Initialized by {@link #start()},
    * not <code>null</code> or modified until {@link #shutdown()} is called.
    */
   private Thread m_queueThread;

   /**
    * Indicates that the queue is shutting down.  Initially <code>false</code>,
    * set to <code>true</code> by {@link #shutdown()}, set back to
    * <code>false</code> once shutdown is completed.  Value should only be
    * modified by the {@link #shutdown()} method, where access is synchronized
    * appropriately.
    */
   private boolean m_shutdown = false;


}
