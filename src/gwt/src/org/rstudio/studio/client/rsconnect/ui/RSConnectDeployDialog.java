/*
 * RSConnectDeployDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentFiles;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;

public class RSConnectDeployDialog 
             extends RSConnectDialog<RSConnectDeploy>
{
   public RSConnectDeployDialog(RSConnectServerOperations server, 
                                RSAccountConnector connector,
                                final GlobalDisplay display, 
                                Session session,
                                EventBus events,
                                final String sourceDir, 
                                String sourceFile,
                                String[] ignoredFiles,
                                final RSConnectAccount lastAccount, 
                                String lastAppName, 
                                boolean isSatellite)
                                
   {
      super(server, display, new RSConnectDeploy(server, connector, display, session,
            StringUtil.getExtension(sourceFile).toLowerCase().equals("rmd")));
      setText("Publish to Server");
      setWidth("350px");
      deployButton_ = new ThemedButton("Publish");
      addCancelButton();
      addOkButton(deployButton_);
      sourceDir_ = sourceDir;
      sourceFile_ = sourceFile;
      events_ = events;
      lastAppName_ = lastAppName;
      isSatellite_ = isSatellite;
      defaultAccount_ = lastAccount;
      connector_ = connector;
      ignoredFiles_ = ignoredFiles;

      String deployTarget = sourceDir;
      if (StringUtil.getExtension(sourceFile).toLowerCase().equals("rmd")) 
      {
         FileSystemItem sourceFSI = FileSystemItem.createDir(sourceDir);
         deployTarget = sourceFSI.completePath(sourceFile);
         FileSystemItem fileFSI = FileSystemItem.createFile(deployTarget);
         contents_.setNewAppName(fileFSI.getStem());
      }
      else
      {
         contents_.setNewAppName(FilePathUtils.friendlyFileName(sourceDir));
      }

      launchCheck_ = new CheckBox("Launch browser");
      launchCheck_.setValue(true);
      launchCheck_.setStyleName(contents_.getStyle().launchCheck());
      addLeftWidget(launchCheck_);
      
      contents_.setSourceDir(sourceDir);
      
      deployButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onDeploy();
         }
      });
      
      contents_.setOnFileAddClick(new Command() 
      {
         @Override
         public void execute()
         {
            onAddFileClick();
         }
      });
      
      // don't enable the deploy button until we're done getting the file list
      deployButton_.setEnabled(false);
      
      indicator_ = addProgressIndicator(false);

      // Get the files to be deployed
      server_.getDeploymentFiles(
            deployTarget,
            new ServerRequestCallback<RSConnectDeploymentFiles>()
            {
               @Override 
               public void onResponseReceived(RSConnectDeploymentFiles files)
               {
                  if (files.getDirSize() > files.getMaxSize())
                  {
                     hide();
                     display_.showErrorMessage("Directory Too Large", 
                           "The directory to be deployed (" + sourceDir + ") " +
                           "exceeds the maximum deployment size, which is " +
                           StringUtil.formatFileSize(files.getMaxSize()) + "." +
                           " Consider creating a new directory containing " + 
                           "only the content you wish to deploy.");
                  }
                  else
                  {
                     contents_.setFileList(files.getDirList(), ignoredFiles_);
                     contents_.setFileCheckEnabled(sourceFile_, false);
                     deployButton_.setEnabled(true);
                  }
               }
               @Override
               public void onError(ServerError error)
               {
                  // we'll just show an empty list in the failure case
               }
            });

      // Get the deployments of this directory from any account (should be fast,
      // since this information is stored locally in the directory). 
      server_.getRSConnectDeployments(deployTarget, 
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(
               JsArray<RSConnectDeploymentRecord> records)
         {
            processDeploymentRecords(records);
            if (records.length() == 1 && defaultAccount_ == null)
            {
               defaultAccount_ = records.get(0).getAccount();
               contents_.setDefaultAccount(defaultAccount_);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            // If an error occurs we won't have any local deployment records,
            // but the user can still create new deployments.
         }
      });
      
      server_.getRSConnectAccountList(new ServerRequestCallback<JsArray<RSConnectAccount>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectAccount> accounts)
         {
            if (accounts.length() == 0)
            {
               // The user has no accounts connected--hide ourselves and 
               // ask the user to connect an account before we continue.
               hide();
               connector_.showAccountWizard(true, new OperationWithInput<Boolean>() 
               {
                  @Override
                  public void execute(Boolean input)
                  {
                     onConnectAccountFinished();
                  }
               });
            }
            else
            {
               contents_.setAccountList(accounts);
               if (defaultAccount_ != null)
                  contents_.setDefaultAccount(defaultAccount_);
               updateApplicationList();
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving accounts", 
                                     error.getMessage());
            closeDialog();
         }
      });
      
      // Update the list of applications when the account is changed
      contents_.addAccountChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateApplicationList();
         }
      });
      
      // Update app info when the application is changed
      contents_.addAppChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateApplicationInfo();
         }
      });
      
      contents_.setOnDeployDisabled(new Command()
      {
         @Override
         public void execute()
         {
            deployButton_.setEnabled(false);
         }
      });

      contents_.setOnDeployEnabled(new Command()
      {
         @Override
         public void execute()
         {
            deployButton_.setEnabled(true);
         }
      });
   }
   
   // Runs when the selected application changes; shows the cached information
   // (URL and status) for the selected application
   private void updateApplicationInfo()
   {
      String appName = contents_.getSelectedApp();
      if (appName == "Create New")
      {
         contents_.showAppInfo(null);
      }
      else if (apps_.containsKey(contents_.getSelectedAccount()))
      {
         JsArray<RSConnectApplicationInfo> apps =
               apps_.get(contents_.getSelectedAccount());
         for (int i = 0; i < apps.length(); i++)
         {
            if (apps.get(i).getName().equals(appName))
            {
               contents_.showAppInfo(apps.get(i));
            }
         }
      }
   }
   
   private void updateApplicationList()
   {
      final RSConnectAccount account = contents_.getSelectedAccount();
      if (account == null)
         return;

      // Check to see if the app list is already in our cache
      if (apps_.containsKey(account))
      {
         setAppList(apps_.get(account));
         return;
      }
      
      // This operation hits the back-end service, so show some progress if 
      // it takes more than a few ms
      final Timer t = new Timer() {
         @Override
         public void run()
         {
            indicator_.onProgress("Contacting Server...");
         }
      };
      t.schedule(500);

      // Not already in our cache, fetch it and populate the cache
      server_.getRSConnectAppList(account.getName(), account.getServer(),
            new ServerRequestCallback<JsArray<RSConnectApplicationInfo>>()
      {
         @Override
         public void onResponseReceived(
               JsArray<RSConnectApplicationInfo> apps)
         {

            t.cancel();
            indicator_.onCompleted();
            apps_.put(account, apps);
            setAppList(apps);
         }

         @Override
         public void onError(ServerError error)
         {
            t.cancel();
            indicator_.onCompleted();
            // we can always create a new app
            contents_.setAppList(null, null);
         }
      });
   }
   
   private void setAppList(JsArray<RSConnectApplicationInfo> apps)
   {
      ArrayList<String> appNames = new ArrayList<String>();
      for (int i = 0; i < apps.length(); i++)
      {
         RSConnectApplicationInfo appInfo = apps.get(i);
         // Filter the app list by URLs deployed from this directory 
         // specifically
         if (deployments_.containsKey(appInfo.getUrl()))
         {
            appNames.add(apps.get(i).getName());
         }
      }
      contents_.setAppList(appNames, lastAppName_);
      updateApplicationInfo();
   }
   
   // Runs when we've finished doing a just-in-time account connection
   private void onConnectAccountFinished()
   {
      server_.getRSConnectAccountList(new ServerRequestCallback<JsArray<RSConnectAccount>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectAccount> accounts)
         {
            if (accounts.length() == 0)
            {
               // The user didn't successfully connect an account--just close 
               // ourselves
               closeDialog();
            }
            else
            {
               // We have an account, show it and re-display ourselves
               contents_.setAccountList(accounts);
               updateApplicationList();
               showModal();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving accounts", 
                                     error.getMessage());
            closeDialog();
         }
      });
   }
   
   private void onDeploy()
   {
      String appName = contents_.getSelectedApp();
      if (appName == null || appName == "Create New")
         appName = contents_.getNewAppName();
      
      RSConnectAccount account = contents_.getSelectedAccount();
      
      // compose the list of files that have been manually added; we want to
      // include all the ones the user added but didn't later uncheck, so
      // cross-reference the list we kept with the one returned by the dialog
      ArrayList<String> deployFiles = contents_.getFileList();
      ArrayList<String> additionalFiles = new ArrayList<String>();
      for (String filePath: filesAddedManually_)
      {
         if (deployFiles.contains(filePath))
         {
            additionalFiles.add(filePath);
         }
      }
      
      if (isSatellite_)
      {
         // in a satellite window, call back to the main window to do a 
         // deployment
         RSConnect.deployFromSatellite(
               sourceDir_, 
               JsArrayUtil.toJsArrayString(contents_.getFileList()),
               JsArrayUtil.toJsArrayString(additionalFiles),
               JsArrayUtil.toJsArrayString(contents_.getIgnoredFileList()),
               sourceFile_, 
               launchCheck_.getValue(), 
               RSConnectDeploymentRecord.create(appName, account, ""));

         // we can't raise the main window if we aren't in desktop mode, so show
         // a dialog to guide the user there
         if (!Desktop.isDesktop())
         {
            display_.showMessage(GlobalDisplay.MSG_INFO, "Deployment Started",
                  "RStudio is deploying " + appName + ". " + 
                  "Check the Deploy console tab in the main window for " + 
                  "status updates. ");
         }
      }
      else
      {
         // in the main window, initiate the deployment directly
         events_.fireEvent(new RSConnectDeployInitiatedEvent(
               sourceDir_,
               contents_.getFileList(),
               additionalFiles,
               contents_.getIgnoredFileList(),
               sourceFile_,
               launchCheck_.getValue(),
               RSConnectDeploymentRecord.create(appName, account, "")));
      }

      closeDialog();
   }
   
   // Create a lookup from app URL to deployments made of this directory
   // to that URL
   private void processDeploymentRecords(
         JsArray<RSConnectDeploymentRecord> records)
   {
      for (int i = 0; i < records.length(); i++)
      {
         RSConnectDeploymentRecord record = records.get(i);
         deployments_.put(record.getUrl(), record);
      }
   }
   
   private void onAddFileClick()
   {
      FileDialogs dialogs = RStudioGinjector.INSTANCE.getFileDialogs();
      final FileSystemItem sourceDir = FileSystemItem.createDir(sourceDir_);
      dialogs.openFile("Select File", 
            RStudioGinjector.INSTANCE.getRemoteFileSystemContext(), 
            sourceDir, 
            new ProgressOperationWithInput<FileSystemItem>()
            {
               @Override
               public void execute(FileSystemItem input, 
                                   ProgressIndicator indicator)
               {
                  if (input != null)
                  {
                     String path = input.getPathRelativeTo(sourceDir);
                     if (path == null)
                     {
                        display_.showMessage(GlobalDisplay.MSG_INFO, 
                              "Cannot Add File", 
                              "Only files in the same folder as the " +
                              "document (" + sourceDir_ + ") or one of its " +
                              "sub-folders may be added.");
                        return;
                     }
                     else
                     {
                        // see if the file is already in the list (we don't 
                        // want to duplicate an existing entry)
                        ArrayList<String> files = contents_.getFileList();
                        for (String file: files)
                        {
                           if (file.equals(path))
                           {
                              indicator.onCompleted();
                              return;
                           }
                        }
                        contents_.addFileToList(path);
                        filesAddedManually_.add(path);
                     }
                  }
                  indicator.onCompleted();
               }
            });
   }
   
   private final EventBus events_;
   private final boolean isSatellite_;
   private final RSAccountConnector connector_;
   
   private String sourceDir_;
   private String sourceFile_;
   private String lastAppName_;
   private ThemedButton deployButton_;
   private ProgressIndicator indicator_;
   private CheckBox launchCheck_;
   private RSConnectAccount defaultAccount_;
   private ArrayList<String> filesAddedManually_ =
         new ArrayList<String>();
   
   private String[] ignoredFiles_;
   
   // Map of account to a list of applications owned by that account
   private Map<RSConnectAccount, JsArray<RSConnectApplicationInfo>> apps_ = 
         new HashMap<RSConnectAccount, JsArray<RSConnectApplicationInfo>>();
   
   // Map of app URL to the deployment made to that URL
   private Map<String, RSConnectDeploymentRecord> deployments_ = 
         new HashMap<String, RSConnectDeploymentRecord>();
}