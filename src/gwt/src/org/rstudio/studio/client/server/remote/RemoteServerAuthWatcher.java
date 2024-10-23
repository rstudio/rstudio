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

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.AuthorizedEvent;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

public class RemoteServerAuthWatcher
{
   public RemoteServerAuthWatcher(RemoteServer server, EventBus eventBus) 
   {
      eventBus_ = eventBus;
      server_ = server;
      isListening_ = false;
      pollTimer_ = new Timer(){
         @Override
         public void run()
         {
            checkAuthStatus();
         };
      };
   }

   public void start()
   {
      if (isListening_)
         stop();

      isListening_ = true;
      
      pollTimer_.schedule(1000);
   }

   public void stop()
   {
      isListening_ = false;
      pollTimer_.cancel();
   }

   private void checkAuthStatus()
   {
      server_.ping(new VoidServerRequestCallback() {
         @Override
         public void onSuccess()
         {
            stop();
            // Send an AuthenticatedEvent to the bus
            AuthorizedEvent event = new AuthorizedEvent();
            eventBus_.fireEvent(event);
         };

         @Override
         public void onFailure()
         {
            pollTimer_.schedule(1000);
         };

         @Override
         public void onCompleted()
         {
            // Do nothing.
         };
         
      });
   }
   

   private RemoteServer server_;
   private EventBus eventBus_;
   private boolean isListening_;
   private Timer pollTimer_;
}
