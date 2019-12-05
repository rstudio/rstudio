/*
 * DependencyManager.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
import java.util.LinkedList;
import java.util.List;

import org.rstudio.core.client.CommandWith2Args;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.dependencies.events.InstallShinyEvent;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.common.dependencies.model.DependencyList;
import org.rstudio.studio.client.common.dependencies.model.DependencyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DependencyManager implements InstallShinyEvent.Handler,
                                          PackageStateChangedHandler
{
   class DependencyRequest
   {
      DependencyRequest(
            String progressCaptionIn,
            String userActionIn,
            CommandWith2Args<String,CommandWithArg<Boolean>> userPromptIn,
            List<Dependency> dependenciesIn,
            boolean silentEmbeddedUpdateIn,
            CommandWithArg<Boolean> onCompleteIn)
      {
         progressCaption = progressCaptionIn;
         userAction = userActionIn;
         userPrompt = userPromptIn;
         dependencies = dependenciesIn;
         silentEmbeddedUpdate = silentEmbeddedUpdateIn;
         onComplete = onCompleteIn;
      }
      String progressCaption;
      String userAction;
      CommandWith2Args<String,CommandWithArg<Boolean>> userPrompt;
      List<Dependency> dependencies;
      boolean silentEmbeddedUpdate;
      CommandWithArg<Boolean> onComplete;
   }
   
   @Inject
   public DependencyManager(GlobalDisplay globalDisplay,
                            DependencyServerOperations server,
                            EventBus eventBus,
                            Session session,
                            Commands commands)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
      satisfied_ = new ArrayList<Dependency>();
      requestQueue_ = new LinkedList<DependencyRequest>();
      session_ = session;
      commands_ = commands;
      
      eventBus.addHandler(InstallShinyEvent.TYPE, this);
      eventBus.addHandler(PackageStateChangedEvent.TYPE, this);
   }
   
   public void withDependencies(String progressCaption,
        CommandWith2Args<String,CommandWithArg<Boolean>> userPrompt,
        List<Dependency> dependencies,
        boolean silentEmbeddedUpdate,
        CommandWithArg<Boolean> onComplete)
   {
      withDependencies(progressCaption,
                       null,
                       userPrompt,
                       dependencies,
                       silentEmbeddedUpdate,
                       onComplete);
   }
   
   public void withDependencies(String progressCaption,
                                String userAction,
                                List<Dependency> dependencies,
                                boolean silentEmbeddedUpdate,
                                final CommandWithArg<Boolean> onComplete)
   {
      withDependencies(progressCaption, 
                       userAction, 
                       null, 
                       dependencies, 
                       silentEmbeddedUpdate,
                       onComplete);
   }
   
   public void withRoxygen(String progressCaption, String userAction, final Command command)
   {
      withDependencies(
            progressCaption,
            userAction,
            getFeatureDependencies("roxygen"),
            false,
            succeeded -> { if (succeeded) command.execute(); });
   }
   
   public void withThemes(String userAction, final Command command)
   {
      withDependencies(
         "Converting Theme",
         userAction,
         getFeatureDependencies("theme_conversion"),
         true,
         succeeded ->
         {
            if (succeeded)
               command.execute();
         });
   }
   
   public void withR2D3(String userAction, final Command command)
   {
      withDependencies(
        "R2D3",
         userAction,
         getFeatureDependencies("r2d3"),
         true,
         succeeded ->
         {
            if (succeeded)
               command.execute();
         });
   }
   
   public void withRPlumber(String userAction, final Command command)
   {
      withDependencies(
        "Plumber",
         userAction,
         getFeatureDependencies("plumber"),
         true,
         new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
      );
   }

   public void withPackrat(String userAction, final Command command)
   {
      withDependencies(
         "Packrat",
         userAction,
         getFeatureDependencies("packrat"),
         false,
         new CommandWithArg<Boolean>()
         {
            @Override
            public void execute(Boolean succeeded)
            {
               if (succeeded)
                  command.execute();
            }
         });
   }
   
   public void withRenv(String userAction, final CommandWithArg<Boolean> onSuccess)
   {
      withDependencies(
            "renv",
            userAction,
            getFeatureDependencies("renv"),
            false,
            onSuccess);
   }
   
   public void withRSConnect(String userAction, 
         boolean requiresRmarkdown,
         CommandWith2Args<String, CommandWithArg<Boolean>> userPrompt, 
         final CommandWithArg<Boolean> onCompleted)
   {
      // build dependency array
      List<Dependency> deps = getFeatureDependencies("rsconnect");
      if (requiresRmarkdown)
         deps.addAll(getFeatureDependencies("rmarkdown"));
      
      withDependencies(
        "Publishing",
        userAction,
        userPrompt,
        deps,
        true, // silently update any embedded packages needed (none at present)
        onCompleted
      );
   }
   
   public void withRMarkdown(String userAction, final Command command)
   {
      withRMarkdown("R Markdown", userAction, command);
   }

   public void withRMarkdown(String progressCaption, String userAction, 
         final Command command)
   {
     withRMarkdown(
        progressCaption,
        userAction, 
        succeeded ->
        {
           if (succeeded)
              command.execute();
        }
     );
   }
   
   public void withRMarkdown(String progressCaption, String userAction, 
         final CommandWithArg<Boolean> command)
   {
     withDependencies(
        progressCaption,
        userAction, 
        getFeatureDependencies("rmarkdown"),
        true, // we want to update to the embedded version if needed
        succeeded -> 
        {
           if (succeeded)
           {
              // if we successfully installed the latest R Markdown version,
              // update the session cache of package information.
              session_.getSessionInfo().setKnitParamsAvailable(true);
              session_.getSessionInfo().setRMarkdownPackageAvailable(true);
              session_.getSessionInfo().setKnitWorkingDirAvailable(true);
              session_.getSessionInfo().setPptAvailable(true);
              
              // restore removed commands
              commands_.knitWithParameters().restore();
           }
           command.execute(succeeded);
        });
   }

   public void withShiny(final String userAction, final Command command)
   {
      // create user prompt command
      CommandWith2Args<String, CommandWithArg<Boolean>> userPrompt =
            new CommandWith2Args<String, CommandWithArg<Boolean>>() {
         @Override
         public void execute(final String unmetDeps, 
                             final CommandWithArg<Boolean> responseCommand)
         {
            globalDisplay_.showYesNoMessage(
              MessageDialog.QUESTION,
              "Install Shiny Package", 
              userAction + " requires installation of an updated version " +
              "of the shiny package.\n\nDo you want to install shiny now?",
                  false, // include cancel
                  new Operation() 
                  {
                     @Override
                     public void execute()
                     {
                        responseCommand.execute(true);
                     }
                  },
                  new Operation() 
                  {
                     @Override
                     public void execute()
                     {
                        responseCommand.execute(false);
                     }
                  },
                  true);
          }
       };
       
       // perform dependency resolution 
       withDependencies(
          "Checking installed packages",
          userPrompt,
          getFeatureDependencies("shiny"),
          true,
          new CommandWithArg<Boolean>()
          {
            @Override
            public void execute(Boolean succeeded)
            {
               if (succeeded)
                  command.execute();
            }
          }
       ); 
   }
   
   public void withShinyAddins(final Command command)
   {
      withDependencies(
        "Checking installed packages",
        "Executing addins", 
        getFeatureDependencies("shiny_addins"),
        false,
        new CommandWithArg<Boolean>()
        {
         @Override
         public void execute(Boolean succeeded)
         {
            if (succeeded)
               command.execute();
         }
        }
     );
   }
   
   @Override
   public void onInstallShiny(InstallShinyEvent event)
   {
      withShiny(event.getUserAction(), 
                new Command() { public void execute() {}});
   }
   
   public void withReticulate(final String progressCaption,
                              final String userPrompt,
                              final Command command)
   {
      withDependencies(
            progressCaption,
            userPrompt,
            getFeatureDependencies("reticulate"),
            true,
            new CommandWithArg<Boolean>()
            {
               @Override
               public void execute(Boolean succeeded)
               {
                  if (succeeded)
                     command.execute();
               }
            });
   }
   
   public void withStan(final String progressCaption,
                        final String userPrompt,
                        final Command command)
   {
      withDependencies(
            progressCaption,
            userPrompt,
            getFeatureDependencies("stan"),
            true,
            (Boolean success) -> { if (success) command.execute(); });
   }
   
   public void withTinyTeX(final String progressCaption,
                           final String userPrompt,
                           final Command command)
   {
      withDependencies(
            progressCaption,
            userPrompt,
            getFeatureDependencies("tinytex"),
            true,
            (Boolean success) -> { if (success) command.execute(); });
   }
   
   @Override
   public void onPackageStateChanged(PackageStateChangedEvent event)
   {
      // when the package state changes, clear the dependency cache -- this
      // is extremely conservative as it's unlikely most (or any) of the
      // packages have been invalidated, but it's safe to do so since it'll
      // just cause us to hit the server once more to verify
      satisfied_.clear();
   }

   public void withDataImportCSV(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from CSV",
        userAction, 
        getFeatureDependencies("import_csv"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withDataImportSAV(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from SPSS, SAS and Stata",
        userAction, 
        getFeatureDependencies("import_sav"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withDataImportXLS(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from Excel",
        userAction, 
        getFeatureDependencies("import_xls"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withDataImportXML(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from XML",
        userAction, 
        getFeatureDependencies("import_xml"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withDataImportJSON(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from JSON",
        userAction, 
        getFeatureDependencies("import_json"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }

   public void withDataImportJDBC(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from JDBC",
        userAction, 
        getFeatureDependencies("import_jdbc"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withDataImportODBC(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from ODBC",
        userAction, 
        getFeatureDependencies("import_odbc"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }

   public void withDataImportMongo(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from Mongo DB",
        userAction, 
        getFeatureDependencies("import_mongo"),
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withProfvis(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Profiler",
        userAction, 
        getFeatureDependencies("profvis"),
        true, // update profvis if needed
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }

   public void withConnectionPackage(String connectionName,
                                     String packageName,
                                     String packageVersion,
                                     final Operation operation)
   {
     withDependencies(
        "Preparing Connection",
        connectionName, 
        connectionPackageDependencies(packageName, packageVersion), 
        false,
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 operation.execute();
           }
        }
     );
   }

   public void withKeyring(final Command command)
   {
     withDependencies(
        "Preparing Keyring",
        "Using keyring", 
        getFeatureDependencies("keyring"),
        true, // update keyring if needed
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }
   
   public void withOdbc(final Command command, final String name)
   {
     withDependencies(
        "Preparing " + name,
        "Using " + name, 
        getFeatureDependencies("odbc"),
        true, // update odbc if needed
        new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
     );
   }

   public void withTestPackage(final Command command, boolean useTestThat)
   {
      List<Dependency> dependencies = new ArrayList<Dependency>();
      String message;
      if (useTestThat)
      {
         dependencies.addAll(getFeatureDependencies("testthat"));
         message = "Using testthat";
      }
      else
      {
         dependencies.addAll(getFeatureDependencies("shinytest"));
         message = "Using shinytest";
      }

      withDependencies(
         "Preparing Tests",
         message, 
         dependencies, 
         true, // update package if needed
         new CommandWithArg<Boolean>()
         {
            @Override
            public void execute(Boolean succeeded)
            {
               if (succeeded) {
                  command.execute();
               }
            }
         }
      );
   }

   public void withDBI(String userAction, final Command command)
   {
      withDependencies(
        "DBI",
         userAction,
         getFeatureDependencies("dbi"),
         true,
         new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
      );
   }

   public void withRSQLite(String userAction, final Command command)
   {
      withDependencies(
        "RSQLite",
         userAction,
         getFeatureDependencies("rsqlite"),
         true,
         new CommandWithArg<Boolean>()
        {
           @Override
           public void execute(Boolean succeeded)
           {
              if (succeeded)
                 command.execute();
           }
        }
      );
   }
   
   private ArrayList<Dependency> connectionPackageDependencies(
              String packageName,
              String packageVersion)
   {
    ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage(packageName, packageVersion));

      return deps;
   }
   
   private void withDependencies(String progressCaption,
         final String userAction,
         final CommandWith2Args<String,CommandWithArg<Boolean>> userPrompt,
         List<Dependency> dependencies, 
         final boolean silentEmbeddedUpdate,
         final CommandWithArg<Boolean> onComplete)
   {
      // add the request to the queue rather than processing it right away; 
      // this frees us of the burden of trying to de-dupe requests for the
      // same packages which may occur simultaneously (since we also cache 
      // results, all such duplicate requests will return simultaneously, fed
      // by a single RPC)
      requestQueue_.add(new DependencyRequest(progressCaption, userAction, 
            userPrompt, dependencies, silentEmbeddedUpdate, 
            new CommandWithArg<Boolean>()
            {
               @Override
               public void execute(Boolean arg)
               {
                  // complete the user action, if any
                  onComplete.execute(arg);

                  // process the next request in the queue
                  processingQueue_ = false;
                  processRequestQueue();
               }
            }));
      processRequestQueue();
   }
   
   private void processRequestQueue()
   {
      if (processingQueue_ == true || requestQueue_.isEmpty())
         return;
      processingQueue_ = true;
      processDependencyRequest(requestQueue_.pop());
   }
   
   private void processDependencyRequest(final DependencyRequest req)
   {
      // convert dependencies to JsArray, excluding satisfied dependencies
      final JsArray<Dependency> deps = JsArray.createArray().cast();
      for (Dependency dep: req.dependencies)
      {
         boolean satisfied = false;
         for (Dependency d: satisfied_)
         {
            if (dep.isEqualTo(d))
            {
               satisfied = true;
               break;
            }
         }
         if (!satisfied)
            deps.push(dep);
      }
      
      // if no unsatisfied dependencies were found, we're done already
      if (deps.length() == 0)
      {
         req.onComplete.execute(true);
         return;
      }

      // create progress indicator
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            250,
            req.progressCaption + "...").getIndicator();
      
      // query for unsatisfied dependencies
      server_.unsatisfiedDependencies(
            deps, req.silentEmbeddedUpdate, 
            new ServerRequestCallback<JsArray<Dependency>>() {

         @Override
         public void onResponseReceived(
                              final JsArray<Dependency> unsatisfiedDeps)
         {
            progress.onCompleted();
            updateSatisfied(deps, unsatisfiedDeps);
            
            // if we've satisfied all dependencies then execute the command
            if (unsatisfiedDeps.length() == 0)
            {
               req.onComplete.execute(true);
               return;
            }
            
            // check to see if we can satisfy the version requirement for all
            // dependencies
            String unsatisfiedVersions = "";
            for (int i = 0; i < unsatisfiedDeps.length(); i++)
            {
               if (!unsatisfiedDeps.get(i).getVersionSatisfied())
               {
                  unsatisfiedVersions += unsatisfiedDeps.get(i).getName() + 
                       " " + unsatisfiedDeps.get(i).getVersion();
                  String version = unsatisfiedDeps.get(i).getAvailableVersion();
                  if (version.isEmpty())
                     unsatisfiedVersions += " is not available\n";
                  else
                     unsatisfiedVersions += " is required but " + version + 
                        " is available\n";
               }
            }
            
            if (!unsatisfiedVersions.isEmpty())
            {
               // error if we can't satisfy requirements
               globalDisplay_.showErrorMessage(
                     StringUtil.isNullOrEmpty(req.userAction) ?
                           "Packages Not Found" : req.userAction, 
                     "Required package versions could not be found:\n\n" +
                     unsatisfiedVersions + "\n" +
                     "Check that getOption(\"repos\") refers to a CRAN " + 
                     "repository that contains the needed package versions.");
               req.onComplete.execute(false);
            }
            else
            {
               // otherwise ask the user if they want to install the 
               // unsatisifed dependencies
               final CommandWithArg<Boolean> installCommand = 
                  new CommandWithArg<Boolean>() {
                  @Override
                  public void execute(Boolean confirmed)
                  {
                     // bail if user didn't confirm
                     if (!confirmed)
                     {
                        req.onComplete.execute(false);
                        return;
                     }

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
                           req.silentEmbeddedUpdate, 
                           req.onComplete);
                  }
               };
               
               if (req.userPrompt != null)
               {
                  req.userPrompt.execute(describeDepPkgs(unsatisfiedDeps), 
                         new CommandWithArg<Boolean>()
                         {
                           @Override
                           public void execute(Boolean arg)
                           {
                              installCommand.execute(arg);
                           }
                         });
               }
               else
               {
                  confirmPackageInstallation(req.userAction, 
                                             unsatisfiedDeps,
                                             installCommand);
               }
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            progress.onError(error.getUserMessage());
            req.onComplete.execute(false);
         }
      });
      
   }
   
   private void installDependencies(final JsArray<Dependency> dependencies,
                                    final boolean silentEmbeddedUpdate,
                                    final CommandWithArg<Boolean> onComplete)
   {
      server_.installDependencies(
         dependencies, 
         new ServerRequestCallback<ConsoleProcess>() {
   
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
                        ifDependenciesSatisifed(dependencies, 
                              silentEmbeddedUpdate, 
                              new CommandWithArg<Boolean>(){
                           @Override
                           public void execute(Boolean succeeded)
                           {
                              dialog.hide();
                              onComplete.execute(succeeded);
                           }
                        });     
                     }
                  }); 
            } 

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               globalDisplay_.showErrorMessage(
                     "Dependency installation failed",
                     error.getUserMessage());
               onComplete.execute(false);
            }
         });
   }
   
   private void ifDependenciesSatisifed(JsArray<Dependency> dependencies,
                                boolean silentEmbeddedUpdate,
                                final CommandWithArg<Boolean> onComplete)
   {
      server_.unsatisfiedDependencies(
        dependencies, silentEmbeddedUpdate, 
        new ServerRequestCallback<JsArray<Dependency>>() {
           
           @Override
           public void onResponseReceived(JsArray<Dependency> dependencies)
           {
              onComplete.execute(dependencies.length() == 0);
           }

           @Override
           public void onError(ServerError error)
           {
              Debug.logError(error);
              globalDisplay_.showErrorMessage(
                    "Could not determine available packages",
                    error.getUserMessage());
              onComplete.execute(false);
           }
        });
   }
   
   private void confirmPackageInstallation(
      String userAction, 
      final JsArray<Dependency> dependencies,
      final CommandWithArg<Boolean> onComplete)
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
         
         msg = "requires updated versions of the following packages: " + 
               describeDepPkgs(dependencies) + ". " +
               "\n\nDo you want to install these packages now?";
      }
      
      if (userAction != null)
      {
         globalDisplay_.showYesNoMessage(
            MessageDialog.QUESTION,
            "Install Required Packages", 
            userAction + " " + msg,
            false,
            new Operation() {
               @Override
               public void execute()
               {
                  onComplete.execute(true);
               }
            },
            new Operation() {
               @Override
               public void execute()
               {
                  onComplete.execute(false);
               }
            },
            true);
      }
      else
      {
         onComplete.execute(false);
      }
   }
   
   private String describeDepPkgs(JsArray<Dependency> dependencies)
   {
      ArrayList<String> deps = new ArrayList<String>();
      for (int i = 0; i < dependencies.length(); i++)
         deps.add(dependencies.get(i).getName());
      return StringUtil.join(deps, ", ");
   }
   
   public void withUnsatisfiedDependencies(final Dependency dependency,
                                           final ServerRequestCallback<JsArray<Dependency>> requestCallback)
   {
      // determine if already satisfied
      for (Dependency d: satisfied_)
      {
         if (d.isEqualTo(dependency))
         {
            JsArray<Dependency> empty = JsArray.createArray().cast();
            requestCallback.onResponseReceived(empty);
            return;
         }
      }

      List<Dependency> dependencies = new ArrayList<Dependency>();
      dependencies.add(dependency);
      withUnsatisfiedDependencies(dependencies, requestCallback);
   }
   
   private void withUnsatisfiedDependencies(final List<Dependency> dependencies,
                                           final ServerRequestCallback<JsArray<Dependency>> requestCallback)
   {
      final JsArray<Dependency> jsDependencies = 
            JsArray.createArray(dependencies.size()).cast();
      for (int i = 0; i < dependencies.size(); i++)
         jsDependencies.set(i, dependencies.get(i));
      
      server_.unsatisfiedDependencies(
            jsDependencies,
            false,
            new ServerRequestCallback<JsArray<Dependency>>()
            {
               @Override
               public void onResponseReceived(JsArray<Dependency> unsatisfied)
               {
                  updateSatisfied(jsDependencies, unsatisfied);
                  requestCallback.onResponseReceived(unsatisfied);
               }

               @Override
               public void onError(ServerError error)
               {
                  requestCallback.onError(error);
               }
            });
   }
   
   /**
    * Updates the cache of satisfied dependencies.
    * 
    * @param all The dependencies that were requested
    * @param unsatisfied The dependencies that were not satisfied
    */
   private void updateSatisfied(JsArray<Dependency> all, 
                                JsArray<Dependency> unsatisfied)
   {
      for (int i = 0; i < all.length(); i++)
      {
         boolean satisfied = true;
         for (int j = 0; j < unsatisfied.length(); j++)
         {
            if (unsatisfied.get(j).isEqualTo(all.get(i)))
            {
               satisfied = false;
               break;
            }
         }
         if (satisfied)
         {
            satisfied_.add(all.get(i));
         }
      }
   }
   
   /**
    * Retrieves a list of R package dependencies for an IDE feature.
    * 
    * @param feature The identifier of the IDE feature.
    * @return An array of R packages the feature depends on.
    */
   private ArrayList<Dependency> getFeatureDependencies(String feature)
   {
      // The list of R package dependencies to return
      ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

      // Read a list of package dependencies for this feature
      DependencyList list = session_.getSessionInfo().getPackageDependencies();
      JsArrayString packages = list.getFeatureDependencies(feature);
      
      // Find the metadata for each package (where we should install it from,
      // what version we need, etc.)
      for (int i = 0; i < packages.length(); i++)
      {
         Dependency dep = list.getPackage(packages.get(i));
         if (dep == null)
         {
            Debug.logWarning("No dependency record found for package '" + 
                             packages.get(i) + "' (required by feature '" +
                             feature + "')");
            continue;
         }
         dep.setName(packages.get(i));
         dependencies.add(dep);
      }
      
      // Return the completed list
      return dependencies;
   }
   
   private boolean processingQueue_ = false;
   private final LinkedList<DependencyRequest> requestQueue_;
   private final GlobalDisplay globalDisplay_;
   private final DependencyServerOperations server_;
   private final ArrayList<Dependency> satisfied_;
   private final Session session_;
   private final Commands commands_;
}
