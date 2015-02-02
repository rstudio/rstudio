/*
 * DependencyManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.dependencies.events.InstallShinyEvent;
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
public class DependencyManager implements InstallShinyEvent.Handler
{
   @Inject
   public DependencyManager(GlobalDisplay globalDisplay,
                            DependencyServerOperations server,
                            EventBus eventBus)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      eventBus.addHandler(InstallShinyEvent.TYPE, this);
   }
   
   public void withDependencies(String progressCaption,
                                CommandWithArg<Command> userPrompt,
                                Dependency[] dependencies, 
                                boolean silentUpdate,
                                Command command)
   {
      withDependencies(progressCaption,
                       null,
                       userPrompt,
                       dependencies,
                       silentUpdate,
                       command);
   }
   
   public void withDependencies(String progressCaption,
                                String userAction,
                                Dependency[] dependencies, 
                                boolean silentUpdate,
                                final Command command)
   {
      withDependencies(progressCaption, 
                       userAction, 
                       null, 
                       dependencies, 
                       silentUpdate,
                       command);
   }
   
   public void withPackrat(String userAction, final Command command)
   {
      withDependencies(
         "Packrat",
         userAction,
         new Dependency[] {
            Dependency.cranPackage("packrat", "0.4.3")
         },
         false,
         command);
   }

   public void withShinyapps(String userAction, final Command command)
   {
      withDependencies(
        "Shinyapps",
        userAction,
        new Dependency[] {
          Dependency.cranPackage("digest", "0.6"),
          Dependency.cranPackage("RCurl", "1.95"),
          Dependency.cranPackage("RJSONIO", "1.0"),
          Dependency.embeddedPackage("shinyapps")
        },
        false,
        command
      );
   }
   
   public void withRMarkdown(String userAction, final Command command)
   {
     withDependencies(   
        "R Markdown",
        userAction, 
        new Dependency[] {
          Dependency.cranPackage("knitr", "1.6"),
          Dependency.cranPackage("yaml", "2.1.5"),
          Dependency.cranPackage("htmltools", "0.2.4"),
          Dependency.cranPackage("caTools", "1.14"),
          Dependency.cranPackage("bitops", "1.0-6"),
          Dependency.cranPackage("rmarkdown", "0.4.2")
        }, 
        true,
        command
     );
   }
 
   public void withShiny(final String userAction, final Command command)
   {
      // create user prompt command
      CommandWithArg<Command> userPrompt = new CommandWithArg<Command>() {
         @Override
         public void execute(final Command yesCommand)
         {
            globalDisplay_.showYesNoMessage(
              MessageDialog.QUESTION,
              "Install Shiny Package", 
              userAction + " requires installation of an updated version " +
              "of the shiny package.\n\nDo you want to install shiny now?",
                  new Operation() {

                     @Override
                     public void execute()
                     {
                        yesCommand.execute();
                     }
                  },
                  true);
          }
       };
       
       // perform dependency resolution 
       withDependencies(
          "Checking installed packages",
          userPrompt,
          new Dependency[] {
            Dependency.cranPackage("httpuv", "1.2"),
            Dependency.cranPackage("caTools", "1.13"),
            Dependency.cranPackage("RJSONIO", "1.0"),
            Dependency.cranPackage("xtable", "1.7"),
            Dependency.cranPackage("digest", "0.6"),
            Dependency.cranPackage("htmltools", "0.2.4"),
            Dependency.cranPackage("shiny", "0.10.0")
          }, 
          true,
          command
       ); 
   }
   
   @Override
   public void onInstallShiny(InstallShinyEvent event)
   {
      withShiny(event.getUserAction(), 
                new Command() { public void execute() {}});
   }
   
   private void withDependencies(String progressCaption,
                                 final String userAction,
                                 final CommandWithArg<Command> userPrompt,
                                 Dependency[] dependencies, 
                                 final boolean silentUpdate,
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
            deps, silentUpdate, new ServerRequestCallback<JsArray<Dependency>>() {

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
               Command installCommand = new Command() {
                  @Override
                  public void execute()
                  {
                     // the incoming JsArray from the server may not serialize
                     // as expected when this code is executed from a satellite
                     // (see RemoteServer.sendRequestViaMainWorkbench), so we
                     // clone it before passing to the dependency installer
                     JsArray<Dependency> newArray = JsArray.createArray().cast();
                     newArray.setLength(unsatisfiedDeps.length());
                     for (int i = 0; i < unsatisfiedDeps.length(); i++)
                     {
                        newArray.set(i, unsatisfiedDeps.get(i));
                     }
                     installDependencies(
                           newArray, 
                           silentUpdate, command);
                  }
               };
               
               if (userPrompt != null)
               {
                  userPrompt.execute(installCommand);
               }
               else
               {
                  confirmPackageInstallation(userAction, 
                                             unsatisfiedDeps,
                                             installCommand);                           
               }
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
                                    final boolean silentUpdate,
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
                        ifDependenciesSatisifed(dependencies, silentUpdate, new Command(){
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
                                        boolean silentUpdate,
                                        final Command onInstalled)
   {
      server_.unsatisfiedDependencies(
        dependencies, silentUpdate, new SimpleRequestCallback<JsArray<Dependency>>() {
           
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
      
      if (userAction != null)
      {
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
      else
      {
         onConfirmed.execute();
      }
   }
   
   private final GlobalDisplay globalDisplay_;
   private final DependencyServerOperations server_;
}
