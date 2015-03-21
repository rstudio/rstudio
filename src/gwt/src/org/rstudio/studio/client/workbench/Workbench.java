/*
 * Workbench.java
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
package org.rstudio.studio.client.workbench;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.vcs.AskPassManager;
import org.rstudio.studio.client.common.vcs.ShowPublicKeyDialog;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.htmlpreview.HTMLPreview;
import org.rstudio.studio.client.pdfviewer.PDFViewer;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.ShinyApplication;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.choosefile.ChooseFile;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerPresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

public class Workbench implements BusyHandler,
                                  ShowErrorMessageHandler,
                                  UserPromptHandler,
                                  ShowWarningBarHandler,
                                  BrowseUrlHandler,
                                  QuotaStatusHandler,
                                  WorkbenchLoadedHandler,
                                  WorkbenchMetricsChangedHandler,
                                  InstallRtoolsEvent.Handler
{
   interface Binder extends CommandBinder<Commands, Workbench> {}
   
   @Inject
   public Workbench(WorkbenchMainView view, 
                    WorkbenchContext workbenchContext,
                    GlobalDisplay globalDisplay,
                    Commands commands,
                    EventBus eventBus,
                    Session session,
                    Provider<UIPrefs> pPrefs,
                    Server server,
                    RemoteFileSystemContext fsContext,
                    FileDialogs fileDialogs,
                    FileTypeRegistry fileTypeRegistry,
                    ConsoleDispatcher consoleDispatcher,
                    Provider<GitState> pGitState,
                    ChooseFile chooseFile,   // required to force gin to create
                    AskPassManager askPass,  // required to force gin to create
                    PDFViewer pdfViewer,     // required to force gin to create
                    HTMLPreview htmlPreview, // required to force gin to create
                    ProfilerPresenter prof,  // required to force gin to create
                    ShinyApplication sApp,   // required to force gin to create
                    DependencyManager dm,    // required to force gin to create
                    RmdOutput rmdOutput)     // required to force gin to create    
  {
      view_ = view;
      workbenchContext_ = workbenchContext;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      eventBus_ = eventBus;
      session_ = session;
      pPrefs_ = pPrefs;
      server_ = server;
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      consoleDispatcher_ = consoleDispatcher;
      pGitState_ = pGitState;
      
      ((Binder)GWT.create(Binder.class)).bind(commands, this);
      
      // edit
      eventBus.addHandler(BusyEvent.TYPE, this);
      eventBus.addHandler(ShowErrorMessageEvent.TYPE, this);
      eventBus.addHandler(UserPromptEvent.TYPE, this);
      eventBus.addHandler(ShowWarningBarEvent.TYPE, this);
      eventBus.addHandler(BrowseUrlEvent.TYPE, this);
      eventBus.addHandler(QuotaStatusEvent.TYPE, this);
      eventBus.addHandler(WorkbenchLoadedEvent.TYPE, this);
      eventBus.addHandler(WorkbenchMetricsChangedEvent.TYPE, this);
      eventBus.addHandler(InstallRtoolsEvent.TYPE, this);

      // We don't want to send setWorkbenchMetrics more than once per 1/2-second
      metricsChangedCommand_ = new TimeBufferedCommand(-1, -1, 500)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            assert !shouldSchedulePassive;
            
            server_.setWorkbenchMetrics(lastWorkbenchMetrics_,
                                        new VoidServerRequestCallback());
         }
      };
   }

   public WorkbenchMainView getMainView()
   {
      return view_ ;
   }

   public void onWorkbenchLoaded(WorkbenchLoadedEvent event)
   {
      server_.initializeForMainWorkbench();

      FileSystemItem defaultDialogDir =
            session_.getSessionInfo().getActiveProjectDir();
      if (defaultDialogDir != null)
         workbenchContext_.setDefaultFileDialogDir(defaultDialogDir);
      
      // check for init messages
      checkForInitMessages();
      
      if (Desktop.isDesktop() && 
          session_.getSessionInfo().getVcsName().equals(VCSConstants.GIT_ID))
      {
         pGitState_.get().addVcsRefreshHandler(new VcsRefreshHandler() {
   
            @Override
            public void onVcsRefresh(VcsRefreshEvent event)
            {
               FileSystemItem projDir = workbenchContext_.getActiveProjectDir();
               if (projDir != null)
               {
                  String title = projDir.getPath();
                  String branch = pGitState_.get().getBranchInfo()
                                                        .getActiveBranch();
                  if (branch != null)
                     title = title + " - " + branch;
                  Desktop.getFrame().setWindowTitle(title);
               }
            }
         });
      }
      
   }
   
   public void onBusy(BusyEvent event)
   {  
   }

   public void onShowErrorMessage(ShowErrorMessageEvent event)
   {
      ErrorMessage errorMessage = event.getErrorMessage();
      globalDisplay_.showErrorMessage(errorMessage.getTitle(), 
                                      errorMessage.getMessage());
     
   }
   
   @Override
   public void onShowWarningBar(ShowWarningBarEvent event)
   {
      WarningBarMessage message = event.getMessage();
      globalDisplay_.showWarningBar(message.isSevere(), message.getMessage());
   } 
   
   public void onBrowseUrl(BrowseUrlEvent event)
   {
      BrowseUrlInfo urlInfo = event.getUrlInfo();
      NewWindowOptions newWindowOptions = new NewWindowOptions();
      newWindowOptions.setName(urlInfo.getWindow());
      globalDisplay_.openWindow(urlInfo.getUrl(), newWindowOptions);
   }
     
   public void onWorkbenchMetricsChanged(WorkbenchMetricsChangedEvent event)
   {
      lastWorkbenchMetrics_ = event.getWorkbenchMetrics();
      metricsChangedCommand_.nudge();
   }
   
   public void onQuotaStatus(QuotaStatusEvent event)
   {
      QuotaStatus quotaStatus = event.getQuotaStatus();
      
      // always show warning if the user is over quota
      if (quotaStatus.isOverQuota())
      {
         long over = quotaStatus.getUsed() - quotaStatus.getQuota();
         StringBuilder msg = new StringBuilder();
         msg.append("You are ");
         msg.append(StringUtil.formatFileSize(over));
         msg.append(" over your ");
         msg.append(StringUtil.formatFileSize(quotaStatus.getQuota()));
         msg.append(" file storage limit. Please remove files to ");
         msg.append("continue working.");
         globalDisplay_.showWarningBar(false, msg.toString());
      }
      
      // show a warning if the user is near their quota (but no more
      // than one time per instantiation of the application)
      else if (quotaStatus.isNearQuota() && !nearQuotaWarningShown_)
      {
         StringBuilder msg = new StringBuilder();
         msg.append("You are nearly over your ");
         msg.append(StringUtil.formatFileSize(quotaStatus.getQuota()));
         msg.append(" file storage limit.");
         globalDisplay_.showWarningBar(false, msg.toString());
         
         nearQuotaWarningShown_ = true;
      }
   }
  
   @Handler
   public void onSetWorkingDir()
   {
      fileDialogs_.chooseFolder(
            "Choose Working Directory",
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  // set console
                  consoleDispatcher_.executeSetWd(input, true); 
                  
                  // set files pane
                  eventBus_.fireEvent(new DirectoryNavigateEvent(input));
                  
                  indicator.onCompleted();
               }
            });
   }
   
   @Handler
   void onSetWorkingDirToProjectDir()
   {
      FileSystemItem projectDir = session_.getSessionInfo()
            .getActiveProjectDir();
      if (projectDir != null)
      {
         consoleDispatcher_.executeSetWd(projectDir, false);
         eventBus_.fireEvent(new DirectoryNavigateEvent(projectDir, true));
      }
   }
   
   @Handler
   public void onSourceFile()
   {
      fileDialogs_.openFile(
            "Source File",
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(FileSystemItem input, ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  indicator.onCompleted();

                  consoleDispatcher_.executeSourceCommand(
                        input.getPath(),
                        fileTypeRegistry_.getTextTypeForFile(input),
                        pPrefs_.get().defaultEncoding().getValue(),
                        false,
                        false,
                        true,
                        false);

                  commands_.activateConsole().execute();
               }
            });
   }
   
   @Handler
   public void onVersionControlShowRsaKey()
   {
      final ProgressIndicator indicator = new GlobalProgressDelayer(
            globalDisplay_, 500, "Reading RSA public key...").getIndicator();
     
      // compute path to public key
      String sshDir = session_.getSessionInfo().getDefaultSSHKeyDir();
      final String keyPath = FileSystemItem.createDir(sshDir).completePath(
                                                               "id_rsa.pub");
              
      // read it
      server_.gitSshPublicKey(keyPath, new ServerRequestCallback<String> () {
         
         @Override
         public void onResponseReceived(String publicKeyContents)
         {
            indicator.onCompleted();
            
            new ShowPublicKeyDialog("RSA Public Key", 
                                    publicKeyContents).showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            String msg = "Error attempting to read key '" + keyPath + "' (" +
                         error.getUserMessage() + ")";
            indicator.onError(msg);
         } 
      }); 
   }
   
   @Handler
   public void onShowShellDialog()
   {
      if (Desktop.isDesktop())
      {
         server_.getTerminalOptions(new SimpleRequestCallback<TerminalOptions>()
         {
            @Override
            public void onResponseReceived(TerminalOptions options)
            {
               Desktop.getFrame().openTerminal(options.getTerminalPath(),
                                               options.getWorkingDirectory(),
                                               options.getExtraPathEntries());
            }
         });
      }
      else
      {
         final ProgressIndicator indicator = new GlobalProgressDelayer(
               globalDisplay_, 500, "Starting shell...").getIndicator();
         
         server_.startShellDialog(new ServerRequestCallback<ConsoleProcess>() {

            @Override
            public void onResponseReceived(ConsoleProcess proc)
            {
               indicator.onCompleted();
               new ConsoleProgressDialog(proc, server_).showModal();
            }
            
            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
         });
      }
   }
   
   @Handler
   public void onToggleFullScreen()
   {
      if (Desktop.isDesktop() && Desktop.getFrame().supportsFullscreenMode())
         Desktop.getFrame().toggleFullscreenMode();
   }
   
   private void checkForInitMessages()
   {
      // only check for init messages in server mode
      if (!Desktop.isDesktop())
      {
         server_.getInitMessages(new ServerRequestCallback<String>() {
            @Override
            public void onResponseReceived(String message) 
            {
               if (message != null)
                  globalDisplay_.showWarningBar(false, message);
            }
            
            @Override
            public void onError(ServerError error)
            {
               // ignore
            }
         });
      }
   }
    
   
   public void onUserPrompt(UserPromptEvent event)
   {
      // is cancel supported?
      UserPrompt userPrompt = event.getUserPrompt();
                
      // resolve labels
      String yesLabel = userPrompt.getYesLabel();
      if (StringUtil.isNullOrEmpty(yesLabel))
         yesLabel = "Yes";
      String noLabel = userPrompt.getNoLabel();
      if (StringUtil.isNullOrEmpty(noLabel))
         noLabel = "No";
         
      // show dialog
      globalDisplay_.showYesNoMessage(
                 userPrompt.getType(),
                 userPrompt.getCaption(),
                 userPrompt.getMessage(),
                 userPrompt.getIncludeCancel(),
                 userPromptResponse(UserPrompt.RESPONSE_YES),
                 userPromptResponse(UserPrompt.RESPONSE_NO),
                 userPrompt.getIncludeCancel() ?
                       userPromptResponse(UserPrompt.RESPONSE_CANCEL) : null,
                 yesLabel, 
                 noLabel, 
                 userPrompt.getYesIsDefault());
   }
   
   private Operation userPromptResponse(final int response)
   {
      return new Operation() {
         public void execute()
         {
            server_.userPromptCompleted(response, 
                                        new SimpleRequestCallback<Void>());
            
         }
      };
   }
   
   @Override
   public void onInstallRtools(final InstallRtoolsEvent event)
   {
      if (BrowseCap.isWindowsDesktop())
      {
         Desktop.getFrame().installRtools(event.getVersion(),
                                          event.getInstallerPath());  
      }
   }
   
   private final Server server_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Provider<UIPrefs> pPrefs_;
   private final WorkbenchMainView view_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final RemoteFileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final WorkbenchContext workbenchContext_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final Provider<GitState> pGitState_;
   private final TimeBufferedCommand metricsChangedCommand_;
   private WorkbenchMetrics lastWorkbenchMetrics_;
   private boolean nearQuotaWarningShown_ = false;
}
