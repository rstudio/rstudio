/*
 * PosixShell.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.posixshell;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.crypto.PublicKeyInfo;
import org.rstudio.studio.client.common.posixshell.events.PosixShellExitEvent;
import org.rstudio.studio.client.common.posixshell.events.PosixShellOutputEvent;
import org.rstudio.studio.client.common.posixshell.model.PosixShellServerOperations;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.common.shell.ShellInteractionManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PosixShell implements PosixShellOutputEvent.Handler,
                                   PosixShellExitEvent.Handler
 
{
   public interface Display extends ShellDisplay
   { 
   }
   
   public interface Observer 
   {
      void onShellTerminated();
   }
   
  
   @Inject
   public PosixShell(Display display,
                     GlobalDisplay globalDisplay,
                     EventBus eventBus,
                     PosixShellServerOperations server,
                     CryptoServerOperations cryptoServer)
   {
      // save references
      display_ = display;
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      manager_ = new ShellInteractionManager(display_, 
                                             cryptoServer,
                                             inputHandler_);
           
      // set max lines
      int maxLines = 1000;
      display_.setMaxOutputLines(maxLines);
   
      // subscribe to server events
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellOutputEvent.TYPE, this));
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellExitEvent.TYPE, this));
   }
   
   
   public Widget getWidget()
   {
      return display_.getShellWidget();
   }
   
   public void start(int width, final Observer observer)
   {
      observer_ = observer;
           
      server_.startPosixShell(
            width,
            display_.getMaxOutputLines(), 
            new ServerRequestCallback<PublicKeyInfo>()
            {
               @Override 
               public void onResponseReceived(PublicKeyInfo publicKeyInfo)
               {
                  manager_.setPublicKeyInfo(publicKeyInfo);
               }

               @Override
               public void onError(ServerError error)
               {   
                  globalDisplay_.showErrorMessage(
                        "Error Starting Shell",
                        error.getUserMessage(),
                        new Operation() {
                           @Override
                           public void execute()
                           {
                              if (observer_ != null)
                                 observer_.onShellTerminated();
                           }
                           
                        });
               }
            });
   }
   
   public void terminate()
   {
      // detach our event bus handlers so we don't get the exited event
      detachEventBusHandlers();
      
      // terminate the shell
      server_.terminatePosixShell(new VoidServerRequestCallback());
   }
   
   @Override
   public void onPosixShellOutput(PosixShellOutputEvent event)
   {
      manager_.displayOutput(event.getOutput());
   }
   
   @Override
   public void onPosixShellExit(PosixShellExitEvent event)
   {
      detachEventBusHandlers();
      
      if (observer_ != null)
         observer_.onShellTerminated();    
   }

   public void detachEventBusHandlers()
   {
      for (int i=0; i<eventBusHandlers_.size(); i++)
         eventBusHandlers_.get(i).removeHandler();
      eventBusHandlers_.clear();
   }
   
 
   private CommandWithArg<String> inputHandler_ = new CommandWithArg<String>() 
   {
      @Override
      public void execute(String input)
      {
         if (input != null)
         {
            server_.sendInputToPosixShell(
                  input, 
                  new ServerRequestCallback<Void>() {
                     @Override
                     public void onError(ServerError error)
                     {
                        manager_.displayError(error.getUserMessage());
                     }
                  });
         }
         else
         {
            server_.interruptPosixShell(new VoidServerRequestCallback());
         }
      }
   };

   private final ShellInteractionManager manager_;
   
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   private Observer observer_ = null;
   private final PosixShellServerOperations server_;
   
   private ArrayList<HandlerRegistration> eventBusHandlers_ = 
                                    new ArrayList<HandlerRegistration>();
}
