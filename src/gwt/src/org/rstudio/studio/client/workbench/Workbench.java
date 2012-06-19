/*
 * Workbench.java
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
package org.rstudio.studio.client.workbench;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
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
import org.rstudio.studio.client.common.vcs.AskPassManager;
import org.rstudio.studio.client.common.vcs.ShowPublicKeyDialog;
import org.rstudio.studio.client.htmlpreview.HTMLPreview;
import org.rstudio.studio.client.pdfviewer.PDFViewer;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.choosefile.ChooseFile;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

public class Workbench implements BusyHandler,
                                  ShowErrorMessageHandler,
                                  ShowWarningBarHandler,
                                  BrowseUrlHandler,
                                  QuotaStatusHandler,
                                  WorkbenchLoadedHandler,
                                  WorkbenchMetricsChangedHandler
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
                    ConsoleDispatcher consoleDispatcher,
                    ChooseFile chooseFile,   // required to force gin to create
                    AskPassManager askPass,  // required to force gin to create
                    PDFViewer pdfViewer,     // required to force gin to create
                    HTMLPreview htmlPreview) // required to force gin to create
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
      consoleDispatcher_ = consoleDispatcher;
      
      ((Binder)GWT.create(Binder.class)).bind(commands, this);
      
      // edit
      eventBus.addHandler(BusyEvent.TYPE, this);
      eventBus.addHandler(ShowErrorMessageEvent.TYPE, this);
      eventBus.addHandler(ShowWarningBarEvent.TYPE, this);
      eventBus.addHandler(BrowseUrlEvent.TYPE, this);
      eventBus.addHandler(QuotaStatusEvent.TYPE, this);
      eventBus.addHandler(WorkbenchLoadedEvent.TYPE, this);
      eventBus.addHandler(WorkbenchMetricsChangedEvent.TYPE, this);

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
      newWindowOptions.setAlwaysUseBrowser(true);
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
                        pPrefs_.get().defaultEncoding().getValue(),
                        false,
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

   private final Server server_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Provider<UIPrefs> pPrefs_;
   private final WorkbenchMainView view_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final RemoteFileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private final WorkbenchContext workbenchContext_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final TimeBufferedCommand metricsChangedCommand_;
   private WorkbenchMetrics lastWorkbenchMetrics_;
   private boolean nearQuotaWarningShown_ = false; 
}
