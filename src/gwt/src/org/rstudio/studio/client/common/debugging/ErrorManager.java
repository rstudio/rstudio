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
import org.rstudio.studio.client.common.debugging.model.ErrorHandlerType;
import org.rstudio.studio.client.common.debugging.model.ErrorManagerState;
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
             implements DebugModeChangedEvent.Handler,
                        ErrorHandlerChangedEvent.Handler,
                        SessionInitHandler
{
   public interface Binder
   extends CommandBinder<Commands, ErrorManager> {}
   
   private enum DebugHandlerState
   {
      None,
      Pending
   }

   @Inject
   public ErrorManager(EventBus events, 
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
      
      events_.addHandler(DebugModeChangedEvent.TYPE, this);
      events_.addHandler(ErrorHandlerChangedEvent.TYPE, this);
      events_.addHandler(SessionInitEvent.TYPE, this);
   }

   // Event and command handlers ----------------------------------------------

   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // if we expected to go into debug mode, this is what we were waiting
      // for--change the error handler back to whatever it was formerly
      if (event.debugging() && 
          debugHandlerState_ == DebugHandlerState.Pending)
      {
         setErrorManagementType(previousHandlerType_);
      }
      debugHandlerState_ = DebugHandlerState.None;
   }

   @Override
   public void onErrorHandlerChanged(ErrorHandlerChangedEvent event)
   {
      int newType = event.getHandlerType().getType();
      if (newType != errorManagerState_.getErrorHandlerType())
      {
         errorManagerState_.setErrorHandlerType(newType);
         syncHandlerCommandsCheckedState();
      }
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      errorManagerState_ = session_.getSessionInfo().getErrorState();
      if (session_.getSessionInfo().getHaveSrcrefAttribute())
      {
         syncHandlerCommandsCheckedState();
      }
      else
      {
         // If we don't have source references, disable the commands that 
         // set error handlers (these generally work on user code only, and
         // we can't reliably distinguish user code without source refs)
         commands_.errorsMessage().setEnabled(false);
         commands_.errorsTraceback().setEnabled(false);
         commands_.errorsBreak().setEnabled(false);
      }
   }

   @Handler
   public void onErrorsMessage()
   {
      setErrorManagementTypeCommand(ErrorHandlerType.ERRORS_MESSAGE);
   }
   
   @Handler 
   public void onErrorsTraceback()
   {
      setErrorManagementTypeCommand(ErrorHandlerType.ERRORS_TRACEBACK);
   }

   @Handler 
   public void onErrorsBreak()
   {
      setErrorManagementTypeCommand(ErrorHandlerType.ERRORS_BREAK);
   }
   
   // Public methods ----------------------------------------------------------

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

   public int getErrorHandlerType()
   {
      return errorManagerState_.getErrorHandlerType();
   }
   
   // Private methods ---------------------------------------------------------

   private void setErrorManagementTypeCommand(int type)
   {
      // The error handler may be currently overridden for debug mode. If the
      // user changes the setting via command during debug mode, we don't want
      // to change it back when leaving debug mode.
      debugHandlerState_ = DebugHandlerState.None;
      setErrorManagementType(type);
   }

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
            // No action necessary--the server emits an event when the handler
            // type changes
         }
      });
   }
   
   private void syncHandlerCommandsCheckedState()
   {
      int type = getErrorHandlerType();
      commands_.errorsMessage().setChecked(
            type == ErrorHandlerType.ERRORS_MESSAGE);
      commands_.errorsTraceback().setChecked(
            type == ErrorHandlerType.ERRORS_TRACEBACK);
      commands_.errorsBreak().setChecked(
            type == ErrorHandlerType.ERRORS_BREAK);
   }

   private final EventBus events_;
   private final DebuggingServerOperations server_;
   private final Session session_;
   private final Commands commands_;

   private DebugHandlerState debugHandlerState_ = DebugHandlerState.None;
   private ErrorManagerState errorManagerState_; 
   private int previousHandlerType_;
}
