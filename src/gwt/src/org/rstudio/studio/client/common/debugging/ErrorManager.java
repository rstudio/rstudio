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
import org.rstudio.studio.client.common.debugging.events.ErrorHandlerChangedEvent;
import org.rstudio.studio.client.common.debugging.events.UnhandledErrorEvent;
import org.rstudio.studio.client.common.debugging.model.ErrorHandlerType;
import org.rstudio.studio.client.common.debugging.model.ErrorManagerState;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ErrorManager
             implements UnhandledErrorEvent.Handler,
                        DebugModeChangedEvent.Handler,
                        ErrorHandlerChangedEvent.Handler,
                        SessionInitHandler
{
   public interface Binder
   extends CommandBinder<Commands, ErrorManager> {}
   
   private enum DebugHandlerState
   {
      None,
      Pending,
      Active
   }

   @Inject
   public ErrorManager(
         EventBus events, 
         Binder binder, 
         Commands commands, 
         DebuggingServerOperations server,
         Session session)
   {
      events_ = events;
      server_ = server;
      commands_ = commands;
      session_ = session;
      binder.bind(commands, this);
      
      events_.addHandler(UnhandledErrorEvent.TYPE, this);
      events_.addHandler(DebugModeChangedEvent.TYPE, this);
      events_.addHandler(ErrorHandlerChangedEvent.TYPE, this);
      events_.addHandler(SessionInitEvent.TYPE, this);
   }

   // Event and command handlers ----------------------------------------------

   @Override
   public void onUnhandledError(UnhandledErrorEvent event)
   {
      lastError_ = event.getError();
   }

   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // if we expected to go into debug mode, this is what we were waiting
      // for--remember that we need to switch out of the temporary error
      // management state for this session
      if (event.debugging() && 
          debugHandlerState_ == DebugHandlerState.Pending)
      {
         debugHandlerState_ = DebugHandlerState.Active;
      }
      // if we're leaving debug mode and we were supplying the error handler
      // during the session, restore the handler to its previous setting
      else if (!event.debugging() && 
               debugHandlerState_ == DebugHandlerState.Active)
      {
         setErrorManagementType(previousHandlerType_);
         debugHandlerState_ = DebugHandlerState.None;
      }
      else
      {
         debugHandlerState_ = DebugHandlerState.None;
      }
   }

   @Override
   public void onErrorHandlerChanged(ErrorHandlerChangedEvent event)
   {
      int newType = event.getHandlerType().getType();
      if (newType != errorManagerState_.getErrorHandlerType())
      {
         errorManagerState_.setErrorHandlerType(newType);
      }
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      errorManagerState_ = session_.getSessionInfo().getErrorState();
    
      commands_.errorsInMyCode().setChecked(
            errorManagerState_.getUserCodeOnly());
     
      commands_.errorsExpandTraceback().setChecked(false);      
   }

   @Handler
   public void onErrorsMessage()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_MESSAGE);
   }
   
   @Handler 
   public void onErrorsTraceback()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_TRACEBACK);
   }

   @Handler 
   public void onErrorsBreak()
   {
      setErrorManagementType(ErrorHandlerType.ERRORS_BREAK);
   }
   
   @Handler
   public void onErrorsInMyCode()
   {
      final boolean userCode = commands_.errorsInMyCode().isChecked();
      if (userCode != errorManagerState_.getUserCodeOnly())
      {
         server_.setErrorsUserCodeOnly(userCode, 
               new ServerRequestCallback<Void>()
         {
            @Override
            public void onResponseReceived(Void v)
            {
               errorManagerState_.setUserCodeOnly(userCode);
            }
            
            @Override
            public void onError(ServerError error)
            {
               // if an error occurs, restore the command's checked state to
               // whatever it was previously 
               commands_.errorsInMyCode().setChecked(
                     errorManagerState_.getUserCodeOnly());
            }
         });;
      }
   }

   @Handler
   public void onErrorsExpandTraceback()
   {
   }
      
   // Public methods ----------------------------------------------------------

   public UnhandledError consumeLastError()
   {
      UnhandledError err = lastError_;
      lastError_ = null;
      return err;
   }
   
   public void setDebugSessionHandlerType(
         int type, 
         final ServerRequestCallback<Void> callback)
   {
      if (type == errorManagerState_.getErrorHandlerType())
         return;
      
      setErrorManagementType(type, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            previousHandlerType_ = errorManagerState_.getErrorHandlerType();
            debugHandlerState_ = DebugHandlerState.Pending;
            callback.onResponseReceived(v);
         }

         @Override
         public void onError(ServerError error)
         {
            callback.onError(error);
         }
      });
   }
   
   public boolean getExpandTraceback()
   {
      return false;
   }

   // Private methods ---------------------------------------------------------

   private void setErrorManagementType(
         int type, 
         ServerRequestCallback<Void> callback)
   {
      server_.setErrorManagementType(type, callback);
   }
      
   private void setErrorManagementType(int type)
   {
      setErrorManagementType(type, 
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
   private final Session session_;
   private final Commands commands_;

   private DebugHandlerState debugHandlerState_ = DebugHandlerState.None;
   private ErrorManagerState errorManagerState_; 
   private int previousHandlerType_;
   private UnhandledError lastError_;
}
