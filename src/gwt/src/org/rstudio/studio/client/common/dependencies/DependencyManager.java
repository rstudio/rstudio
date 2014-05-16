/*
 * DependencyManager.java
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

package org.rstudio.studio.client.common.dependencies;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.common.dependencies.model.DependencyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DependencyManager
{
   @Inject
   public DependencyManager(GlobalDisplay globalDisplay,
                            DependencyServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   
   public void withDependencies(String progressCaption,
                                final String userAction,
                                Dependency[] dependencies, 
                                final Command command)
   {
      // convert dependencies to JsArray
      JsArray<Dependency> deps = JsArray.createArray().cast();
      deps.setLength(dependencies.length);
      for (int i = 0; i<deps.length(); i++)
         deps.set(i, dependencies[i]);
      
      // create progress indicator
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            250,
            progressCaption + "...").getIndicator();
      
      // query for unsatisfied dependencies
      server_.unsatisfiedDependencies(
            deps, new ServerRequestCallback<JsArray<Dependency>>() {

         @Override
         public void onResponseReceived(
                              final JsArray<Dependency> unsatisfiedDeps)
         {
            progress.onCompleted();
            
            // if we've satisfied all dependencies then execute the command
            if (unsatisfiedDeps.length() == 0)
            {
               command.execute();
            }
            
            // otherwise ask the user if they want to install the 
            // unsatisifed dependencies
            else
            {
               confirmPackageInstallation(userAction, 
                                          unsatisfiedDeps,
                                          new Command() {

                  @Override
                  public void execute()
                  {
                     installDependencies(unsatisfiedDeps, command);
                  }
                  
               });
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            progress.onError(error.getUserMessage());
            
         }
      });
      
   }
   
   private void installDependencies(final JsArray<Dependency> dependencies,
                                    final Command onSuccess)
   {
      server_.installDependencies(
         dependencies, 
         new SimpleRequestCallback<ConsoleProcess>() {
   
            @Override
            public void onResponseReceived(ConsoleProcess proc)
            {   
               final ConsoleProgressDialog dialog = 
                     new ConsoleProgressDialog(proc, server_);
               dialog.showModal();
   
               proc.addProcessExitHandler(
                  new ProcessExitEvent.Handler()
                  {
                     @Override
                     public void onProcessExit(ProcessExitEvent event)
                     {
                        ifDependenciesSatisifed(dependencies, new Command(){
                           @Override
                           public void execute()
                           {
                              dialog.hide();
                              onSuccess.execute();
                           }
                        });     
                     }
                  }); 
            } 
         });
   }
   
   private void ifDependenciesSatisifed(JsArray<Dependency> dependencies,
                                        final Command onInstalled)
   {
      server_.unsatisfiedDependencies(
        dependencies, new SimpleRequestCallback<JsArray<Dependency>>() {
           
           @Override
           public void onResponseReceived(JsArray<Dependency> dependencies)
           {
              if (dependencies.length() == 0)
                 onInstalled.execute();
           }
        });
   }
   
   private void confirmPackageInstallation(
      String userAction, 
      final JsArray<Dependency> dependencies,
      final Command onConfirmed)
   {
      String msg = null;
      if (dependencies.length() == 1)
      {
         msg = "requires an updated version of the " + 
               dependencies.get(0).getName() + " package. " +
               "\n\nDo you want to install this package now?";
      }
      else
      {
         ArrayList<String> deps = new ArrayList<String>();
         for (int i = 0; i < dependencies.length(); i++)
            deps.add(dependencies.get(i).getName());
         
         msg = "requires updated versions of the following packages: " + 
               StringUtil.join(deps, ", ") + ". " +
               "\n\nDo you want to install these packages now?";
      }
      
      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION,
         "Install Required Packages", 
         userAction + " " + msg,
         new Operation() {

            @Override
            public void execute()
            {
               onConfirmed.execute();
            }
         },
         true);
   }
   
   private final GlobalDisplay globalDisplay_;
   private final DependencyServerOperations server_;

}
