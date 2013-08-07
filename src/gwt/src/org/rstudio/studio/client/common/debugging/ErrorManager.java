/*
 * ErrorManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.common.debugging;

import org.rstudio.studio.client.server.Void;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.UnhandledErrorEvent;
import org.rstudio.studio.client.common.debugging.model.ErrorHandlerType;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ErrorManager
             implements UnhandledErrorEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, ErrorManager> {}

   @Inject
   public ErrorManager(EventBus events, Binder binder, Commands commands, DebuggingServerOperations server)
   {
      events_ = events;
      server_ = server;
      binder.bind(commands, this);
      
      events_.addHandler(UnhandledErrorEvent.TYPE, this);
   }

   // Event and command handlers ----------------------------------------------

   @Override
   public void onUnhandledError(UnhandledErrorEvent event)
   {
      lastError_ = event.getError();
   }
   
   @Handler 
   public void onErrorsAutomatic()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_AUTOMATIC);
   }

   @Handler 
   public void onErrorsBreak()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_BREAK_ALWAYS);
   }
   
   @Handler
   public void onErrorsBreakUser()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_BREAK_USER);
   }

   // Public methods ----------------------------------------------------------

   public UnhandledError consumeLastError()
   {
      UnhandledError err = lastError_;
      lastError_ = null;
      return err;
   }

   // Private methods ---------------------------------------------------------
   
   private void setErrorManagementType(int type)
   {
      server_.setErrorManagementType(type, 
            new ServerRequestCallback<Void>()
      {         
         @Override
         public void onError(ServerError error)
         {
            // TODO: Something reasonable here. 
         }
      });
   }

   private final EventBus events_;
   private final DebuggingServerOperations server_;

   private UnhandledError lastError_;
}
