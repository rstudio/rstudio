/*
 * Packages.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedHandler;
import org.rstudio.studio.client.workbench.views.packages.events.LoadedPackageUpdatesEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedHandler;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.CheckForUpdatesDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class Packages
      extends BasePresenter
      implements InstalledPackagesChangedHandler,
                 PackageStatusChangedHandler,
                 DeferredInitCompletedEvent.Handler,
                 PackagesDisplayObserver
{
   public interface Binder extends CommandBinder<Commands, Packages> {}

   public interface Display extends WorkbenchView
   {
      void listPackages(List<PackageInfo> packagesDS);
      
      void installPackage(PackageInstallContext installContext,
                          PackageInstallOptions defaultInstallOptions,
                          PackagesServerOperations server,
                          GlobalDisplay globalDisplay,
                          OperationWithInput<PackageInstallRequest> operation);
      
      void setPackageStatus(PackageStatus status);
  
      void setObserver(PackagesDisplayObserver observer) ;
      void setProgress(boolean showProgress);
   }
   
   @Inject
   public Packages(Display view, 
                   EventBus events,
                   PackagesServerOperations server,
                   GlobalDisplay globalDisplay,
                   Session session,
                   Binder binder,
                   Commands commands,
                   DefaultCRANMirror defaultCRANMirror)
   {
      super(view);
      view_ = view;
      server_ = server;
      globalDisplay_ = globalDisplay ;
      view_.setObserver(this) ;
      events_ = events ;
      defaultCRANMirror_ = defaultCRANMirror;
      binder.bind(commands, this);

      events.addHandler(InstalledPackagesChangedEvent.TYPE, this);
      events.addHandler(PackageStatusChangedEvent.TYPE, this);
      events.addHandler(DeferredInitCompletedEvent.TYPE, this);
      
      // make the install options persistent
      new JSObjectStateValue("packages-pane", "installOptions", ClientState.PROJECT_PERSISTENT,
            session.getSessionInfo().getClientState(), false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
               installOptions_ = value.cast();
            lastKnownState_ = installOptions_;
         }

         @Override
         protected JsObject getValue()
         {
            return installOptions_.cast();
         }

         @Override
         protected boolean hasChanged()
         {
            if (!PackageInstallOptions.areEqual(lastKnownState_, installOptions_))
            {
               lastKnownState_ = installOptions_;
               return true;
            }

            return false;
         }

         private PackageInstallOptions lastKnownState_;
      };
      
      listPackages();
   }
   
   void onInstallPackage()
   {
      withPackageInstallContext(new OperationWithInput<PackageInstallContext>(){

         @Override
         public void execute(final PackageInstallContext installContext)
         {
            if (installContext.isDefaultLibraryWriteable())
            {
               continueInstallPackage(installContext);
            }
            else
            {
               globalDisplay_.showYesNoMessage(MessageDialog.QUESTION, 
                 "Create Package Library",
                 "Would you like to create a personal library '" +
                 installContext.getDefaultUserLibraryPath() + "' " +
                 "to install packages into?",
                 false,
                 new Operation() // Yes operation
                 {
                    @Override
                    public void execute()
                    {
                       ProgressIndicator indicator = 
                             globalDisplay_.getProgressIndicator(
                                                  "Error Creating Library");
                        server_.initDefaultUserLibrary(
                              new VoidServerRequestCallback(indicator) {
                                 @Override
                                 protected void onSuccess()
                                 {
                                    // call this function back recursively
                                    // so we can retrieve the updated 
                                    // PackageInstallContext from the server
                                    onInstallPackage();
                                 }
                              });  
                     }
                 }, 
                 new Operation() // No operation
                 {
                     @Override
                     public void execute()
                     {
                        globalDisplay_.showMessage(
                              MessageDialog.WARNING,
                              "Install Packages",
                              "Unable to install packages (default library '" +
                              installContext.getDefaultLibraryPath() + "' is " +
                              "not writeable)");
                        
                     }  
                 },
                 true);
            }
         }
         
      });
   }
   
   
   private void continueInstallPackage(
                           final PackageInstallContext installContext)
   {
      // if CRAN needs to be configured then do it
      if (!installContext.isCRANMirrorConfigured())
      {
         defaultCRANMirror_.configure(new Command() {
            public void execute()
            {
               doInstallPackage(installContext); 
            } 
         });
      }
      else
      {
         doInstallPackage(installContext);
      }   
   }
  
   private void doInstallPackage(final PackageInstallContext installContext)
   {
      // if install options have not yet initialized the default library
      // path then set it now from the context
      if (StringUtil.isNullOrEmpty(installOptions_.getLibraryPath()))
      {
         installOptions_ = PackageInstallOptions.create(
                                 installOptions_.getInstallFromRepository(),
                                 installContext.getDefaultLibraryPath(), 
                                 installOptions_.getInstallDependencies());
      }
      
      view_.installPackage(
         installContext,
         installOptions_,
         server_,
         globalDisplay_,
         new OperationWithInput<PackageInstallRequest>() 
         {
            public void execute(PackageInstallRequest request)
            {
               installOptions_ = request.getOptions();
               
               boolean usingDefaultLibrary = 
                  request.getOptions().getLibraryPath().equals(
                                       installContext.getDefaultLibraryPath());

               StringBuilder command = new StringBuilder();
               command.append("install.packages(");
               
               List<String> packages = request.getPackages(); 
               if (packages != null)
               {
                  if (packages.size() > 1)
                     command.append("c(");
                  for (int i=0; i<packages.size(); i++)
                  {
                     if (i > 0)
                        command.append(", ");
                     command.append("\""); 
                     command.append(packages.get(i));
                     command.append("\"");
                  }
                  if (packages.size() > 1)
                     command.append(")");
                  
                  // dependencies
                  if (!request.getOptions().getInstallDependencies())
                     command.append(", dependencies = FALSE");
               }
               // must be a local package
               else
               {
                  // get path
                  FileSystemItem localPackage = request.getLocalPackage();
                  
                  // convert to string 
                  String path = localPackage.getPath();
                  
                  // append command
                  command.append("\"" + path + "\", repos = NULL");
                  
                  // append type = source if needed
                  if (path.endsWith(".tar.gz"))
                     command.append(", type = \"source\"");
               }
               
               if (!usingDefaultLibrary)
               {
                  command.append(", lib=\"");
                  command.append(request.getOptions().getLibraryPath());
                  command.append("\"");
               }
               
               command.append(")");
               String cmd = command.toString();
               executeWithLoadedPackageCheck(new InstallCommand(packages, cmd));
           }
         });
   }
    
   
   void onUpdatePackages()
   {
      withPackageInstallContext(new OperationWithInput<PackageInstallContext>(){

         @Override
         public void execute(final PackageInstallContext installContext)
         {
            // if there are no writeable library paths then we just
            // short circuit to all packages are up to date message
            if (installContext.getWriteableLibraryPaths().length() == 0)
            {
               globalDisplay_.showMessage(MessageDialog.INFO, 
                                          "Check for Updates", 
                                          "All packages are up to date.");
               
            }
            
            // if CRAN needs to be configured then do it
            else if (!installContext.isCRANMirrorConfigured())
            {
               defaultCRANMirror_.configure(new Command() {
                  public void execute()
                  {
                     doUpdatePackages(installContext); 
                  } 
               });
            }
            
            // otherwise we are good to go!
            else
            {
               doUpdatePackages(installContext);
            }    
         }
         
      });
   }
   
   private void doUpdatePackages(final PackageInstallContext installContext)
   {
      new CheckForUpdatesDialog(
         globalDisplay_,
         new ServerDataSource<JsArray<PackageUpdate>>() {
            public void requestData(
               ServerRequestCallback<JsArray<PackageUpdate>> requestCallback)
            {
               server_.checkForPackageUpdates(requestCallback); 
            }   
         },
         new OperationWithInput<ArrayList<PackageUpdate>>() {
            @Override
            public void execute(ArrayList<PackageUpdate> updates)
            {
               InstallCommand cmd = buildUpdatePackagesCommand(updates, 
                                                               installContext);
               executeWithLoadedPackageCheck(cmd);
            }  
      }).showModal();
   }
   
   
   private InstallCommand buildUpdatePackagesCommand(
                              ArrayList<PackageUpdate> updates,
                              final PackageInstallContext installContext)
   {
      // split the updates into their respective target libraries
      List<String> packages = new ArrayList<String>();
      LinkedHashMap<String, ArrayList<PackageUpdate>> updatesByLibPath = 
         new  LinkedHashMap<String, ArrayList<PackageUpdate>>();  
      for (PackageUpdate update : updates)
      {
         // auto-create target list if necessary
         String libPath = update.getLibPath();
         if (!updatesByLibPath.containsKey(libPath))
            updatesByLibPath.put(libPath, new ArrayList<PackageUpdate>());
         
         // insert into list
         updatesByLibPath.get(libPath).add(update); 
         
         // track global list of packages
         packages.add(update.getPackageName());
      }
      
      // generate an install packages command for each targeted library
      StringBuilder command = new StringBuilder();
      for (String libPath : updatesByLibPath.keySet())
      {
         if (command.length() > 0)
            command.append("\n");
         
         ArrayList<PackageUpdate> libPathUpdates = updatesByLibPath.get(libPath); 
         command.append("install.packages(");
         if (libPathUpdates.size() > 1)
            command.append("c(");
         for (int i=0; i<libPathUpdates.size(); i++)
         {
            PackageUpdate update = libPathUpdates.get(i);
            if (i > 0)
               command.append(", ");
            command.append("\""); 
            command.append(update.getPackageName());
            command.append("\"");
         }
         if (libPathUpdates.size() > 1)
            command.append(")");
         
         if (!libPath.equals(installContext.getDefaultLibraryPath()))
         {
            command.append(", lib=\"");
            command.append(libPath);
            command.append("\"");
         }
        
         command.append(")");
         
      }
      
      return new InstallCommand(packages, command.toString());
   }
   
   
   @Handler
   public void onRefreshPackages()
   {
      listPackages();
   }
   
   public void removePackage(final PackageInfo packageInfo)
   {
      withPackageInstallContext(new OperationWithInput<PackageInstallContext>(){

         @Override
         public void execute(final PackageInstallContext installContext)
         {
            final boolean usingDefaultLibrary = packageInfo.getLibrary().equals(
                                       installContext.getDefaultLibraryPath());
            
            StringBuilder message = new StringBuilder();
            message.append("Are you sure you wish to permanently uninstall the '"); 
            message.append(packageInfo.getName() + "' package");
            if (!usingDefaultLibrary)
            {
               message.append(" from library '");
               message.append(packageInfo.getLibrary());
               message.append("'");
            }
            message.append("? This action cannot be undone.");
               
            globalDisplay_.showYesNoMessage(
               MessageDialog.WARNING,
               "Uninstall Package ",
               message.toString(),
               new Operation() 
               {
                  @Override
                  public void execute()
                  {     
                     StringBuilder command = new StringBuilder();
                     command.append("remove.packages(\"");
                     command.append(packageInfo.getName());
                     command.append("\"");
                     if (!usingDefaultLibrary)
                     {
                        command.append(", lib=\"");
                        command.append(packageInfo.getLibrary());
                        command.append("\"");
                     }
                     command.append(")");
                     String cmd = command.toString();
                     events_.fireEvent(new SendToConsoleEvent(cmd, true));
                  }  
               },
               true); 
         }
      });
   }
      
   public void listPackages()
   {
      view_.setProgress(true);
      server_.listPackages(
            new SimpleRequestCallback<JsArray<PackageInfo>>("Error Listing Packages")
      {
         @Override
         public void onError(ServerError error)
         {
            super.onError(error);
            view_.setProgress(false);
         }

         @Override
         public void onResponseReceived(JsArray<PackageInfo> response)
         {
            // sort the packages
            allPackages_ = new ArrayList<PackageInfo>();
            for (int i=0; i<response.length(); i++)
               allPackages_.add(response.get(i));
            Collections.sort(allPackages_, new Comparator<PackageInfo>() {
               public int compare(PackageInfo o1, PackageInfo o2)
               {
                  return o1.getName().compareToIgnoreCase(o2.getName());
               }
            });
            
            view_.setProgress(false);
            setViewPackageList();
         }
      });
   }

   public void loadPackage(final String packageName, final String libName)
   {  
      // check status to make sure the package was unloaded
      checkPackageStatusOnNextConsolePrompt(packageName, libName);
      
      // send the command
      StringBuilder command = new StringBuilder();
      command.append("library(\"");
      command.append(packageName);
      command.append("\"");
      command.append(", lib.loc=\"");
      command.append(libName);
      command.append("\"");
      command.append(")");
      events_.fireEvent(new SendToConsoleEvent(command.toString(), true));
     
   }

   public void unloadPackage(String packageName, String libName)
   { 
      // check status to make sure the package was unloaded
      checkPackageStatusOnNextConsolePrompt(packageName, libName);
      
      StringBuilder command = new StringBuilder();
      command.append("detach(\"package:");
      command.append(packageName);
      command.append("\", unload=TRUE)");
      events_.fireEvent(new SendToConsoleEvent(command.toString(), true));
   }
   
   public void showHelp(PackageInfo packageInfo)
   {
      events_.fireEvent(new ShowHelpEvent(packageInfo.getUrl())) ;
   }
   
   
   public void onInstalledPackagesChanged(InstalledPackagesChangedEvent event)
   {
      listPackages() ;
   }
   
   @Override
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
   {
      listPackages();
   }
   
   public void onPackageFilterChanged(String filter)
   {
      packageFilter_ = filter.toLowerCase();
      setViewPackageList();
   }

   public void onPackageStatusChanged(PackageStatusChangedEvent event)
   {
      PackageStatus status = event.getPackageStatus();
      view_.setPackageStatus(status);
      
      // also update the list of allPackages_
      for (int i = 0; i<allPackages_.size(); i++)
      {
         PackageInfo packageInfo = allPackages_.get(i);
         if (packageInfo.getName().equals(status.getName()) &&
             packageInfo.getLibrary().equals(status.getLib()))
         {
            allPackages_.set(i, status.isLoaded() ? packageInfo.asLoaded() :
                                                    packageInfo.asUnloaded());
         }
      }
   }
   
   private void setViewPackageList()
   {
      ArrayList<PackageInfo> packages = null; ;
      
      // apply filter (if any)
      if (packageFilter_.length() > 0)
      {
         packages = new ArrayList<PackageInfo>();
         
         // first do prefix search
         for (PackageInfo pkgInfo : allPackages_)
         {
            if (pkgInfo.getName().toLowerCase().startsWith(packageFilter_))
               packages.add(pkgInfo);
         }
         
         // then do contains search on name & desc
         for (PackageInfo pkgInfo : allPackages_)
         { 
            if (pkgInfo.getName().toLowerCase().contains(packageFilter_) ||
                pkgInfo.getDesc().toLowerCase().contains(packageFilter_))
            {
               if (!packages.contains(pkgInfo))
                  packages.add(pkgInfo);
            }
         }
      }
      else
      {
         packages = allPackages_;
      }
      
      view_.listPackages(packages);
   }
   
   private void checkPackageStatusOnNextConsolePrompt(
                                         final String packageName,
                                         final String libName)
   {
      // remove any existing handler
      removeConsolePromptHandler();
      
      consolePromptHandlerReg_ = events_.addHandler(ConsolePromptEvent.TYPE, 
         new ConsolePromptHandler() {
            @Override
            public void onConsolePrompt(ConsolePromptEvent event)
            {  
               // remove handler so it is only called once
               removeConsolePromptHandler();
               
               // check status and set it
               server_.isPackageLoaded(
                         packageName, 
                         libName,
                         new ServerRequestCallback<Boolean>() {
                  @Override
                  public void onResponseReceived(Boolean status)
                  {
                     PackageStatus pkgStatus = PackageStatus.create(packageName, 
                                                                    libName, 
                                                                    status);
                     view_.setPackageStatus(pkgStatus);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     // ignore errors
                  }
               });  
            }
         });
   }

   private void removeConsolePromptHandler()
   {
      if (consolePromptHandlerReg_ != null)
      {
         consolePromptHandlerReg_.removeHandler();
         consolePromptHandlerReg_ = null;
      }
   }
   
   private void withPackageInstallContext(
         final OperationWithInput<PackageInstallContext> operation)
   {
      final ProgressIndicator indicator = 
         globalDisplay_.getProgressIndicator("Error");
      indicator.onProgress("Retrieving package installation context...");

      server_.getPackageInstallContext(
         new SimpleRequestCallback<PackageInstallContext>() {

            @Override
            public void onResponseReceived(PackageInstallContext context)
            {
               indicator.onCompleted();
               operation.execute(context);
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });     
   }
   
   public void onLoadedPackageUpdates(LoadedPackageUpdatesEvent event)
   {
      restartForInstallWithConfirmation(event.getInstallCmd());  
   }
   
   private class InstallCommand
   {
      public InstallCommand(List<String> packages, String cmd)
      {
         this.packages = packages;
         this.cmd = cmd;
      }
      public final List<String> packages;
      public final String cmd;
   }
   
   private void executeWithLoadedPackageCheck(final InstallCommand command)
   {
      // check if we are potentially going to be overwriting an
      // already installed package. if so then prompt for restart
      if ((command.packages != null))
      {
         server_.loadedPackageUpdatesRequired(
               command.packages, 
               new ServerRequestCallback<Boolean>() {

                  @Override
                  public void onResponseReceived(Boolean required)
                  {
                     if (required)
                     {
                        restartForInstallWithConfirmation(command.cmd);
                     }
                     else
                     {
                        executePkgCommand(command.cmd);
                     }
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     executePkgCommand(command.cmd);
                  }

               }); 
      }
      else
      {
         executePkgCommand(command.cmd);
      }
   }

   private void executePkgCommand(String cmd)
   {
      events_.fireEvent(new SendToConsoleEvent(cmd, true));
   }
   
   private void restartForInstallWithConfirmation(final String installCmd)
   {
      String msg = "One or more of the packages that will be updated by this " +
                   "installation are currently loaded. Restarting R prior " +
                   "to updating these packages is strongly recommended.\n\n" +
                   "RStudio can restart R and then automatically continue " +
                   "the installation after restarting (all work and " +
                   "data will be preserved during the restart).\n\n" +
                   "Do you want to restart R prior to installing?";
                  
      final boolean haveInstallCmd = installCmd.startsWith("install.packages");
      
      globalDisplay_.showYesNoMessage(
            MessageDialog.WARNING,
            "Updating Loaded Packages",
            msg,
            true,
            new Operation() { public void execute()
            {
               events_.fireEvent(new SuspendAndRestartEvent(
                      SuspendOptions.createSaveAll(true), installCmd));  
                  
            }},
            new Operation() { public void execute()
            {
               server_.ignoreNextLoadedPackageCheck(
                                            new VoidServerRequestCallback() {
                  @Override
                  public void onSuccess()
                  {
                     if (haveInstallCmd)
                        executePkgCommand(installCmd);
                  }
               });
            }},
            true);   
   }


   private final Display view_;
   private final PackagesServerOperations server_;
   private ArrayList<PackageInfo> allPackages_ = new ArrayList<PackageInfo>();
   private String packageFilter_ = new String();
   private HandlerRegistration consolePromptHandlerReg_ = null;
   private final EventBus events_ ;
   private final GlobalDisplay globalDisplay_ ;
   private final DefaultCRANMirror defaultCRANMirror_;
   private PackageInstallOptions installOptions_ = 
                                  PackageInstallOptions.create(true, "", true);
}
