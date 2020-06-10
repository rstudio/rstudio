/*
 * NewConnectionInstallOdbcHost.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.connections.ui;


import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionInstallOdbcHost extends Composite
                                          implements ConsoleOutputEvent.Handler, 
                                                     ProcessExitEvent.Handler
{
   interface Binder extends UiBinder<Widget, NewConnectionInstallOdbcHost>
   {}
   
   @Inject
   private void initialize(ConnectionsServerOperations server,
                           GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   public NewConnectionInstallOdbcHost()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
 
      initWidget(createWidget());

      consoleProgressWidget_.setReadOnly(true);
   }

   public void onDeactivate(Operation operation)
   {
      nextPageEnabledOperation_.execute(true);
      operation.execute();
   }

   public void interruptOdbcInstall()
   {
      terminateOdbcInstall(null);
   }
   
   private Widget createWidget()
   {
      return mainWidget_;
   }

   public void writeOutput(String output)
   {
      consoleProgressWidget_.consoleWriteOutput(output);
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      writeOutput(event.getOutput());
   }

   @Override
   public void onProcessExit(ProcessExitEvent event)
   {    
      unregisterHandlers();
      terminateOdbcInstall(null);
   }

   private void reapOdbcInstall(final  Operation operation) {
      if (consoleProcess_ != null) {
         consoleProcess_.reap(new VoidServerRequestCallback() {
            @Override
            public void onSuccess()
            {
               if (operation != null) operation.execute();
            }
            
            @Override
            public void onFailure()
            {
               if (operation != null) operation.execute();
            }
         });
      }
   }

   private void terminateOdbcInstall(final Operation operation) {
      if (consoleProcess_ != null) {
         consoleProcess_.interrupt(new VoidServerRequestCallback() {
            @Override
            public void onSuccess()
            {
               reapOdbcInstall(operation);
            }
            
            @Override
            public void onFailure()
            {
               reapOdbcInstall(operation);
            }
         });
      }
   }

   protected void addHandlerRegistration(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }
   
   protected void unregisterHandlers()
   {
      registrations_.removeHandler();
   }

   public void attachToProcess(final ConsoleProcess consoleProcess)
   {
      consoleProcess_ = consoleProcess;
      
      addHandlerRegistration(consoleProcess.addConsoleOutputHandler(this));
      addHandlerRegistration(consoleProcess.addProcessExitHandler(this));

      consoleProcess.start(new SimpleRequestCallback<Void>()
      {
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            globalDisplay_.showErrorMessage(
               "Installation failed",
               error.getUserMessage());
            
            unregisterHandlers();
         }
      });
   }

   private void installOdbcDriver()
   {
      label_.setText("The " + info_.getName() + " driver is being installed...");
      
      server_.installOdbcDriver(
         info_.getName(), 
         options_.getIntermediateInstallPath(),
         new ServerRequestCallback<ConsoleProcess>() {
   
            @Override
            public void onResponseReceived(ConsoleProcess proc)
            {
               attachToProcess(proc);
               proc.addProcessExitHandler(
                  new ProcessExitEvent.Handler()
                  {
                     @Override
                     public void onProcessExit(ProcessExitEvent event)
                     {
                        if (event.getExitCode() != 0) {
                           label_.setText("Installation for the " + info_.getName() + " driver failed with status " + event.getExitCode() + ".");
                        }
                        else {
                           server_.getOdbcConnectionContext(
                              info_.getName(), 
                              new ServerRequestCallback<NewConnectionInfo>() {
                                 @Override
                                 public void onResponseReceived(NewConnectionInfo info)
                                 {
                                    if (!StringUtil.isNullOrEmpty(info.getError())) {
                                       globalDisplay_.showErrorMessage(
                                          "Installation failed",
                                          info.getError());

                                       label_.setText(info.getError());
                                    }
                                    else {
                                       label_.setText("The " + info_.getName() + " driver is now installed!");
                                       nextPageEnabledOperation_.execute(true);
                                       driverInstalled_ = true;

                                       options_.setIntermediateSnippet(info.getSnippet());
                                    }
                                 }

                                 @Override
                                 public void onError(ServerError error)
                                 {
                                    Debug.logError(error);
                                    globalDisplay_.showErrorMessage(
                                       "Installation failed",
                                       error.getMessage());
                                 }
                              }
                           );
                        }
                     }
                  }); 
            } 

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               globalDisplay_.showErrorMessage(
                  "Installation failed",
                  error.getUserMessage());
            }
         });
   }

   public void initializeInfo(NewConnectionInfo info)
   {
      info_ = info;

      if (!driverInstalled_)
         nextPageEnabledOperation_.execute(false);
      
      if (!installationAttempted_) {
         installationAttempted_ = true;
         installOdbcDriver();
      }
   }

   public ConnectionOptions collectInput()
   {
      return options_;
   }

   public void setIntermediateResult(ConnectionOptions result) 
   {
      options_ = result;
   }
   
   public interface Styles extends CssResource
   {
   }

   public void setNextPageEnabled(OperationWithInput<Boolean> operation)
   {
      nextPageEnabledOperation_ = operation;
   }
   
   private ConnectionsServerOperations server_;
   private GlobalDisplay globalDisplay_;

   @UiField
   ConsoleProgressWidget consoleProgressWidget_;

   @UiField
   Label label_;

   private NewConnectionInfo info_;

   private Widget mainWidget_;

   private HandlerRegistrations registrations_ = new HandlerRegistrations();

   private ConsoleProcess consoleProcess_;

   private OperationWithInput<Boolean> nextPageEnabledOperation_;

   private Boolean installationAttempted_ = false;

   private Boolean driverInstalled_ = false;

   private ConnectionOptions options_;
}
