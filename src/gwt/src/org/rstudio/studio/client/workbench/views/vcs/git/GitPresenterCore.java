/*
 * GitPresenterCore.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.MessageDisplay.PasswordResult;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.events.AskPassEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.VcsState;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitPresenterCore
{
   public interface Binder extends CommandBinder<Commands, GitPresenterCore> {}
   
   @Inject
   public GitPresenterCore(GitServerOperations server,
                           VcsState vcsState,
                           final Commands commands,
                           Binder commandBinder,
                           EventBus eventBus,
                           final GlobalDisplay globalDisplay,
                           final Satellite satellite,
                           final SatelliteManager satelliteManager)
   {
      server_ = server;
      vcsState_ = vcsState;
      
      commandBinder.bind(commands, this);

      vcsState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            boolean hasRemote = vcsState_.hasRemote();
            commands.vcsPull().setEnabled(hasRemote);
            commands.vcsPush().setEnabled(hasRemote);
         }
      });

      eventBus.addHandler(AskPassEvent.TYPE, new AskPassEvent.Handler()
      {
         private boolean handleAskPass(String targetWindow)
         {
            // calculate the current window name
            String window = StringUtil.notNull(satellite.getSatelliteName());
            
            // handle it if the target is us
            if (window.equals(targetWindow))
               return true;
            
            // also handle if we are the main window and the specified
            // satellite doesn't exist
            if (!satellite.isCurrentWindowSatellite() &&
                !satelliteManager.satelliteWindowExists(targetWindow))
               return true;
            
            // othewise don't handle
            else
               return false;
         }
         
         @Override
         public void onAskPass(final AskPassEvent e)
         {
            if (!handleAskPass(e.getWindow()))
               return;
            
            askpassPending_ = true;
            
            globalDisplay.promptForPassword(
                  "Password",
                  e.getPrompt(),
                  "",
                  e.getRememberPasswordPrompt(),
                  rememberByDefault_,
                  new ProgressOperationWithInput<PasswordResult>()
                  {
                     @Override
                     public void execute(final PasswordResult result,
                                         final ProgressIndicator indicator)
                     {
                        askpassPending_ = false;
                        
                        rememberByDefault_ = result.remember;

                        RSAEncrypt.encrypt_ServerOnly(
                              server_,
                              result.password,
                              new RSAEncrypt.ResponseCallback()
                              {
                                 @Override
                                 public void onSuccess(String encryptedData)
                                 {
                                    server_.askpassCompleted(
                                     encryptedData,
                                     !StringUtil.isNullOrEmpty(e.getRememberPasswordPrompt())
                                         && result.remember,
                                     new VoidServerRequestCallback(indicator));
                                    
                                 }

                                 @Override
                                 public void onFailure(ServerError error)
                                 {
                                    Debug.logError(error);
                                 }
                              });
                     }
                  },
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        askpassPending_ = false;
                        
                        server_.askpassCompleted(
                                           null, false,
                                           new SimpleRequestCallback<Void>());
                     }
                  });
         }
      });
      
      // if there is an askpass pending when the window closes then send an
      // askpass cancel
      Window.addWindowClosingHandler(new ClosingHandler() {

         @Override
         public void onWindowClosing(ClosingEvent event)
         {
            if (askpassPending_)
            {
               askpassPending_ = false;
               
               server_.askpassCompleted(null, 
                                        false,
                                        new SimpleRequestCallback<Void>());
            }
            
         } 
      });


   }

   @Handler
   void onVcsRefresh()
   {
      vcsState_.refresh();
   }

   
   @Handler
   void onVcsPull()
   {
      server_.gitPull(new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog("Git Pull", proc).showModal();
         }
      });
   }

   @Handler
   void onVcsPush()
   {
      server_.gitPush(new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog("Git Push", proc).showModal();
         }
      });
   }
    
   private final GitServerOperations server_;
   private final VcsState vcsState_;
   private boolean rememberByDefault_ = true;
   private boolean askpassPending_ = false;
}
