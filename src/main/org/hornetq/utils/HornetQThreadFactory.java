/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.hornetq.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.hornetq.core.logging.Logger;

/**
 * 
 * A HornetQThreadFactory
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class HornetQThreadFactory implements ThreadFactory
{
   private static final Logger log = Logger.getLogger(HornetQThreadFactory.class);

   private final ThreadGroup group;

   private final AtomicInteger threadCount = new AtomicInteger(0);

   private final int threadPriority;

   private final boolean daemon;
   
   private final ClassLoader tccl;

   public HornetQThreadFactory(final String groupName, final boolean daemon, final ClassLoader tccl)
   {
      this(groupName, Thread.NORM_PRIORITY, daemon, tccl);
   }

   public HornetQThreadFactory(final String groupName, final int threadPriority, final boolean daemon, final ClassLoader tccl)
   {
      group = new ThreadGroup(groupName + "-" + System.identityHashCode(this));

      this.threadPriority = threadPriority;
      
      this.tccl = tccl;

      this.daemon = daemon;
   }

   public Thread newThread(final Runnable command)
   {
      final Thread t;
      // attach the thread to a group only if there is no security manager:
      // when sandboxed, the code does not have the RuntimePermission modifyThreadGroup
      if (System.getSecurityManager() == null)
      {
         t = new Thread(group, command, "Thread-" + threadCount.getAndIncrement() + " (" + group.getName() + ")");
      }
      else
      {
         t = new Thread(command, "Thread-" + threadCount.getAndIncrement());
      }
      
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            t.setDaemon(daemon);
            t.setPriority(threadPriority);
            return null;
         }
      });
        
      try
      {
         AccessController.doPrivileged(new PrivilegedAction<Object>()
         {
            public Object run()
            {
               t.setContextClassLoader(tccl);
               return null;
            }
         });
      }
      catch (java.security.AccessControlException e)
      {
         log.warn("Missing privileges to set Thread Context Class Loader on Thread Factory. Using current Thread Context Class Loader");
      }

      return t;
   }
}
