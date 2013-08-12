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
import org.rstudio.core.client.js.JsObject;
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
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
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
         errorManagerStateDirty_ = true;
      }
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      new JSObjectStateValue(
            "error-management",
            "errorHandlerSettings",
            ClientState.TEMPORARY,
            session_.getSessionInfo().getClientState(),
            false)
       {
          @Override
          protected void onInit(JsObject value)
          {
             if (value != null)
                errorManagerState_ = value.cast();
             else
                errorManagerState_ = ErrorManagerState.create(
                      true, ErrorHandlerType.ERRORS_TRACEBACK, false);
             
             commands_.errorsInMyCode().setChecked(
                   errorManagerState_.getUserCode());
             commands_.errorsExpandTraceback().setChecked(
                   errorManagerState_.getExpandTracebacks());
          }
   
          @Override
          protected JsObject getValue()
          {
             return errorManagerState_.cast();
          }
   
          @Override
          protected boolean hasChanged()
          {
             return errorManagerStateDirty_;
          }
       };
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
      boolean userCode = commands_.errorsInMyCode().isChecked();
      if (userCode != errorManagerState_.getUserCode())
      {
         errorManagerState_.setUserCode(userCode);
         errorManagerStateDirty_ = true;
         
         // reflect the change on the server
         setErrorManagementType(errorManagerState_.getErrorHandlerType());
      }
   }

   @Handler
   public void onErrorsExpandTraceback()
   {
      boolean expandTraceback = commands_.errorsExpandTraceback().isChecked();
      if (expandTraceback != errorManagerState_.getExpandTracebacks())
      {
         errorManagerState_.setExpandTracebacks(expandTraceback);
         errorManagerStateDirty_ = true;
      }
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
      return errorManagerState_.getExpandTracebacks();
   }

   // Private methods ---------------------------------------------------------

   private void setErrorManagementType(
         int type, 
         ServerRequestCallback<Void> callback)
   {
      server_.setErrorManagementType(
            type, errorManagerState_.getUserCode(), callback);
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
   private boolean errorManagerStateDirty_ = false;
   private int previousHandlerType_;
   private UnhandledError lastError_;
}
