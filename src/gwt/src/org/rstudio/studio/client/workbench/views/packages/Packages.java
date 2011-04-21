/*
 * Packages.java
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
package org.rstudio.studio.client.workbench.views.packages;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.cran.DefaultCRANMirror;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedHandler;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedHandler;
import org.rstudio.studio.client.workbench.views.packages.model.InstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.CheckForUpdatesDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Packages
      extends BasePresenter
      implements InstalledPackagesChangedHandler,
                 PackageStatusChangedHandler,
                 PackagesDisplayObserver
{
   public interface Binder extends CommandBinder<Commands, Packages> {}

   public interface Display extends WorkbenchView
   {
      void listPackages(List<PackageInfo> packagesDS);
      
      void installPackage(String installRepository,
                          PackagesServerOperations server,
                          GlobalDisplay globalDisplay,
                          OperationWithInput<InstallOptions> operation);
      
      void setPackageStatus(String packageName, boolean loaded);
  
      void setObserver(PackagesDisplayObserver observer) ;
      void setProgress(boolean showProgress);
   }
   
   @Inject
   public Packages(Display view, 
                   EventBus events,
                   PackagesServerOperations server,
                   GlobalDisplay globalDisplay,
                   Session session,
                   DefaultCRANMirror defaultCRANMirror)
   {
      super(view);
      view_ = view;
      server_ = server;
      globalDisplay_ = globalDisplay ;
      view_.setObserver(this) ;
      events_ = events ;
      defaultCRANMirror_ = defaultCRANMirror;

      events.addHandler(InstalledPackagesChangedEvent.TYPE, this);
      events.addHandler(PackageStatusChangedEvent.TYPE, this);
      
      // make the install repository persistent
      new StringStateValue("packages", "installRepository", true,
            session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            if (value != null && value.length() > 0)
               installRepository_ = value;
         }

         @Override
         protected String getValue()
         {
            return installRepository_;
         }
      };

      listPackages();
   }
   
   void onInstallPackage()
   {
      defaultCRANMirror_.ensureConfigured(new Command() {
         public void execute()
         {
            doInstallPackage(); 
         } 
      });
   }
  
   private void doInstallPackage()
   {
      view_.installPackage(installRepository_,
                           server_,
                           globalDisplay_,
                           new OperationWithInput<InstallOptions>() {

            public void execute(InstallOptions options)
            {
               String command = "install.packages(\"" +
                                 options.getPackageName() +
                                 "\"";

               if (options.getRepository() != null)
                  command += ", repos = \"" + options.getRepository() + "\"";

               command += ")";

               events_.fireEvent(new SendToConsoleEvent(command, true));

               // save repository
               installRepository_ = options.getRepository();
            }
           });
   }
   
   
   
   void onUpdatePackages()
   {
      defaultCRANMirror_.ensureConfigured(new Command() {
         public void execute()
         {
            doUpdatePackages(); 
         } 
      });
   }
   
   private void doUpdatePackages()
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
               /*
               StringBuilder command = new StringBuilder();
               command.append("update.packages(oldPkgs = old.packages(instPkgs = c(");
               for (int i=0; i<updates.size(); i++)
               {
                  if (i>0)
                    command.append(", ");
                  command.append("\"");
                  command.append(updates.get(i).getPackageName());
                  command.append("\"");
               }
               command.append(")), ask = FALSE)");
               String cmd = command.toString();
               events_.fireEvent(new SendToConsoleEvent(cmd, true));
               */
            }  
      }).showModal();
   }
   
   public void removePackage(final PackageInfo packageInfo)
   {
      globalDisplay_.showYesNoMessage(
            MessageDialog.WARNING,
            "Confirm Remove",
            "Are you sure you want to remove the " + 
            packageInfo.getName() + " package?",
            new ProgressOperation() 
            {
               @Override
               public void execute(final ProgressIndicator indicator)
               {
                  indicator.onProgress("Removing package...");
                  server_.getDefaultLibrary(new ServerRequestCallback<String>(){

                     @Override
                     public void onResponseReceived(String defaultLibrary)
                     {
                        StringBuilder command = new StringBuilder();
                        command.append("remove.packages(\"");
                        command.append(packageInfo.getName());
                        command.append("\"");
                        if (!packageInfo.getLibrary().equals(defaultLibrary))
                        {
                           command.append(", lib=\"");
                           command.append(packageInfo.getLibrary());
                           command.append("\"");
                        }
                        command.append(")");
                        String cmd = command.toString();
                        events_.fireEvent(new SendToConsoleEvent(cmd, true));
                        indicator.onCompleted();
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        indicator.onError(error.getUserMessage());
                     }
                     
                  });      
               }      
            },
            true);
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

   
   
   public void loadPackage(final String packageName)
   {  
      // check status to make sure the package was loaded
      checkPackageStatusOnNextConsolePrompt(packageName);
      
      // send the command
      StringBuilder command = new StringBuilder();
      command.append("library(\"");
      command.append(packageName);
      command.append("\")");
      events_.fireEvent(new SendToConsoleEvent(command.toString(), true));
   }

   public void unloadPackage(final String packageName)
   { 
      // check status to make sure the package was unloaded
      checkPackageStatusOnNextConsolePrompt(packageName);
      
      StringBuilder command = new StringBuilder();
      command.append("detach(\"package:");
      command.append(packageName);
      command.append("\")");
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
   
   public void onPackageFilterChanged(String filter)
   {
      packageFilter_ = filter.toLowerCase();
      setViewPackageList();
   }

   public void onPackageStatusChanged(PackageStatusChangedEvent event)
   {
      PackageStatus status = event.getPackageStatus();
      view_.setPackageStatus(status.getName(), status.isLoaded());
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
   
   private void checkPackageStatusOnNextConsolePrompt(final String packageName)
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
               server_.isPackageLoaded(packageName, 
                                       new ServerRequestCallback<Boolean>() {
                  @Override
                  public void onResponseReceived(Boolean loaded)
                  {
                     view_.setPackageStatus(packageName, loaded);
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
   
   private final Display view_;
   private final PackagesServerOperations server_;
   private ArrayList<PackageInfo> allPackages_ = new ArrayList<PackageInfo>();
   private String packageFilter_ = new String();
   private HandlerRegistration consolePromptHandlerReg_ = null;
   private final EventBus events_ ;
   private final GlobalDisplay globalDisplay_ ;
   private final DefaultCRANMirror defaultCRANMirror_;
   private String installRepository_;
}
