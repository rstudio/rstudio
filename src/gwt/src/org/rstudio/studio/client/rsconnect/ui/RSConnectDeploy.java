/*
 * RSConnectDeploy.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectAppName;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentFiles;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RSConnectDeploy extends Composite
                             implements AppNameTextbox.Host
{
   private static RSConnectDeployUiBinder uiBinder = GWT
         .create(RSConnectDeployUiBinder.class);

   interface RSConnectDeployUiBinder extends UiBinder<Widget, RSConnectDeploy>
   {
   }
   
   public interface DeployStyle extends CssResource
   {
      String accountAnchor();
      String accountList();
      String controlLabel();
      String deployLabel();
      String descriptionPanel();
      String fileList();
      String firstControlLabel();
      String gridControl();
      String launchCheck();
      String normalStatus();
      String otherStatus();
      String rootCell();
      String source();
      String sourceDestLabels();
      String statusLabel();
      String transferArrow();
      String urlAnchor();
      String wizard();
      String progressPanel();
      String appDetailsPanel();
      String wizardDeployPage();
   }
   
   public interface DeployResources extends ClientBundle
   {
      @Source("publishShinyIllustration_2x.png")
      ImageResource publishShinyIllustration2x();

      @Source("publishRmdIllustration_2x.png")
      ImageResource publishRmdIllustration2x();

      @Source("publishPlotIllustration_2x.png")
      ImageResource publishPlotIllustration2x();

      @Source("publishPresentationIllustration_2x.png")
      ImageResource publishPresentationIllustration2x();

      @Source("publishHTMLIllustration_2x.png")
      ImageResource publishHTMLIllustration2x();

      @Source("RSConnectDeploy.css")
      DeployStyle style();
   }
   
   public static DeployResources RESOURCES = GWT.create(DeployResources.class);
   
   public RSConnectDeploy(RSConnectPublishSource source,
                          final int contentType,
                          RSConnectDeploymentRecord fromPrevious,
                          boolean asWizard)
   {
      if (source != null)
      {
         forDocument_ = source.isDocument();
      }
      else
      {
         forDocument_ = asWizard;
      }
      
      contentType_ = contentType;
      fromPrevious_ = fromPrevious;
      
      // import static/code and single/multiple settings from previous
      // deployment, if we have one
      if (fromPrevious != null)
      {
         asMultipleRmd_ = fromPrevious.getAsMultiple();
         asStatic_ = fromPrevious.getAsStatic();
      }
      
      // inject dependencies 
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // create UI
      initWidget(uiBinder.createAndBindUi(this));
      style_ = RESOURCES.style();
      
      if (asWizard)
      {
         deployIllustration_.setVisible(false);
         rootPanel_.addStyleName(style_.wizard());
         hideCheckUncheckAllButton();
      }

      final boolean rsConnectEnabled = RStudioGinjector.INSTANCE.getUIPrefs()
            .enableRStudioConnect().getGlobalValue();
      
      // Invoke the "add account" wizard
      if (contentType == RSConnect.CONTENT_TYPE_APP || rsConnectEnabled)
      {
         addAccountAnchor_.addClickHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               connector_.showAccountWizard(false, 
                     contentType == RSConnect.CONTENT_TYPE_APP ||
                     contentType == RSConnect.CONTENT_TYPE_APP_SINGLE, 
                     new OperationWithInput<Boolean>() 
               {
                  @Override
                  public void execute(Boolean successful)
                  {
                     if (successful)
                     {
                        accountList_.refreshAccountList();
                     }
                  }
               });
               
               event.preventDefault();
               event.stopPropagation();
            }
         });
      }
      else
      {
         // if not deploying a Shiny app and RSConnect UI is not enabled, then
         // there's no account we can add suitable for this content
         addAccountAnchor_.setVisible(false);
      }
      
      createNewAnchor_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            forgetPreviousDeployment();
            event.preventDefault();
            event.stopPropagation();
         }
      });

      checkUncheckAllButton_.getElement().getStyle().setMarginLeft(0, Unit.PX);
      checkUncheckAllButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            checkUncheckAll();
         }
      });

      addFileButton_.setVisible(forDocument_);
      addFileButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            onAddFileClick();
         }
      });
      
      previewButton_.getElement().getStyle().setMarginLeft(0, Unit.PX);
      previewButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            if (display_ != null && !StringUtil.isNullOrEmpty(
                  source_.getDeployFile()))
            {
               display_.showHtmlFile(source_.getDeployFile());
            }
         }
      });
      
      // If we're loading a previous deployment, hide new app name fields
      if (fromPrevious_ != null)
      {
         newAppPanel_.setVisible(false);
      }
      
      // If we already know the source, apply it
      if (source_ != null)
      {
         applySource();
      }
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server, 
                          RSAccountConnector connector,    
                          GlobalDisplay display,
                          UIPrefs prefs)
   {
      server_ = server;
      connector_ = connector;
      display_ = display;
      prefs_ = prefs;
      accountList_ = new RSConnectAccountList(server_, display_, false, 
            !asStatic_);
      appName_ = new AppNameTextbox(this);
      
      // when the account list finishes populating, select the account from the
      // previous deployment if we have one
      accountList_.setOnRefreshCompleted(new Operation() {
         @Override
         public void execute()
         {
            if (fromPrevious_ != null)
            {
               // when re-deploying, select the account used the last time 
               // around
               accountList_.selectAccount(fromPrevious_.getAccount());
            }
            else
            {
               // when doing a first-time publish, select the account the user
               // prefers (currently this just tracks the last account used)
               RSConnectAccount preferred = 
                     prefs_.preferredPublishAccount().getGlobalValue();
               if (preferred != null)
               {
                  accountList_.selectAccount(preferred);
               }
               
               // validate the app name now that we have an account selected
               appName_.validateAppName();
            }
         }
      });
      
      // when the user selects a different account, show the appropriate UI
      addAccountChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent arg0)
         {
            if (fromPrevious_ != null)
            {
               boolean existing = accountList_.getSelectedAccount().equals(
                     fromPrevious_.getAccount());
               appInfoPanel_.setVisible(existing);
               newAppPanel_.setVisible(!existing);

               // validate name if necessary
               if (existing && onDeployEnabled_ != null)
                  onDeployEnabled_.execute();
               else if (!existing)
                  appName_.validateAppName();
            }
            else
            {
               // re-validate app name as different accounts have different 
               // name restrictions
               appName_.validateAppName();
            }
         }
      });
   }
    
   public void setSourceDir(String dir)
   {
      dir = StringUtil.shortPathName(FileSystemItem.createDir(dir), 250);
      deployLabel_.setText(dir);
   }
   
   public void setDefaultAccount(RSConnectAccount account)
   {
      accountList_.selectAccount(account);
   }
   
   public int setAccountList(JsArray<RSConnectAccount> accounts)
   {
      return accountList_.setAccountList(accounts);
   }
   
   public void addFileToList(String path)
   {
      addFile(path, true);
   }
   
   public ArrayList<String> getFileList()
   {
      return getCheckedFileList(true);
   }
   
   public ArrayList<String> getIgnoredFileList()
   {
      return getCheckedFileList(false);
   }
   
   public void showAppInfo(RSConnectApplicationInfo info)
   {
      if (info != null)
      {
         urlAnchor_.setText(info.getUrl());
         urlAnchor_.setHref(info.getUrl());
      }
      appDetailsPanel_.setVisible(true);

      if (isUpdate())
      {
         appInfoPanel_.setVisible(true);
         newAppPanel_.setVisible(false);
         if (onDeployEnabled_ != null)
            onDeployEnabled_.execute();
      }
   }
   
   public HandlerRegistration addAccountChangeHandler(ChangeHandler handler)
   {
      return accountList_.addChangeHandler(handler);
   }

   public void setOnDeployEnabled(Command cmd)
   {
      appName_.setOnNameIsValid(cmd);
      onDeployEnabled_ = cmd;
   }
   
   public void setOnDeployDisabled(Command cmd)
   {
      appName_.setOnNameIsInvalid(cmd);
   }
   
   public DeployStyle getStyle()
   {
      return style_;
   }
   
   public void onActivate(ProgressIndicator indicator)
   {
      populateAccountList(indicator, false);
      populateDeploymentFiles(indicator);
   }
   
   public void setPublishSource(RSConnectPublishSource source, 
         int contentType, boolean asMultipleRmd, boolean asStatic)
   {
      source_ = source;
      contentType_ = contentType;
      asMultipleRmd_ = asMultipleRmd;
      
      // we want to show cloud accounts only for non-static content
      if (source.isShiny() != accountList_.getShowCloudAccounts())
      {
         accountList_.setShowCloudAccounts(source.isShiny());
         accountList_.refreshAccountList();
      }
      
      asStatic_ = asStatic;

      applySource();
   }
   
   public void focus()
   {
      appName_.setFocus(true);
   }
   
   public RSConnectPublishResult getResult() 
   {
      // compose the list of files that have been manually added; we want to
      // include all the ones the user added but didn't later uncheck, so
      // cross-reference the list we kept with the one returned by the dialog
      ArrayList<String> deployFiles = getFileList();
      ArrayList<String> additionalFiles = new ArrayList<String>();
      for (String filePath: filesAddedManually_)
      {
         if (deployFiles.contains(filePath))
         {
            additionalFiles.add(filePath);
         }
      }
      
      String appTitle = appName_.getTitle();
      
      // if we're redeploying to the same account, use the previous app name;
      // otherwise, read the new name the user's entered
      String appName = isUpdate() ?
            fromPrevious_.getName() : appName_.getName();
            
      // if this was new content, set this account as the default to use for 
      // new content
      if (fromPrevious_ == null && 
          !getSelectedAccount().equals(
                prefs_.preferredPublishAccount().getGlobalValue()))
      {
         prefs_.preferredPublishAccount().setGlobalValue(getSelectedAccount());
         prefs_.writeUIPrefs();
      }
      
      return new RSConnectPublishResult(
            appName, 
            isUpdate() ? null : appTitle,
            getSelectedAccount(), 
            source_,
            new RSConnectPublishSettings(deployFiles, 
               additionalFiles, 
               getIgnoredFileList(),
               asMultipleRmd_,
               asStatic_),
            isUpdate());
   }
   
   public void validateResult(final OperationWithInput<Boolean> onComplete)
   {
      // if the name isn't valid to begin with, we know the result immediately
      if (!appName_.isValid())
      {
         appName_.validateAppName();
         onComplete.execute(false);
         focus();
         return;
      }
      
      if (isUpdate())
      {
         // no need to validate names for updates
         onComplete.execute(true);
      }
      else
      {
         // see if there's an app with this name before running the deploy
         checkForExistingApp(getSelectedAccount(), appName_.getName(), 
               new OperationWithInput<Boolean>()
               {
                  @Override
                  public void execute(Boolean valid)
                  {
                     onComplete.execute(valid);
                     if (!valid)
                        focus();
                  }
               });
      }
   }
   
   public void setUnsanitizedAppName(String name)
   {
      appName_.setTitle(name);
      appName_.validateAppName();
   }

   @Override
   public boolean supportsTitle()
   {
      if (getSelectedAccount() == null)
         return true;
      else
         return !getSelectedAccount().isCloudAccount();
   }

   @Override
   public void generateAppName(String title,
         final CommandWithArg<RSConnectAppName> result)
   {
      // don't generate a name if no source or account is present (we'll
      // generate one once the source has been acquired)
      if (source_ == null || getSelectedAccount() == null)
         return;
      
      server_.generateAppName(title, 
            source_.getDeployKey(), 
            getSelectedAccount().getName(), 
            new ServerRequestCallback<RSConnectAppName>()
            {
               @Override
               public void onResponseReceived(RSConnectAppName name)
               {
                  result.execute(name);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  result.execute(null);
               }
            });
   }

   // Private methods --------------------------------------------------------
   
   private void setFileList(ArrayList<String> files,
         ArrayList<String> additionalFiles, ArrayList<String> ignoredFiles)
   {
      fileChecks_ = new ArrayList<DirEntryCheckBox>();
      
      // clear existing file list
      fileListPanel_.clear(); 
      for (int i = 0; i < files.size(); i++)
      {
         boolean checked = true;
         boolean add = true;
         
         // if this file is marked ignored, uncheck it
         if (ignoredFiles != null)
         {
            for (int j = 0; j < ignoredFiles.size(); j++)
            {
               if (ignoredFiles.get(j).equals(files.get(i)))
               {
                  checked = false; 
                  break;
               }
            }
         }

         // if this file is marked additional, don't add it twice (we're about
         // to add the additional files separately below)
         if (additionalFiles != null)
         {
            for (int j = 0; j < additionalFiles.size(); j++)
            {
               if (additionalFiles.get(j).equals(files.get(i)))
               {
                  add = false; 
                  break;
               }
            }
         }

         if (add)
         {
            addFile(files.get(i), checked);
         }
      }

      // add any additional files 
      if (additionalFiles != null)
      {
         for (int i = 0; i < additionalFiles.size(); i++)
         {
            addFile(additionalFiles.get(i), true);
         }
      }
      
      // hide check/uncheck all button if there are only a few files
      if (fileChecks_.size() < 3)
      {
         hideCheckUncheckAllButton();
      }
   }
   
   private RSConnectAccount getSelectedAccount()
   {
      return accountList_.getSelectedAccount();
   }
   
   private void hideCheckUncheckAllButton()
   {
      checkUncheckAllButton_.setVisible(false);
      addFileButton_.getElement().getStyle().setMarginLeft(0, Unit.PX);
   }
   
   private void setPreviousInfo()
   {
      // when the dialog is servicing a redeploy, find information on the
      // content as currently deployed
      if (fromPrevious_ != null)
      {
         appProgressName_.setText(fromPrevious_.getName());
         appExistingName_.setText(fromPrevious_.getDisplayName());
         appProgressPanel_.setVisible(true);
         appInfoPanel_.setVisible(true);

         final ServerRequestCallback<JsArray<RSConnectApplicationInfo>> 
               onAppsReceived = 
               new ServerRequestCallback<JsArray<RSConnectApplicationInfo>>()
         {
            @Override
            public void onResponseReceived(
                  JsArray<RSConnectApplicationInfo> infos)
            {
               // hide server progress
               appProgressPanel_.setVisible(false);

               // find an app with the same account, server, and name;
               // when found, populate the UI with app details
               boolean found = false;
               if (infos != null)
               {
                  for (int i = 0; i < infos.length(); i++)
                  {
                     RSConnectApplicationInfo info = infos.get(i);
                     if (info.getName() == fromPrevious_.getName())
                     {
                        showAppInfo(info);
                        found = true;
                        break;
                     }
                  }
               }

               if (!found)
               {
                  forgetPreviousDeployment();
               }
            }
            @Override
            public void onError(ServerError error)
            {
               // it's okay if we fail here, since the application info
               // display is purely informative
               appProgressPanel_.setVisible(false);
               showAppInfo(null);
            }
         };

         
         if (!StringUtil.isNullOrEmpty(fromPrevious_.getAppId()))
         {
            // we know this app's ID, so get its details directly
            server_.getRSConnectApp(
                  fromPrevious_.getAppId(),
                  fromPrevious_.getAccountName(), 
                  fromPrevious_.getServer(), 
                  new ServerRequestCallback<RSConnectApplicationInfo>()
                  {
                     @Override
                     public void onResponseReceived(
                           RSConnectApplicationInfo info)
                     {
                        // create a single-entry array with the app ID 
                        JsArray<RSConnectApplicationInfo> infos = 
                              JsArray.createArray().cast();
                        if (info != null)
                           infos.push(info);
                        onAppsReceived.onResponseReceived(infos);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        onAppsReceived.onError(error);
                     }
                  });
         }
         else
         {
            // we don't know the app ID, get the apps by name
            server_.getRSConnectAppList(
                  fromPrevious_.getAccountName(), 
                  fromPrevious_.getServer(), 
                  onAppsReceived);
            }
      }
   }
   
   private void populateAccountList(final ProgressIndicator indicator,
                                    final boolean isRetry)
   {
       server_.getRSConnectAccountList(
            new ServerRequestCallback<JsArray<RSConnectAccount>>()
       {
         @Override
         public void onResponseReceived(JsArray<RSConnectAccount> accounts)
         {
            // populate the accounts in the UI (the account display widget 
            // filters based on account criteria)
            int numAccounts = setAccountList(accounts);

            // if this is our first try, ask the user to connect an account
            // since none are currently connected
            if (numAccounts == 0 && !isRetry)
            {
               connector_.showAccountWizard(accounts.length() == 0, 
                     source_.isShiny(),
                     new OperationWithInput<Boolean>() 
               {
                  @Override
                  public void execute(Boolean input)
                  {
                     populateAccountList(indicator, true);
                  }
               });
            }
            else
            {
               setPreviousInfo();
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            indicator.onError("Error retrieving accounts:\n\n" +
                              error.getMessage());
            indicator.onCompleted();
         }
      });
   }
   
   private void populateDeploymentFiles(final ProgressIndicator indicator)
   {
      if (source_ == null)
         return;
      
      // if this is a self-contained document, we don't need to scrape it for
      // dependencies; just inject it directly into the list.
      if (source_.isSelfContained() && source_.isStatic() && 
          !source_.isWebsiteRmd())
      {
         ArrayList<String> files = new ArrayList<String>();
         FileSystemItem selfContained = FileSystemItem.createFile(
                     source_.getDeployFile());
         files.add(selfContained.getName());
         setFileList(files, null, null);
         setPrimaryFile(selfContained.getName());
         return;
      }

      // ternery operator maps to appropriate files to list for deployment:
      // website code    - website code directory 
      // static website  - website build directory
      // document        - R Markdown document
      // non-document    - Shiny app directory
      final String fileSource = source_.isDocument() ? 
            source_.isWebsiteRmd() ? 
                  source_.isStatic() ? 
                     source_.getDeployDir() :
                     source_.getWebsiteDir() :
                  source_.getDeployFile() : 
            source_.getDeployDir();

      indicator.onProgress("Collecting files...");
      server_.getDeploymentFiles(
            fileSource,
            asMultipleRmd_,
            new ServerRequestCallback<RSConnectDeploymentFiles>()
            {
               @Override 
               public void onResponseReceived(RSConnectDeploymentFiles files)
               {
                  if (files.getDirSize() > files.getMaxSize())
                  {
                     indicator.onError(
                           "The item to be deployed (" + fileSource + ") " +
                           "exceeds the maximum deployment size, which is " +
                           StringUtil.formatFileSize(files.getMaxSize()) + "." +
                           " Consider creating a new directory containing " + 
                           "only the content you wish to deploy.");

                  }
                  else
                  {
                     if (files.getDirList() == null || 
                         files.getDirList().length() == 0)
                     {
                        indicator.onError("Could not determine the list of " +
                          "files to deploy.");
                        indicator.onCompleted();
                     }
                     setFileList(
                           JsArrayUtil.fromJsArrayString(files.getDirList()), 
                           fromPrevious_ != null ?
                                 fromPrevious_.getAdditionalFiles() : null, 
                           fromPrevious_ != null ? 
                                 fromPrevious_.getIgnoredFiles() : null);
                     if (!source_.isWebsiteRmd())
                        setPrimaryFile(
                              FileSystemItem.createFile(
                                    source_.getDeployFile()).getName());
                  }
                  
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        indicator.clearProgress();
                     }
                  });
               }
               @Override
               public void onError(ServerError error)
               {
                  // we need to have a list of files to deploy to proceed
                  indicator.onError("Could not find files to deploy: \n\n" +
                     error.getMessage());
                  indicator.onCompleted();
               }
            });
      
   }

   private void addFile(String path, boolean checked)
   {
      DirEntryCheckBox fileCheck = new DirEntryCheckBox(path);
      fileCheck.setValue(checked);
      fileListPanel_.add(fileCheck);
      fileChecks_.add(fileCheck);
   }
   
   private ArrayList<String> getCheckedFileList(boolean checked)
   {
      ArrayList<String> files = new ArrayList<String>();
      if (fileChecks_ == null)
         return files;
      for (int i = 0; i < fileChecks_.size(); i++)
      {
         if (fileChecks_.get(i).getValue() == checked)
         {
            files.add(fileChecks_.get(i).getPath());
         }
      }
      return files;
   }
   
   private void onAddFileClick()
   {
      FileDialogs dialogs = RStudioGinjector.INSTANCE.getFileDialogs();
      final FileSystemItem sourceDir = 
            FileSystemItem.createDir(source_.getDeployDir());
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
                              "document (" + sourceDir + ") or one of its " +
                              "sub-folders may be added.");
                        return;
                     }
                     else
                     {
                        // see if the file is already in the list (we don't 
                        // want to duplicate an existing entry)
                        ArrayList<String> files = getFileList();
                        for (String file: files)
                        {
                           if (file.equals(path))
                           {
                              indicator.onCompleted();
                              return;
                           }
                        }
                        addFileToList(path);
                        filesAddedManually_.add(path);
                     }
                  }
                  indicator.onCompleted();
               }
            });
   }

   private void setPrimaryFile(String path)
   {
      if (fileChecks_ == null)
         return;

      for (int i = 0; i < fileChecks_.size(); i++)
      {
         DirEntryCheckBox fileCheck = fileChecks_.get(i);
         if (fileCheck.getPath().equals(path))
         {
            // don't allow the user to unselect the primary file
            fileCheck.setEnabled(false);
            
            // make this bold and move it to the top
            fileCheck.getElement().getStyle().setFontWeight(FontWeight.BOLD);
            fileListPanel_.remove(fileCheck);
            fileListPanel_.insert(fileCheck, 0);
         }
      }
   }
   
   private void applySource()
   {
      // If this is a self-contained file, don't show the file list; instead, 
      // show the description of what we're about to publish
      if (source_.isSelfContained() && source_.isStatic() && 
          !source_.isWebsiteRmd()) 
      {
         filePanel_.setVisible(false);
         descriptionPanel_.setVisible(true);
         if (contentType_ == RSConnect.CONTENT_TYPE_PLOT ||
             contentType_ == RSConnect.CONTENT_TYPE_HTML)
         {
            descriptionImage_.setResource(
                  new ImageResource2x(RSConnectResources.INSTANCE.previewPlot2x()));
         }
         else if (contentType_ == RSConnect.CONTENT_TYPE_PRES)
         {
            descriptionImage_.setResource(
                  new ImageResource2x(RSConnectResources.INSTANCE.previewPresentation2x()));
         }
         else
         {
            descriptionImage_.setResource(
                  new ImageResource2x(RSConnectResources.INSTANCE.previewDoc2x()));
         }
      }
      
      // if the app name textbox isn't populated, derive from the filename
      // (for apps and documents--other content types use temporary filenames)
      if (appName_.getTitle().isEmpty())
      {
         if (contentType_ == RSConnect.CONTENT_TYPE_APP || 
             contentType_ == RSConnect.CONTENT_TYPE_APP_SINGLE || 
             contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT)
         {
            // set the app name to the filename
            String appTitle = 
                  FilePathUtils.fileNameSansExtension(source_.getSourceFile());

            // if this is a document with the name "index", guess the directory
            // as the content name rather than the file
            if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT &&
                appTitle.toLowerCase().equals("index"))
            {
               appTitle = FilePathUtils.fileNameSansExtension(
                     source_.getDeployDir());
            }

            setUnsanitizedAppName(appTitle);
         }
         else if (contentType_ == RSConnect.CONTENT_TYPE_WEBSITE)
         {
            setUnsanitizedAppName(FilePathUtils.fileNameSansExtension(
                  source_.getWebsiteDir()));
         }
      }
      
      ImageResource illustration = null;
      if (contentType_ == RSConnect.CONTENT_TYPE_APP)
         illustration = new ImageResource2x(RESOURCES.publishShinyIllustration2x());
      else if (contentType_ == RSConnect.CONTENT_TYPE_PLOT)
         illustration = new ImageResource2x(RESOURCES.publishPlotIllustration2x());
      else if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT)
         illustration = new ImageResource2x(RESOURCES.publishRmdIllustration2x());
      else if (contentType_ == RSConnect.CONTENT_TYPE_HTML)
         illustration = new ImageResource2x(RESOURCES.publishHTMLIllustration2x());
      else if (contentType_ == RSConnect.CONTENT_TYPE_PRES)
         illustration = new ImageResource2x(RESOURCES.publishPresentationIllustration2x());
      if (illustration != null)
         deployIllustration_.setResource(illustration);
   }
   
   private boolean isUpdate()
   {
      return fromPrevious_ != null && 
            getSelectedAccount() != null &&
            getSelectedAccount().equals(fromPrevious_.getAccount());
   }
   
   private void checkForExistingApp(final RSConnectAccount account, 
         final String appName,
         final OperationWithInput<Boolean> onValidated)
   {
      server_.getRSConnectAppList(account.getName(), account.getServer(), 
            new ServerRequestCallback<JsArray<RSConnectApplicationInfo>>()
            {
               @Override
               public void onResponseReceived(
                     JsArray<RSConnectApplicationInfo> apps)
               {
                  String url = null;
                  for (int i = 0; i < apps.length(); i++)
                  {
                     if (apps.get(i).getName().equalsIgnoreCase(appName)) 
                     {
                        url = apps.get(i).getUrl();
                        break;
                     }
                  }
                  
                  if (url == null)
                  {
                     // no name conflicts
                     onValidated.execute(true);
                  }
                  else
                  {
                     display_.showYesNoMessage(
                           GlobalDisplay.MSG_QUESTION, 
                           "Overwrite " + appName + "?", 
                           "You've already published an application named '" + 
                           appName +"' to " + account.getServer() + " (" + 
                           url + "). Do you want to replace the existing " + 
                           "application with this content?", false, 
                           new ProgressOperation()
                           {
                              @Override
                              public void execute(ProgressIndicator indicator)
                              {
                                 indicator.onCompleted();
                                 onValidated.execute(true);
                              }
                           }, 
                           new ProgressOperation()
                           {
                              @Override
                              public void execute(ProgressIndicator indicator)
                              {
                                 indicator.onCompleted();
                                 onValidated.execute(false);
                              }
                           }, 
                           "Replace", 
                           "Cancel", 
                           true);
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  // just treat it as valid--the alternative is to show an error
                  // message that says "hey, we couldn't figure out what apps
                  // are already on the server, so we have no idea whether or
                  // not this name is taken--publish anyway?", which does not
                  // inspire confidence
                  onValidated.execute(true);
               }
            });
   }
   
   private void forgetPreviousDeployment()
   {
      // set the UI state to creating a new app with the same name as the one
      // that was removed
      appName_.setVisible(true);
      if (fromPrevious_ != null)
         appName_.setDetails(fromPrevious_.getTitle(), fromPrevious_.getName());
      appInfoPanel_.setVisible(false);
      newAppPanel_.setVisible(true);

      // pretend we're creating a brand-new app
      fromPrevious_ = null;
   }
   
   private void checkUncheckAll()
   {
      allChecked_ = !allChecked_;
      for (DirEntryCheckBox box: fileChecks_)
      {
         // don't toggle state for disabled boxes, or common Shiny .R filenames
         String file = box.getPath().toLowerCase();
         if (box.isEnabled() &&
             file != "ui.r" &&
             file != "server.r" &&
             file != "app.r")
         {
            box.setValue(allChecked_);
         }
      }
      checkUncheckAllButton_.setText(allChecked_ ? "Uncheck All" : "Check All");
   }
   
   
   @UiField Anchor addAccountAnchor_;
   @UiField Anchor createNewAnchor_;
   @UiField Anchor urlAnchor_;
   @UiField Grid mainGrid_;
   @UiField HTMLPanel appDetailsPanel_;
   @UiField HTMLPanel appInfoPanel_;
   @UiField HTMLPanel appProgressPanel_;
   @UiField HTMLPanel newAppPanel_;
   @UiField HTMLPanel rootPanel_;
   @UiField Image deployIllustration_;
   @UiField Image descriptionImage_;
   @UiField InlineLabel deployLabel_;
   @UiField Label appExistingName_;
   @UiField Label appProgressName_;
   @UiField Label nameLabel_;
   @UiField ThemedButton addFileButton_;
   @UiField ThemedButton checkUncheckAllButton_;
   @UiField ThemedButton previewButton_;
   @UiField VerticalPanel fileListPanel_;
   @UiField VerticalPanel filePanel_;
   @UiField VerticalPanel descriptionPanel_;
   
   // provided fields
   @UiField(provided=true) RSConnectAccountList accountList_;
   @UiField(provided=true) AppNameTextbox appName_;
   
   private ArrayList<DirEntryCheckBox> fileChecks_;
   private ArrayList<String> filesAddedManually_ = 
         new ArrayList<String>();
   
   private RSConnectServerOperations server_;
   private GlobalDisplay display_;
   private RSAccountConnector connector_;
   private UIPrefs prefs_;
   
   private RSConnectPublishSource source_;
   private boolean asMultipleRmd_;
   private boolean asStatic_;
   private int contentType_;
   private Command onDeployEnabled_;
   private RSConnectDeploymentRecord fromPrevious_;
   private boolean allChecked_ = true;

   private final DeployStyle style_;
   private final boolean forDocument_;
}
