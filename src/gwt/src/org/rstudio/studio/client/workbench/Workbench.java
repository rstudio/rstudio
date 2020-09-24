/*
 * Workbench.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.filedialog.events.OpenFileDialogEvent;
import org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationVisibility;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.rstudioapi.AskSecretManager;
import org.rstudio.studio.client.common.vcs.AskPassManager;
import org.rstudio.studio.client.common.vcs.ShowPublicKeyDialog;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.htmlpreview.HTMLPreview;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewEvent;
import org.rstudio.studio.client.htmlpreview.events.ShowPageViewerEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.pdfviewer.PDFViewer;
import org.rstudio.studio.client.plumber.PlumberAPI;
import org.rstudio.studio.client.projects.ProjectOpener;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistryProvider;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.ShinyGadgetDialogEvent;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.remote.ExecuteUserCommandEvent;
import org.rstudio.studio.client.shiny.ShinyApplication;
import org.rstudio.studio.client.shiny.ui.ShinyGadgetDialog;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.commands.ReportShortcutBindingEvent;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.events.ShowMainMenuEvent.Menu;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.choosefile.ChooseFile;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerPresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialPresenter.Tutorial;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialCommandEvent;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialLaunchEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

public class Workbench implements BusyEvent.Handler,
                                  ShowErrorMessageEvent.Handler,
                                  UserPromptEvent.Handler,
                                  ShowWarningBarEvent.Handler,
                                  BrowseUrlEvent.Handler,
                                  QuotaStatusEvent.Handler,
                                  WorkbenchLoadedEvent.Handler,
                                  WorkbenchMetricsChangedEvent.Handler,
                                  InstallRtoolsEvent.Handler,
                                  ShinyGadgetDialogEvent.Handler,
                                  ExecuteUserCommandEvent.Handler,
                                  AdminNotificationEvent.Handler,
                                  OpenFileDialogEvent.Handler,
                                  ShowPageViewerEvent.Handler,
                                  TutorialLaunchEvent.Handler,
                                  DeferredInitCompletedEvent.Handler,
                                  ReportShortcutBindingEvent.Handler
{
   interface Binder extends CommandBinder<Commands, Workbench> {}

   @Inject
   public Workbench(WorkbenchMainView view,
                    WorkbenchContext workbenchContext,
                    GlobalDisplay globalDisplay,
                    Commands commands,
                    EventBus eventBus,
                    Session session,
                    Provider<UserPrefs> pPrefs,
                    Server server,
                    RemoteFileSystemContext fsContext,
                    FileDialogs fileDialogs,
                    FileTypeRegistry fileTypeRegistry,
                    ConsoleDispatcher consoleDispatcher,
                    WorkbenchNewSession newSession,
                    ProjectOpener projectOpener,
                    Provider<GitState> pGitState,
                    SourceWindowManager sourceWindowManager,
                    UserInterfaceHighlighter highlighter,       // force gin to create
                    ChooseFile chooseFile,                      // force gin to create
                    AskPassManager askPass,                     // force gin to create
                    PDFViewer pdfViewer,                        // force gin to create
                    HTMLPreview htmlPreview,                    // force gin to create
                    ProfilerPresenter prof,                     // force gin to create
                    ShinyApplication sApp,                      // force gin to create
                    PlumberAPI sAPI,                            // force gin to create
                    DependencyManager dm,                       // force gin to create
                    ApplicationVisibility av,                   // force gin to create
                    RmdOutput rmdOutput,                        // force gin to create
                    ProjectTemplateRegistryProvider provider,   // force gin to create
                    WorkbenchServerOperations serverOperations, // force gin to create
                    AskSecretManager askSecret)                 // force gin to create
  {
      view_ = view;
      workbenchContext_ = workbenchContext;
      projectOpener_ = projectOpener;
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
      newSession_ = newSession;
      serverOperations_ = serverOperations;
      sourceWindowManager_ = sourceWindowManager;

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
      eventBus.addHandler(ShinyGadgetDialogEvent.TYPE, this);
      eventBus.addHandler(ExecuteUserCommandEvent.TYPE, this);
      eventBus.addHandler(AdminNotificationEvent.TYPE, this);
      eventBus.addHandler(OpenFileDialogEvent.TYPE, this);
      eventBus.addHandler(ShowPageViewerEvent.TYPE, this);
      eventBus.addHandler(TutorialLaunchEvent.TYPE, this);
      eventBus.addHandler(DeferredInitCompletedEvent.TYPE, this);
      eventBus.addHandler(ReportShortcutBindingEvent.TYPE, this);

      // We don't want to send setWorkbenchMetrics more than once per 1/2-second
      metricsChangedCommand_ = new TimeBufferedCommand(500)
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
      return view_;
   }

   public void onWorkbenchLoaded(WorkbenchLoadedEvent event)
   {
      server_.initializeForMainWorkbench();

      FileSystemItem defaultDialogDir =
            session_.getSessionInfo().getActiveProjectDir();
      if (defaultDialogDir != null)
         workbenchContext_.setDefaultFileDialogDir(defaultDialogDir);

      checkForInitMessages();
      checkForLicenseMessage();

      RStudioGinjector.INSTANCE.getFocusVisiblePolyfill().load(null);

      if (Desktop.isDesktop() &&
          StringUtil.equals(session_.getSessionInfo().getVcsName(), VCSConstants.GIT_ID))
      {
         pGitState_.get().addVcsRefreshHandler(vcsRefreshEvent ->
         {
            String title = workbenchContext_.createWindowTitle();
            if (title != null)
               Desktop.getFrame().setWindowTitle(title);
         });
      }
   }

   public void onTutorialLaunch(final TutorialLaunchEvent event)
   {
      commands_.activateTutorial().execute();

      Tutorial tutorial = event.getTutorial();
      Scheduler.get().scheduleDeferred(() -> {

         TutorialCommandEvent commandEvent = new TutorialCommandEvent(
               TutorialCommandEvent.TYPE_LAUNCH_DEFAULT_TUTORIAL,
               tutorial.toJsObject());

         eventBus_.fireEvent(commandEvent);

      });
   }

   public void onDeferredInitCompleted(DeferredInitCompletedEvent ev)
   {
      checkForCrashHandlerPermission();
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
      globalDisplay_.showWarningBar(event.isSevere(), event.getMessage());
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

   @Override
   public void onShinyGadgetDialog(ShinyGadgetDialogEvent event)
   {
      new ShinyGadgetDialog(event.getCaption(),
                            event.getUrl(),
                            new Size(event.getPreferredWidth(),
                                     event.getPreferredHeight())).showModal();
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
   void onNewSession()
   {
      newSession_.openNewSession(globalDisplay_,
                                 workbenchContext_,
                                 serverOperations_,
                                 projectOpener_,
                                 server_);
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
               Desktop.getFrame().openTerminal(
                     StringUtil.notNull(options.getTerminalPath()),
                     StringUtil.notNull(options.getWorkingDirectory()),
                     StringUtil.notNull(options.getExtraPathEntries()),
                     options.getShellType());
            }
         });
      }
      else
      {
         eventBus_.fireEvent(new ActivateNamedTerminalEvent());
      }
   }

   @Handler
   public void onBrowseAddins()
   {
      BrowseAddinsDialog dialog = new BrowseAddinsDialog(new OperationWithInput<Command>()
      {
         @Override
         public void execute(Command input)
         {
            if (input != null)
               input.execute();
         }
      });
      dialog.showModal();
   }

   @Handler
   public void onModifyKeyboardShortcuts()
   {
      new ModifyKeyboardShortcutsWidget().showModal();
   }

   @Handler
   public void onToggleFullScreen()
   {
      if (Desktop.hasDesktopFrame())
         Desktop.getFrame().toggleFullscreenMode();
   }

   @Handler
   public void onShowFileMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.File));
   }

   @Handler
   public void onShowEditMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Edit));
   }

   @Handler
   public void onShowCodeMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Code));
   }

   @Handler
   public void onShowViewMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.View));
   }

   @Handler
   public void onShowPlotsMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Plots));
   }

   @Handler
   public void onShowSessionMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Session));
   }

   @Handler
   public void onShowBuildMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Build));
   }

   @Handler
   public void onShowDebugMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Debug));
   }

   @Handler
   public void onShowProfileMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Profile));
   }

   @Handler
   public void onShowToolsMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Tools));
   }

   @Handler
   public void onShowHelpMenu()
   {
      eventBus_.fireEvent(new ShowMainMenuEvent(Menu.Help));
   }

   private void checkForInitMessages()
   {
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
      else
      {
         Desktop.getFrame().getInitMessages(message ->
         {
            if (!StringUtil.isNullOrEmpty(message))
            {
               globalDisplay_.showLicenseWarningBar(false, message);
            }
         });
      }
   }

   private void checkForLicenseMessage()
   {
      String licenseMessage = session_.getSessionInfo().getLicenseMessage();
      if (!StringUtil.isNullOrEmpty(licenseMessage))
      {
         globalDisplay_.showLicenseWarningBar(false, licenseMessage);
      }
   }

   private void checkForCrashHandlerPermission()
   {
      boolean shouldPrompt = session_.getSessionInfo().getPromptForCrashHandlerPermission();
      if (shouldPrompt)
      {
         String message =
               "May we upload crash reports to RStudio automatically?\n\nCrash reports don't include " +
               "any personal information, except for IP addresses which are used to determine how many users " +
               "are affected by each crash.\n\nCrash reporting can be disabled at any time under the Global Options.";

         globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
               "Enable Automated Crash Reporting",
               message,
               false,
               new Operation() {
                  @Override
                  public void execute() {
                     server_.setUserCrashHandlerPrompted(true, new SimpleRequestCallback<Void>());
                  }
               },
               new Operation() {
                  @Override
                  public void execute() {
                     server_.setUserCrashHandlerPrompted(false, new SimpleRequestCallback<Void>());
                  }
               },
               true);
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

   public void onAdminNotification(AdminNotificationEvent event)
   {
      AdminNotification notification = event.getAdminNotification();

      // show dialog
      globalDisplay_.showMessage(notification.getType(),
                                 "Admin Notification",
                                 notification.getMessage(),
                                 adminNotificationAcknowledged(notification.getId()));
   }

   @Override
   public void onOpenFileDialog(OpenFileDialogEvent event)
   {
      final ProgressOperationWithInput<FileSystemItem> onSelected =
            new ProgressOperationWithInput<FileSystemItem>()
      {
         @Override
         public void execute(FileSystemItem input,
                             ProgressIndicator indicator)
         {
            indicator.onCompleted();

            server_.openFileDialogCompleted(
                  input == null ? "" : input.getPath(),
                  new VoidServerRequestCallback());
         }
      };

      String caption = event.getCaption();
      String label = event.getLabel();
      int type = event.getType();
      FileSystemItem initialFilePath = event.getFile();
      String filter = event.getFilter();
      boolean selectExisting = event.selectExisting();

      if (type == OpenFileDialogEvent.TYPE_SELECT_FILE)
      {
         if (selectExisting)
         {
            fileDialogs_.openFile(
                  caption,
                  label,
                  fsContext_,
                  initialFilePath,
                  filter,
                  false,
                  false,
                  onSelected);
         }
         else
         {
            fileDialogs_.saveFile(
                  caption,
                  label,
                  fsContext_,
                  initialFilePath,
                  "",
                  false,
                  false,
                  onSelected);
         }
      }
      else if (type == OpenFileDialogEvent.TYPE_SELECT_DIRECTORY)
      {
         fileDialogs_.chooseFolder(
               caption,
               label,
               fsContext_,
               initialFilePath,
               false,
               onSelected);
      }
      else
      {
         assert false: "unexpected file dialog type '" + type + "'";
         server_.openFileDialogCompleted(null, new VoidServerRequestCallback());
      }
   }

   private Operation adminNotificationAcknowledged(final String id)
   {
      return new Operation() {
         public void execute()
         {
            server_.adminNotificationAcknowledged(id, new SimpleRequestCallback<Void>());
         }
      };
   }

   @Override
   public void onInstallRtools(final InstallRtoolsEvent event)
   {
      if (BrowseCap.isWindowsDesktop())
      {
         Desktop.getFrame().installRtools(StringUtil.notNull(event.getVersion()),
                                          StringUtil.notNull(event.getInstallerPath()));
      }
   }

   @Override
   public void onShowPageViewer(ShowPageViewerEvent event)
   {
      // show the page viewer window
      HTMLPreviewParams params = event.getParams();
      eventBus_.fireEvent(new ShowHTMLPreviewEvent(params));

      // server will now take care of sending the html_preview_completed event
   }

   @Override
   public void onExecuteUserCommand(ExecuteUserCommandEvent event)
   {
      server_.executeUserCommand(event.getCommandName(), new VoidServerRequestCallback());
   }

   @Override
   public void onReportShortcutBinding(ReportShortcutBindingEvent event)
   {
      AppCommand command = commands_.getCommandById(event.getCommand());
      if (command == null)
         globalDisplay_.showWarningBar(false, event.getCommand());
      else
         globalDisplay_.showWarningBar(false, event.getCommand() + " : " + command.summarize());
   }

   private final Server server_;
   private final WorkbenchServerOperations serverOperations_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Provider<UserPrefs> pPrefs_;
   private final WorkbenchMainView view_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final RemoteFileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final WorkbenchContext workbenchContext_;
   private final ProjectOpener projectOpener_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final Provider<GitState> pGitState_;
   private final TimeBufferedCommand metricsChangedCommand_;
   private final SourceWindowManager sourceWindowManager_;
   private WorkbenchMetrics lastWorkbenchMetrics_;
   private final WorkbenchNewSession newSession_;
   private boolean nearQuotaWarningShown_ = false;
}
