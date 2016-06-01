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
                                CommandWith2Args<String,Command> userPrompt,
                                Dependency[] dependencies, 
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
                                Dependency[] dependencies, 
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

   public void withPackrat(String userAction, final Command command)
   {
      withDependencies(
         "Packrat",
         userAction,
         new Dependency[] {
            Dependency.cranPackage("packrat", "0.4.7", true)
         },
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
   
   public void withRSConnect(String userAction, 
         boolean requiresRmarkdown,
         CommandWith2Args<String, Command> userPrompt, 
         final CommandWithArg<Boolean> onCompleted)
   {
      // build dependency array
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("digest", "0.6"));
      deps.add(Dependency.cranPackage("RCurl", "1.95"));
      deps.add(Dependency.cranPackage("RJSONIO", "1.0"));
      deps.add(Dependency.cranPackage("PKI", "0.1"));
      deps.add(Dependency.cranPackage("rstudioapi", "0.5"));
      deps.add(Dependency.cranPackage("yaml", "2.1.5"));
      if (requiresRmarkdown)
         deps.addAll(rmarkdownDependencies());
      deps.add(Dependency.cranPackage("packrat", "0.4.7", true));
      deps.add(Dependency.cranPackage("rsconnect", "0.4.3", true));
      
      withDependencies(
        "Publishing",
        userAction,
        userPrompt,
        deps.toArray(new Dependency[deps.size()]),
        true, // we want the embedded rsconnect package to be updated if needed
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
     withDependencies(   
        progressCaption,
        userAction, 
        rmarkdownDependenciesArray(), 
        true, // we want to update to the embedded versoin if needed
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
   
   public static List<Dependency> rmarkdownDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("evaluate", "0.8"));
      deps.add(Dependency.cranPackage("digest", "0.6"));
      deps.add(Dependency.cranPackage("formatR", "1.1"));
      deps.add(Dependency.cranPackage("highr", "0.3"));
      deps.add(Dependency.cranPackage("markdown", "0.7"));
      deps.add(Dependency.cranPackage("stringr", "0.6"));
      deps.add(Dependency.cranPackage("yaml", "2.1.5"));
      deps.add(Dependency.cranPackage("Rcpp", "0.11.5"));
      deps.add(Dependency.cranPackage("htmltools", "0.2.4"));
      deps.add(Dependency.cranPackage("caTools", "1.14"));
      deps.add(Dependency.cranPackage("bitops", "1.0-6"));
      deps.add(Dependency.cranPackage("knitr", "1.13", true));
      deps.add(Dependency.cranPackage("jsonlite", "0.9.19"));
      deps.add(Dependency.cranPackage("base64enc", "0.1-3"));
      deps.add(Dependency.embeddedPackage("rmarkdown"));
      return deps;
   }
   
   public static Dependency[] rmarkdownDependenciesArray()
   {
      List<Dependency> deps = rmarkdownDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }
 
   public void withShiny(final String userAction, final Command command)
   {
      // create user prompt command
      CommandWith2Args<String, Command> userPrompt =
            new CommandWith2Args<String, Command>() {
         @Override
         public void execute(final String unmetDeps, final Command yesCommand)
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
          shinyDependenciesArray(
              "0.11.0", // shiny version
              "0.2.6"), // htmltools version
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
      // define dependencies
      ArrayList<Dependency> deps = shinyDependencies(
                                    "0.13", // shiny version
                                    "0.3"); // htmltools version
      deps.add(Dependency.cranPackage("miniUI", "0.1.1", true));
      deps.add(Dependency.cranPackage("rstudioapi", "0.5", true));
      
      withDependencies(   
        "Checking installed packages",
        "Executing addins", 
        deps.toArray(new Dependency[deps.size()]),
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
   
   private Dependency[] shinyDependenciesArray(String shinyVersion,
                                               String htmltoolsVersion)
   {
      ArrayList<Dependency> deps = shinyDependencies(shinyVersion,
                                                     htmltoolsVersion);
      return deps.toArray(new Dependency[deps.size()]);
   }

   private ArrayList<Dependency> shinyDependencies(String shinyVersion,
         String htmltoolsVersion)
         {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("Rcpp", "0.11.5"));
      deps.add(Dependency.cranPackage("httpuv", "1.3.3"));
      deps.add(Dependency.cranPackage("mime", "0.3"));
      deps.add(Dependency.cranPackage("jsonlite", "0.9.19"));
      deps.add(Dependency.cranPackage("xtable", "1.7"));
      deps.add(Dependency.cranPackage("digest", "0.6"));
      deps.add(Dependency.cranPackage("R6", "2.0"));
      deps.add(Dependency.cranPackage("htmltools", htmltoolsVersion));
      deps.add(Dependency.cranPackage("shiny", shinyVersion, true));
      return deps;
         }
   
   @Override
   public void onInstallShiny(InstallShinyEvent event)
   {
      withShiny(event.getUserAction(), 
                new Command() { public void execute() {}});
   }
   
   public void withDataImportCSV(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from CSV",
        userAction, 
        dataImportCsvDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportCsvDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("readr", "0.2.2"));
      deps.add(Dependency.cranPackage("Rcpp", "0.11.5"));
      return deps;
   }
   
   private Dependency[] dataImportCsvDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportCsvDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }
   
   public void withDataImportSAV(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from SPSS, SAS and Stata",
        userAction, 
        dataImportSavDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportSavDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("haven", "0.2.0"));
      deps.add(Dependency.cranPackage("Rcpp", "0.11.5"));
      return deps;
   }
   
   private Dependency[] dataImportSavDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportSavDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportXLS(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from Excel",
        userAction, 
        dataImportXlsDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportXlsDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("readxl", "0.1.0"));
      deps.add(Dependency.cranPackage("Rcpp", "0.11.5"));
      return deps;
   }
   
   private Dependency[] dataImportXlsDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportXlsDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportXML(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from XML",
        userAction, 
        dataImportXmlDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportXmlDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("xml2", "0.1.2"));
      return deps;
   }
   
   private Dependency[] dataImportXmlDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportXmlDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportJSON(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from JSON",
        userAction, 
        dataImportJsonDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportJsonDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("jsonlite", "0.9.19"));
      return deps;
   }
   
   private Dependency[] dataImportJsonDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportJsonDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportJDBC(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from JDBC",
        userAction, 
        dataImportJdbcDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportJdbcDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("RJDBC", "0.2-5"));
      deps.add(Dependency.cranPackage("rJava", "0.4-15"));
      return deps;
   }
   
   private Dependency[] dataImportJdbcDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportJdbcDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportODBC(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from ODBC",
        userAction, 
        dataImportOdbcDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportOdbcDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("RODBC", "1.3-12"));
      return deps;
   }
   
   private Dependency[] dataImportOdbcDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportOdbcDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withDataImportMongo(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Import from Mongo DB",
        userAction, 
        dataImportMongoDependenciesArray(), 
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
   
   private ArrayList<Dependency> dataImportMongoDependencies()
   {
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(Dependency.cranPackage("mongolite", "0.8"));
      deps.add(Dependency.cranPackage("jsonlite", "0.9.19"));
      return deps;
   }
   
   private Dependency[] dataImportMongoDependenciesArray()
   {
      ArrayList<Dependency> deps = dataImportMongoDependencies();
      return deps.toArray(new Dependency[deps.size()]);
   }

   public void withProfvis(String userAction, final Command command)
   {
     withDependencies(
        "Preparing Profiler",
        userAction, 
        new Dependency[] {
           Dependency.cranPackage("stringr", "0.6"),
           Dependency.cranPackage("jsonlite", "0.9.19"),
           Dependency.cranPackage("htmltools", "0.3"),
           Dependency.cranPackage("yaml", "2.1.5"),
           Dependency.cranPackage("htmlwidgets", "0.6", true),
           Dependency.cranPackage("profvis", "0.3.2", true)
          
        }, 
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
   
   private void withDependencies(String progressCaption,
                                 final String userAction,
                                 final CommandWith2Args<String,Command> userPrompt,
                                 Dependency[] dependencies, 
                                 final boolean silentEmbeddedUpdate,
                                 final CommandWithArg<Boolean> onComplete)
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
            deps, silentEmbeddedUpdate, 
            new ServerRequestCallback<JsArray<Dependency>>() {

         @Override
         public void onResponseReceived(
                              final JsArray<Dependency> unsatisfiedDeps)
         {
            progress.onCompleted();
            
            // if we've satisfied all dependencies then execute the command
            if (unsatisfiedDeps.length() == 0)
            {
               onComplete.execute(true);
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
               globalDisplay_.showErrorMessage(userAction, 
                     "Required package versions could not be found:\n\n" +
                     unsatisfiedVersions + "\n" +
                     "Check that getOption(\"repos\") refers to a CRAN " + 
                     "repository that contains the needed package versions.");
               onComplete.execute(false);
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
                     // bail if 
                     if (!confirmed)
                     {
                        onComplete.execute(false);
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
                           silentEmbeddedUpdate, 
                           onComplete);
                  }
               };
               
               if (userPrompt != null)
               {
                  userPrompt.execute(describeDepPkgs(unsatisfiedDeps), 
                         new Command()
                         {
                           @Override
                           public void execute()
                           {
                              installCommand.execute(true);
                           }
                         });
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
            onComplete.execute(false);
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
         onComplete.execute(true);
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
      List<Dependency> dependencies = new ArrayList<Dependency>();
      dependencies.add(dependency);
      withUnsatisfiedDependencies(dependencies, requestCallback);
   }
   
   public void withUnsatisfiedDependencies(final List<Dependency> dependencies,
                                           final ServerRequestCallback<JsArray<Dependency>> requestCallback)
   {
      JsArray<Dependency> jsDependencies = JsArray.createArray(dependencies.size()).cast();
      for (int i = 0; i < dependencies.size(); i++)
         jsDependencies.set(i, dependencies.get(i));
      
      server_.unsatisfiedDependencies(
            jsDependencies,
            false,
            requestCallback);
   }
   
   private final GlobalDisplay globalDisplay_;
   private final DependencyServerOperations server_;
}
