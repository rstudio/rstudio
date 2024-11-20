/*
 * RemoteServerAuthWatcher.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.server.remote;


import com.google.gwt.user.client.Timer;

public class RemoteServerAuthWatcher
{
   public static interface CheckAuthStatus 
   {
      void checkAuthStatus();
   }

   public RemoteServerAuthWatcher(CheckAuthStatus authStatusChecker)
   {
      authStatusChecker_ = authStatusChecker;
      isListening_ = false;
      pollTimer_ = new Timer(){
         @Override
         public void run()
         {
            authStatusChecker_.checkAuthStatus();
         };
      };
   }

   public void start()
   {
      if (isListening_)
         stop();

      isListening_ = true;
      
      pollTimer_.scheduleRepeating(1000);
   }

   public void stop()
   {
      isListening_ = false;
      pollTimer_.cancel();
   }

   private CheckAuthStatus authStatusChecker_;
   private boolean isListening_;
   private Timer pollTimer_;
}
