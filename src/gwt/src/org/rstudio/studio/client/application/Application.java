/*
 * Application.java
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

package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DocumentEx;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit.QuitContext;
import org.rstudio.studio.client.application.events.*;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.model.InvalidSessionInfo;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;
import org.rstudio.studio.client.application.model.SessionInitOptions;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.application.ui.AboutDialog;
import org.rstudio.studio.client.application.ui.RTimeoutOptions;
import org.rstudio.studio.client.application.ui.RequestLogVisualization;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.mathjax.MathJaxLoader;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.events.NewProjectEvent;
import org.rstudio.studio.client.projects.events.OpenProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.ClientStateUpdater;
import org.rstudio.studio.client.workbench.Workbench;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.SessionOpener;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

@Singleton
public class Application implements ApplicationEventHandlers
{
   public interface Binder extends CommandBinder<Commands, Application> {}

   @Inject
   public Application(ApplicationView view,
                      GlobalDisplay globalDisplay,
                      EventBus events,
                      Binder binder,
                      Commands commands,
                      Server server,
                      Session session,
                      Projects projects,
                      SatelliteManager satelliteManager,
                      ApplicationUncaughtExceptionHandler uncaughtExHandler,
                      ApplicationTutorialApi tutorialApi,
                      SessionOpener sessionOpener,
                      Provider<UserPrefs> userPrefs,
                      Provider<UserState> userState,
                      Provider<Workbench> workbench,
                      Provider<EventBus> eventBusProvider,
                      Provider<ClientStateUpdater> clientStateUpdater,
                      Provider<ApplicationClientInit> pClientInit,
                      Provider<ApplicationQuit> pApplicationQuit,
                      Provider<ApplicationInterrupt> pApplicationInterrupt,
                      Provider<ApplicationThemes> pAppThemes,
                      Provider<ProductEditionInfo> pEdition)
   {
      // save references
      view_ = view;
      globalDisplay_ = globalDisplay;
      events_ = events;
      session_ = session;
      commands_ = commands;
      satelliteManager_ = satelliteManager;
      clientStateUpdater_ = clientStateUpdater;
      server_ = server;
      sessionOpener_ = sessionOpener;
      userPrefs_ = userPrefs;
      userState_ = userState;
      workbench_ = workbench;
      eventBusProvider_ = eventBusProvider;
      pClientInit_ = pClientInit;
      pApplicationQuit_ = pApplicationQuit;
      pApplicationInterrupt_ = pApplicationInterrupt;
      pEdition_ = pEdition;
      pAppThemes_ = pAppThemes;

      // bind to commands
      binder.bind(commands_, this);

      // register as main window
      satelliteManager.initialize();

      // subscribe to events
      events.addHandler(LogoutRequestedEvent.TYPE, this);
      events.addHandler(UnauthorizedEvent.TYPE, this);
      events.addHandler(ReloadEvent.TYPE, this);
      events.addHandler(ReloadWithLastChanceSaveEvent.TYPE, this);
      events.addHandler(QuitEvent.TYPE, this);
      events.addHandler(SuicideEvent.TYPE, this);
      events.addHandler(SessionAbendWarningEvent.TYPE, this);
      events.addHandler(SessionSerializationEvent.TYPE, this);
      events.addHandler(SessionRelaunchEvent.TYPE, this);
      events.addHandler(ServerUnavailableEvent.TYPE, this);
      events.addHandler(InvalidClientVersionEvent.TYPE, this);
      events.addHandler(ServerOfflineEvent.TYPE, this);
      events.addHandler(InvalidSessionEvent.TYPE, this);
      events.addHandler(SwitchToRVersionEvent.TYPE, this);
      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(FileUploadEvent.TYPE, this);
      events.addHandler(AriaLiveStatusEvent.TYPE, this);

      // register for uncaught exceptions
      uncaughtExHandler.register();
   }

   public void go(final RootLayoutPanel rootPanel,
                  final RTimeoutOptions timeoutOptions,
                  final Command dismissLoadingProgress,
                  final ServerRequestCallback<String> connectionStatusCallback)
   {
      rootPanel_ = rootPanel;

      Widget w = view_.getWidget();
      rootPanel.add(w);

      rootPanel.setWidgetTopBottom(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
      rootPanel.setWidgetLeftRight(w, 0, Style.Unit.PX, 0, Style.Unit.PX);

      final ServerRequestCallback<SessionInfo> callback = new ServerRequestCallback<SessionInfo>() {

         public void onResponseReceived(final SessionInfo sessionInfo)
         {
            // initialize workbench
            // if this is a switch project then wait to dismiss the
            // loading progress animation for 10 seconds. typically
            // this will be enough time to switch projects. if it
            // isn't then it's nice to reveal whatever progress
            // operation or error state is holding up the switch
            // directly to the user
            if (ApplicationAction.isSwitchProject())
            {
               new Timer() {
                  @Override
                  public void run()
                  {
                     dismissLoadingProgress.execute();
                  }
               }.schedule(10000);
            }
            else
            {
               dismissLoadingProgress.execute();
            }

            session_.setSessionInfo(sessionInfo);

            // load MathJax
            MathJaxLoader.ensureMathJaxLoaded();

            // initialize workbench
            // refresh prefs incase they were loaded without sessionInfo (this happens exclusively
            // in desktop mode, though unsure why)
            userState_.get().writeState(boolArg ->
            {
               userPrefs_.get().writeUserPrefs(boolArg1 ->
               {
                  initializeWorkbench();
               });
            });
         }

         public void onError(ServerError error)
         {
            Debug.logError(error);
            dismissLoadingProgress.execute();

            if (!StringUtil.isNullOrEmpty(error.getRedirectUrl()))
            {
               // error is informing us that we should redirect
               // redirect to the specified URL (as a sub URL of the site's root)
               String redirectUrl = ApplicationUtils.getHostPageBaseURLWithoutContext(false) +
            		   error.getRedirectUrl();
               navigateWindowWithDelay(redirectUrl);
            }
            else
            {
               globalDisplay_.showErrorMessage("RStudio Initialization Error",
                                               error.getUserMessage());
            }
         }
      };

      final ApplicationClientInit clientInit = pClientInit_.get();

      if (timeoutOptions != null)
      {
         timeoutOptions.setObserver(clientInit);
      }

      // read options from querystring
      SessionInitOptions options = SessionInitOptions.create(
            SessionInitOptions.RESTORE_WORKSPACE_DEFAULT,
            SessionInitOptions.RUN_RPROFILE_DEFAULT);
      try
      {
         String restore = Window.Location.getParameter(SessionInitOptions.RESTORE_WORKSPACE_OPTION);
         if (!StringUtil.isNullOrEmpty(restore))
         {
            options.setRestoreWorkspace(Integer.parseInt(restore));
         }

         String run = Window.Location.getParameter(SessionInitOptions.RUN_RPROFILE_OPTION);
         if (!StringUtil.isNullOrEmpty(run))
         {
            options.setRunRprofile(Integer.parseInt(run));
         }
      }
      catch(Exception e)
      {
         // lots of opportunities for exceptions from malformed querystrings;
         // eat them and log them here so that we can still init the client with
         // default options
         Debug.logException(e);
      }

      // attempt init
      clientInit.execute(callback, options, true);

      sessionOpener_.getJobConnectionStatus(connectionStatusCallback);
   }

   @Handler
   public void onShowToolbar()
   {
      setToolbarPref(true);
   }

   @Handler
   public void onHideToolbar()
   {
      setToolbarPref(false);
   }

   @Handler
   public void onToggleToolbar()
   {
      setToolbarPref(!view_.isToolbarShowing());
   }

   @Handler
   public void onFocusMainToolbar()
   {
      view_.focusToolbar();
   }

   @Handler
   void onSignOut()
   {
      events_.fireEvent(new LogoutRequestedEvent());
   }

   @Handler
   void onLoadServerHome()
   {
      loadUserHomePage();
   }

   @Handler
   void onShowAboutDialog()
   {
      server_.getProductInfo(new ServerRequestCallback<ProductInfo>()
      {
         @Override
         public void onResponseReceived(ProductInfo info)
         {
            AboutDialog about = new AboutDialog(info);
            about.showModal();
         }
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   @Handler
   void onShowLicenseDialog()
   {
      if (pEdition_.get() != null)
      {
         pEdition_.get().showLicense();
      }
   }

   @Handler
   void onShowSessionServerOptionsDialog()
   {
      if (pEdition_.get() != null)
      {
         pEdition_.get().showSessionServerOptionsDialog();
      }
   }

   @Override
   public void onUnauthorized(UnauthorizedEvent event)
   {
      // if the user is currently uploading a file (which potentially takes a long time)
      // and we were to navigate them, they would be unable to complete the upload
      if (!fileUploadInProgress_)
      {
         server_.disconnect();
         navigateToSignIn();
      }
   }

   @Override
   public void onFileUpload(FileUploadEvent event)
   {
      fileUploadInProgress_ = event.inProgress();
   }

   @Override
   public void onAriaLiveStatus(AriaLiveStatusEvent event)
   {
      int delayMs = (event.getTiming() == Timing.IMMEDIATE) ?
            0 : userPrefs_.get().typingStatusDelayMs().getValue();
      if (!ModalDialogTracker.dispatchAriaLiveStatus(event.getMessage(), delayMs, event.getSeverity()))
         view_.reportStatus(event.getMessage(), delayMs, event.getSeverity());
   }

   @Override
   public void onServerOffline(ServerOfflineEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationOffline();
   }

   @Override
   public void onLogoutRequested(LogoutRequestedEvent event)
   {
      cleanupWorkbench();

      // create an invisible form to host the sign-out process
      FormElement form = DocumentEx.get().createFormElement();
      form.setMethod("POST");
      form.setAction(absoluteUrl("auth-sign-out", true));
      form.getStyle().setDisplay(Display.NONE);

      InputElement csrfToken = DocumentEx.get().createHiddenInputElement();
      csrfToken.setName(CSRF_TOKEN_FIELD);
      csrfToken.setValue(ApplicationCsrfToken.getCsrfToken());
      form.appendChild(csrfToken);

      // append the form to the document and submit it
      DocumentEx.get().getBody().appendChild(form);
      form.submit();

      if (Desktop.isRemoteDesktop())
      {
         // let the desktop application know that we are signing out
         Desktop.getFrame().signOut();
      }
   }

   @Handler
   public void onHelpUsingRStudio()
   {
      String customDocsURL = session_.getSessionInfo().docsURL();
      if (customDocsURL.length() > 0)
         globalDisplay_.openWindow(customDocsURL);
      else
         globalDisplay_.openRStudioLink("docs");
   }

   @Handler
   public void onRstudioCommunityForum()
   {
      globalDisplay_.openRStudioLink("community-forum");
   }

   @Handler
   public void onRstudioSupport()
   {
      globalDisplay_.openRStudioLink("support");
   }

   @Handler
   public void onUpdateCredentials()
   {
      server_.updateCredentials();
   }

   @Handler
   public void onRaiseException() {
      throw new RuntimeException("foo");
   }

   @Handler
   public final native void onRaiseException2() /*-{
      $wnd.welfkjweg();
   }-*/;

   @Handler
   public void onShowRequestLog()
   {
      GWT.runAsync(new RunAsyncCallback()
      {
         public void onFailure(Throwable reason)
         {
            Window.alert(reason.toString());
         }

         public void onSuccess()
         {
            final RequestLogVisualization viz = new RequestLogVisualization();
            final RootLayoutPanel root = RootLayoutPanel.get();
            root.add(viz);
            root.setWidgetTopBottom(viz, 10, Unit.PX, 10, Unit.PX);
            root.setWidgetLeftRight(viz, 10, Unit.PX, 10, Unit.PX);
            viz.addCloseHandler(new CloseHandler<RequestLogVisualization>()
            {
               public void onClose(CloseEvent<RequestLogVisualization> event)
               {
                  root.remove(viz);
               }
            });
         }
      });
   }

   @Handler
   public void onLogFocusedElement()
   {
      Element el = DomUtils.getActiveElement();
      DomUtils.dump(el, "Focused Element: ");
   }

   @Handler
   public void onRefreshSuperDevMode()
   {
      SuperDevMode.reload();
   }

   @Override
   public void onSessionSerialization(SessionSerializationEvent event)
   {
      switch(event.getAction().getType())
      {
      case SessionSerializationAction.LOAD_DEFAULT_WORKSPACE:
         view_.showSerializationProgress(
                         "Loading workspace" + getSuffix(event),
                         false, // non-modal, appears to user as std latency
                         500,   // willing to show progress earlier since
                                // this will always be at workbench startup
                         0);    // no timeout
         break;
      case SessionSerializationAction.SAVE_DEFAULT_WORKSPACE:
         view_.showSerializationProgress(
                          "Saving workspace image" + getSuffix(event),
                          true, // modal, inputs will fall dead anyway
                          0,    // show immediately
                          0);   // no timeout
         break;
      case SessionSerializationAction.SUSPEND_SESSION:
         events_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.SESSION_SUSPEND));
         view_.showSerializationProgress(
                          "Backing up R session...",
                          true,    // modal, inputs will fall dead anyway
                          0,       // show immediately
                          60000);  // timeout after 60 seconds. this is done
                                   // in case the user suspends or loses
                                   // connectivity during the backup (in which
                                   // case the 'completed' event dies with
                                   // server and is never received by the client
         break;
      case SessionSerializationAction.RESUME_SESSION:
         view_.showSerializationProgress(
                          "Resuming R session...",
                          false, // non-modal, appears to user as std latency
                          2000,  // don't show this for reasonable restore time
                                 // (happens inline while using a running
                                 // workbench so be more conservative)
                          0);    // no timeout
         break;
      case SessionSerializationAction.COMPLETED:
         view_.hideSerializationProgress();
         break;
      }
   }

   @Override
   public void onSessionRelaunch(SessionRelaunchEvent event)
   {
      switch (event.getType())
      {
      case RELAUNCH_INITIATED:
         // session needs to be relaunched
         // redirect to where the server instructed us to go
         if (!event.getRedirectUrl().isEmpty())
         {
            String url = ApplicationUtils.getHostPageBaseURLWithoutContext(false) + event.getRedirectUrl();
            navigateWindowWithDelay(url);
         }
         else
         {
            // server did not specify where to redirect - fallback to the home page
            loadUserHomePage();
         }
         break;
      case RELAUNCH_COMPLETE:
         view_.hideSerializationProgress();
         break;
      }
   }

   private String getSuffix(SessionSerializationEvent event)
   {
      SessionSerializationAction action = event.getAction();
      String targetPath = action.getTargetPath();
      if (targetPath != null)
      {
         String verb = " from ";
         if (action.getType() == SessionSerializationAction.SAVE_DEFAULT_WORKSPACE)
            verb = " to ";
         return verb + targetPath + "...";
      }
      else
      {
         return "...";
      }
   }

   @Override
   public void onServerUnavailable(ServerUnavailableEvent event)
   {
      view_.hideSerializationProgress();
   }

   @Override
   public void onSwitchToRVersion(final SwitchToRVersionEvent event)
   {
      final ApplicationQuit applicationQuit = pApplicationQuit_.get();
      applicationQuit.prepareForQuit("Switch R Version", new QuitContext() {
         public void onReadyToQuit(boolean saveChanges)
         {
            // see if we have a project (otherwise switch to "None")
            String project = session_.getSessionInfo().getActiveProjectFile();
            if (project == null)
               project = Projects.NONE;

            // do the quit
            applicationQuit.performQuit(null,
                                        saveChanges,
                                        project,
                                        event.getRVersionSpec());
         }
      });
   }

   @Override
   public void onReload(ReloadEvent event)
   {
      cleanupWorkbench();

      reloadWindowWithDelay(false);
   }

   @Override
   public void onReloadWithLastChanceSave(ReloadWithLastChanceSaveEvent event)
   {
      Barrier barrier = new Barrier();
      barrier.addBarrierReleasedHandler(releasedEvent ->
      {
         events_.fireEvent(new ReloadEvent());
      });

      Token token = barrier.acquire();
      try
      {
         events_.fireEvent(new LastChanceSaveEvent(barrier));
      }
      finally
      {
         token.release();
      }
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // don't try to persist client state while restarting
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
      {
         pauseClientStateUpdater();
      }
      else if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
      {
         resumeClientStateUpdater();
      }
   }

   @Override
   public void onQuit(QuitEvent event)
   {
      cleanupWorkbench();

      // only show the quit state in server mode (desktop mode has its
      // own handling triggered to process exit)
      if (!Desktop.isDesktop())
      {
         if (event.getSwitchProjects())
         {
            String nextSessionUrl = event.getNextSessionUrl();
            sessionOpener_.switchSession(nextSessionUrl);
         }
         else
         {
            if (session_.getSessionInfo().getMultiSession())
            {
               view_.showApplicationMultiSessionQuit();
            }
            else
            {
               view_.showApplicationQuit();
            }

            if (Desktop.isRemoteDesktop())
            {
               // inform the desktop application that the remote session has finished quitting
               Desktop.getFrame().onSessionQuit();
            }

            // attempt to close the window if this is a quit
            // action (may or may not be able to depending on
            // how it was created)
            if (ApplicationAction.isQuit() && !ApplicationAction.isQuitToHome())
            {
               try
               {
                  WindowEx.get().close();
               }
               catch(Exception ex)
               {
               }
            }
            else if (session_.getSessionInfo().getShowUserHomePage())
            {
               if (!Desktop.isRemoteDesktop())
                  loadUserHomePage();
            }
         }
      }
   }

   public void loadUserHomePage()
   {
      assert session_.getSessionInfo().getShowUserHomePage();

      navigateWindowWithDelay(
            session_.getSessionInfo().getUserHomePageUrl());
   }

   public void reloadWindowWithDelay(final boolean baseUrlOnly)
   {
      new Timer() {
         @Override
         public void run()
         {
            if (baseUrlOnly)
               Window.Location.replace(GWT.getHostPageBaseURL());
            else
               Window.Location.reload();
         }
      }.schedule(100);
   }

   public void navigateWindowWithDelay(final String url)
   {
      new Timer() {
         @Override
         public void run()
         {
            Window.Location.replace(url);
         }
      }.schedule(100);
   }

   @Override
   public void onSuicide(SuicideEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationSuicide(event.getMessage());
   }

   @Override
   public void onClientDisconnected(ClientDisconnectedEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationDisconnected();
   }

   @Override
   public void onInvalidClientVersion(InvalidClientVersionEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationUpdateRequired();
   }


   @Override
   public void onInvalidSession(InvalidSessionEvent event)
   {
      // calculate the url without the scope
      InvalidSessionInfo info = event.getInfo();
      String baseURL = GWT.getHostPageBaseURL();
      String scopePath = info.getScopePath();
      int loc = baseURL.indexOf(scopePath);
      if (loc != -1)
         baseURL = baseURL.substring(0, loc) + "/";

      if (info.getScopeState() == InvalidSessionInfo.ScopeMissingProject)
      {
         baseURL += "projectnotfound.htm";
      }
      else
      {
         // add the scope info to the query string
         baseURL += "?project="
               + URL.encodeQueryString(info.getSessionProject()) + "&id="
               + URL.encodeQueryString(info.getSessionProjectId());
      }
      navigateWindowWithDelay(baseURL);
   }

   @Override
   public void onSessionAbendWarning(SessionAbendWarningEvent event)
   {
      view_.showSessionAbendWarning();
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      if (Satellite.isCurrentWindowSatellite())
         return;

      final SessionInfo info = RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      if (info.getInitOptions() == null)
         return;

      String warning = "";
      int restoreWorkspace = info.getInitOptions().restoreWorkspace();
      if (restoreWorkspace == SessionInitOptions.RESTORE_WORKSPACE_NO)
      {
         warning += "The workspace was not restored";
         if (info.getInitOptions().runRprofile() == SessionInitOptions.RUN_RPROFILE_NO)
         {
            warning += ", and startup scripts were not executed";
         }
         warning += ".";
      }
      else
      {
         int runRprofile = info.getInitOptions().runRprofile();
         if (runRprofile == SessionInitOptions.RUN_RPROFILE_NO)
            warning += "Startup scripts were not executed.";
      }
      if (!StringUtil.isNullOrEmpty(warning))
      {
         globalDisplay_.showWarningBar(false,
               "This R session was started in safe mode. " + warning);
      }
   }

   private void navigateWindowTo(String relativeUrl)
   {
      navigateWindowTo(relativeUrl, true);
   }

   private void navigateWindowTo(String relativeUrl, boolean includeContext)
   {
      cleanupWorkbench();

      // navigate window
      Window.Location.replace(absoluteUrl(relativeUrl, includeContext));
   }

   private String absoluteUrl(String relativeUrl, boolean includeContext)
   {
      // ensure there is no session context if requested
      String url = includeContext ?
            GWT.getHostPageBaseURL() :
            ApplicationUtils.getHostPageBaseURLWithoutContext(true);

      // add relative URL
      url += relativeUrl;

      return url;
   }
   private void initializeWorkbench()
   {
      // Initialize application theme system
      pAppThemes_.get().initializeThemes(rootPanel_.getElement());

      // subscribe to ClientDisconnected event (wait to do this until here
      // because there were spurious ClientDisconnected events occurring
      // after a session interrupt sequence. we couldn't figure out why,
      // and since this is a temporary hack why not add another temporary
      // hack to go with it here :-)
      // TODO: move this back to the constructor after we revise the
      // interrupt hack(s)
      events_.addHandler(ClientDisconnectedEvent.TYPE, this);

      // create workbench
      Workbench wb = workbench_.get();
      eventBusProvider_.get().fireEvent(new SessionInitEvent());

      // disable commands
      SessionInfo sessionInfo = session_.getSessionInfo();

      if (BrowseCap.isWindowsDesktop())
      {
         commands_.interruptTerminal().remove();
      }

      if (!sessionInfo.getAllowShell())
      {
         commands_.showShellDialog().remove();
         removeTerminalCommands();
      }

      if (!sessionInfo.getPresentationState().isActive())
         commands_.activatePresentation().remove();

      if (!sessionInfo.getAllowVcs())
         commands_.showVcsOptions().remove();

      if (!sessionInfo.getAllowPublish())
         commands_.showPublishingOptions().remove();

      if (!sessionInfo.getAllowFullUI())
      {
         removeProjectCommands();
      }

      if (Desktop.isDesktop() && !Desktop.isRemoteDesktop())
         commands_.signOut().remove();
      else if (!sessionInfo.getShowIdentity() || !sessionInfo.getAllowFullUI())
         commands_.signOut().remove();

      if (Desktop.isDesktop() ||
         !sessionInfo.getAllowFullUI() ||
         !sessionInfo.getShowUserHomePage() ||
         StringUtil.isNullOrEmpty(sessionInfo.getUserHomePageUrl()))
      {
         commands_.loadServerHome().remove();
      }

      if (!sessionInfo.getLauncherJobsEnabled())
      {
         removeJobLauncherCommands();
      }

      // only enable suspendSession() in devmode
      commands_.suspendSession().setVisible(SuperDevMode.isActive());

      if (!sessionInfo.getAllowPackageInstallation())
      {
         commands_.installPackage().remove();
         commands_.updatePackages().remove();
      }
      if (!sessionInfo.getAllowVcs())
      {
         commands_.versionControlProjectSetup().remove();
      }
      if (!sessionInfo.getAllowFileDownloads())
      {
         commands_.exportFiles().remove();
      }
      if (!sessionInfo.getAllowFileUploads())
      {
         commands_.uploadFile().remove();
      }

      // disable external publishing if requested
      if (!SessionUtils.showExternalPublishUi(session_, userState_.get()))
      {
         commands_.publishHTML().remove();
      }

      // remove knit params if they aren't supported
      if (!sessionInfo.getKnitParamsAvailable())
         commands_.knitWithParameters().remove();

      // show the correct set of data import commands
      if (userPrefs_.get().useDataimport().getValue())
      {
         commands_.importDatasetFromFile().remove();
         commands_.importDatasetFromURL().remove();

         commands_.importDatasetFromCsvUsingReadr().setVisible(false);
         commands_.importDatasetFromSAV().setVisible(false);
         commands_.importDatasetFromSAS().setVisible(false);
         commands_.importDatasetFromStata().setVisible(false);

         try
         {
            String rVersion = sessionInfo.getRVersionsInfo().getRVersion();
            if (ApplicationUtils.compareVersions(rVersion, "3.0.2") >= 0)
            {
               commands_.importDatasetFromCsvUsingReadr().setVisible(true);
            }
            if (ApplicationUtils.compareVersions(rVersion, "3.1.0") >= 0)
            {
               commands_.importDatasetFromSAV().setVisible(true);
               commands_.importDatasetFromSAS().setVisible(true);
               commands_.importDatasetFromStata().setVisible(true);
            }
         }
         catch (Exception e)
         {
         }
      }
      else
      {
         commands_.importDatasetFromCsv().remove();
         commands_.importDatasetFromCsvUsingBase().remove();
         commands_.importDatasetFromCsvUsingReadr().remove();
         commands_.importDatasetFromSAV().remove();
         commands_.importDatasetFromSAS().remove();
         commands_.importDatasetFromStata().remove();
         commands_.importDatasetFromXLS().remove();
      }

      Element el = Document.get().getElementById("rstudio_container");
      if (el == null)
      {
         // some satellite windows don't have "rstudio_container"
         el = view_.getWidget().getElement();
      }

      // "application" role prioritizes application keyboard handling
      // over screen-reader shortcuts
      el.setAttribute("role", "application");

      // If no project, ensure we show the product-edition title; if there is a project
      // open this was already done
      if (!Desktop.isDesktop() &&
            session_.getSessionInfo().getActiveProjectFile() == null &&
            pEdition_.get() != null)
      {
         // set title so tab has product edition name
         Document.get().setTitle(pEdition_.get().editionName());
      }

      // show workbench
      view_.showWorkbenchView(wb.getMainView().asWidget());

      // hide zoom in and zoom out in web mode
      if (!Desktop.hasDesktopFrame())
      {
         commands_.zoomActualSize().remove();
         commands_.zoomIn().remove();
         commands_.zoomOut().remove();
      }

      // remove main menu commands in desktop mode
      if (Desktop.hasDesktopFrame())
      {
         commands_.showFileMenu().remove();
         commands_.showEditMenu().remove();
         commands_.showCodeMenu().remove();
         commands_.showViewMenu().remove();
         commands_.showPlotsMenu().remove();
         commands_.showSessionMenu().remove();
         commands_.showBuildMenu().remove();
         commands_.showDebugMenu().remove();
         commands_.showProfileMenu().remove();
         commands_.showToolsMenu().remove();
         commands_.showHelpMenu().remove();
      }

      // show new session when appropriate
      if (!Desktop.hasDesktopFrame())
      {
         if (sessionInfo.getMultiSession())
            commands_.newSession().setMenuLabel("New Session...");
         else
            commands_.newSession().remove();
      }

      // show support link only in RStudio Pro
      if (pEdition_.get() != null)
      {
         if (!pEdition_.get().proLicense())
            commands_.rstudioSupport().remove();

         // pro-only menu items
         if (!pEdition_.get().proLicense() || !Desktop.hasDesktopFrame())
         {
            commands_.showLicenseDialog().remove();
            commands_.showSessionServerOptionsDialog().remove();
         }
      }

      // toolbar (must be after call to showWorkbenchView because
      // showing the toolbar repositions the workbench view widget)
      showToolbar(userPrefs_.get().toolbarVisible().getValue(), false);

      // sync to changes in the toolbar visibility state
      userPrefs_.get().toolbarVisible().addValueChangeHandler(
            valueChangeEvent -> showToolbar(valueChangeEvent.getValue(), true));

      clientStateUpdaterInstance_ = clientStateUpdater_.get();

      // initiate action if requested. do this after a delay
      // so that the source database has time to load
      // before we interrogate it for unsaved documents
      if (ApplicationAction.hasAction())
      {
         new Timer() {
            @Override
            public void run() {
               if (ApplicationAction.isQuit())
               {
                  commands_.quitSession().execute();
               }
               else if (ApplicationAction.isNewProject())
               {
                  ApplicationAction.removeActionFromUrl();
                  events_.fireEvent(new NewProjectEvent(true, false));
               }
               else if (ApplicationAction.isOpenProject())
               {
                  ApplicationAction.removeActionFromUrl();
                  events_.fireEvent(new OpenProjectEvent(true, false));
               }
               else if (ApplicationAction.isSwitchProject())
               {
                  handleSwitchProjectAction();
               }
            }
         }.schedule(500);
      }
   }

   private void handleSwitchProjectAction()
   {
      String projectId = ApplicationAction.getId();
      if (projectId.length() > 0)
      {
         server_.getProjectFilePath(
            projectId,
            new ServerRequestCallback<String>() {

               @Override
               public void onResponseReceived(String projectFilePath)
               {
                  if (projectFilePath.length() > 0)
                  {
                     events_.fireEvent(
                           new SwitchToProjectEvent(projectFilePath, true));
                  }
               }
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }

            });
      }
   }


   private void setToolbarPref(boolean showToolbar)
   {
      userPrefs_.get().toolbarVisible().setGlobalValue(showToolbar);
      userPrefs_.get().writeUserPrefs();
   }

   private void showToolbar(boolean showToolbar, boolean announce)
   {
      // show or hide the toolbar
      view_.showToolbar(showToolbar, announce);

      // manage commands
      commands_.showToolbar().setVisible(!showToolbar);
      commands_.hideToolbar().setVisible(showToolbar);
   }

   private void cleanupWorkbench()
   {
      server_.disconnect();

      satelliteManager_.closeAllSatellites();

      if (clientStateUpdaterInstance_ != null)
      {
         clientStateUpdaterInstance_.suspend();
         clientStateUpdaterInstance_ = null;
      }
   }

   private void navigateToSignIn()
   {
      navigateWindowTo("auth-sign-in");
   }

   private void removeTerminalCommands()
   {
      commands_.newTerminal().remove();
      commands_.activateTerminal().remove();
      commands_.renameTerminal().remove();
      commands_.closeTerminal().remove();
      commands_.clearTerminalScrollbackBuffer().remove();
      commands_.previousTerminal().remove();
      commands_.nextTerminal().remove();
      commands_.showTerminalInfo().remove();
      commands_.interruptTerminal().remove();
      commands_.sendTerminalToEditor().remove();
      commands_.sendToTerminal().remove();
      commands_.showTerminalOptions().remove();
      commands_.openNewTerminalAtEditorLocation().remove();
      commands_.sendFilenameToTerminal().remove();
      commands_.openNewTerminalAtFilePaneLocation().remove();
      commands_.setTerminalToCurrentDirectory().remove();
      commands_.closeAllTerminals().remove();
   }

   private void removeProjectCommands()
   {
      commands_.openProject().remove();
      commands_.newProject().remove();
      commands_.closeProject().remove();
      commands_.openProjectInNewWindow().remove();
      commands_.clearRecentProjects().remove();
      commands_.quitSession().remove();
      commands_.projectMru0().remove();
      commands_.projectMru1().remove();
      commands_.projectMru2().remove();
      commands_.projectMru3().remove();
      commands_.projectMru4().remove();
      commands_.projectMru5().remove();
      commands_.projectMru6().remove();
      commands_.projectMru7().remove();
      commands_.projectMru8().remove();
      commands_.projectMru9().remove();
      commands_.projectMru10().remove();
      commands_.projectMru11().remove();
      commands_.projectMru12().remove();
      commands_.projectMru13().remove();
      commands_.projectMru14().remove();
    }

   private void removeJobLauncherCommands()
   {
      // we will not remove the launcher commands if we have session servers defined
      Command removeCommands = () ->
      {
         commands_.startLauncherJob().remove();
         commands_.sourceAsLauncherJob().remove();
         commands_.runSelectionAsLauncherJob().remove();
         commands_.activateLauncherJobs().remove();
         commands_.sortLauncherJobsRecorded().remove();
         commands_.sortLauncherJobsState().remove();
      };

      if (Desktop.hasDesktopFrame())
      {
         Desktop.getFrame().getSessionServers(servers ->
         {
            if (servers.length() == 0)
            {
               removeCommands.execute();
            }
         });
      }
      else
      {
         removeCommands.execute();
      }
   }

   private void pauseClientStateUpdater()
   {
      if (!Desktop.isDesktop() && clientStateUpdaterInstance_ != null)
         clientStateUpdaterInstance_.pauseSendingUpdates();
   }

   private void resumeClientStateUpdater()
   {
      if (!Desktop.isDesktop() && clientStateUpdaterInstance_ != null)
         clientStateUpdaterInstance_.resumeSendingUpdates();
   }

   private final ApplicationView view_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   private final Session session_;
   private final Commands commands_;
   private final SatelliteManager satelliteManager_;
   private final Provider<ClientStateUpdater> clientStateUpdater_;
   private final Server server_;
   private final SessionOpener sessionOpener_;
   private final Provider<UserPrefs> userPrefs_;
   private final Provider<UserState> userState_;
   private final Provider<Workbench> workbench_;
   private final Provider<EventBus> eventBusProvider_;
   private final Provider<ApplicationClientInit> pClientInit_;
   private final Provider<ApplicationQuit> pApplicationQuit_;
   private final Provider<ApplicationInterrupt> pApplicationInterrupt_;
   private final Provider<ProductEditionInfo> pEdition_;
   private final Provider<ApplicationThemes> pAppThemes_;

   private boolean fileUploadInProgress_ = false;

   private final String CSRF_TOKEN_FIELD = "csrf-token";

   private ClientStateUpdater clientStateUpdaterInstance_;
   private RootLayoutPanel rootPanel_;
}
