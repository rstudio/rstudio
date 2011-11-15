/*
 * VCSCore.java
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
package org.rstudio.studio.client.workbench.views.vcs;

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
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.events.AskPassEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.model.VcsState;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VCSCore
{
   public interface Binder extends CommandBinder<Commands, VCSCore> {}
   
   @Inject
   public VCSCore(VCSServerOperations server,
                  VcsState vcsState,
                  final Commands commands,
                  Binder commandBinder,
                  EventBus eventBus,
                  final GlobalDisplay globalDisplay)
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
         @Override
         public void onAskPass(final AskPassEvent e)
         {
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
                        server_.askpassCompleted(
                                           null, false,
                                           new SimpleRequestCallback<Void>());
                     }
                  });
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
      server_.vcsPull(new SimpleRequestCallback<ConsoleProcess>() {
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
      server_.vcsPush(new SimpleRequestCallback<ConsoleProcess>() {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog("Git Push", proc).showModal();
         }
      });
   }
    
   private final VCSServerOperations server_;
   private final VcsState vcsState_;
   private boolean rememberByDefault_ = true;
}
