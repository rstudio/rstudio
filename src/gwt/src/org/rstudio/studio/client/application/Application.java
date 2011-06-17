/*
 * Application.java
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

package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.*;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.application.ui.RequestLogVisualization;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.ClientStateUpdater;
import org.rstudio.studio.client.workbench.Workbench;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Agreement;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

@Singleton
public class Application implements ApplicationEventHandlers,
                                    UncaughtExceptionHandler
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
                      WorkbenchContext workbenchContext,
                      SourceShim sourceShim,
                      Provider<Workbench> workbench,
                      Provider<EventBus> eventBusProvider,
                      Provider<ClientStateUpdater> clientStateUpdater,
                      Provider<ApplicationClientInit> pClientInit,
                      Provider<AceThemes> pAceThemes)
   {
      // save references
      view_ = view ;
      globalDisplay_ = globalDisplay;
      events_ = events;
      session_ = session;
      workbenchContext_ = workbenchContext;
      sourceShim_ = sourceShim;
      commands_ = commands;
      clientStateUpdater_ = clientStateUpdater;
      server_ = server;
      workbench_ = workbench;
      eventBusProvider_ = eventBusProvider;
      pClientInit_ = pClientInit;
      pAceThemes_ = pAceThemes;

      // bind to commands
      binder.bind(commands_, this);
      
      // subscribe to events
      events.addHandler(LogoutRequestedEvent.TYPE, this);
      events.addHandler(UnauthorizedEvent.TYPE, this);
      events.addHandler(QuitEvent.TYPE, this);
      events.addHandler(SuicideEvent.TYPE, this);
      events.addHandler(SessionAbendWarningEvent.TYPE, this);    
      events.addHandler(SessionSerializationEvent.TYPE, this);
      events.addHandler(ServerUnavailableEvent.TYPE, this);
      events.addHandler(InvalidClientVersionEvent.TYPE, this);
      events.addHandler(ServerOfflineEvent.TYPE, this);
      events.addHandler(SaveActionChangedEvent.TYPE, this);
      
      // set uncaught exception handler (first save default so we can call it)
      defaultUncaughtExceptionHandler_ = GWT.getUncaughtExceptionHandler();
      GWT.setUncaughtExceptionHandler(this);
   }
  
   
   public void go(RootLayoutPanel rootPanel, final Command dismissLoadingProgress)
   {
      Widget w = view_.getWidget();
      rootPanel.add(w);
      rootPanel.setWidgetTopBottom(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
      rootPanel.setWidgetLeftRight(w, 0, Style.Unit.PX, 0, Style.Unit.PX);

      // attempt init
      pClientInit_.get().execute(
                              new ServerRequestCallback<SessionInfo>() {

         public void onResponseReceived(final SessionInfo sessionInfo)
         {
            // initialize workbench after verifying agreement
            verifyAgreement(sessionInfo, new Operation() {
               public void execute()
               {
                  dismissLoadingProgress.execute();

                  session_.setSessionInfo(sessionInfo);

                  // configure workbench
                  initializeWorkbench();
               }
            }); 
         }

         public void onError(ServerError error)
         {
            Debug.logError(error);
            dismissLoadingProgress.execute();

            globalDisplay_.showErrorMessage("RStudio Initialization Error",
                                            error.getUserMessage());
         }
      }) ;
   }
   
   
   public void onUnauthorized(UnauthorizedEvent event)
   {
      navigateToSignIn();
   }   
   
   public void onServerOffline(ServerOfflineEvent event)
   {
      view_.showApplicationOffline();
   }
    
   public void onLogoutRequested(LogoutRequestedEvent event)
   {
      navigateWindowTo("auth-sign-out");
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
   public void onHelpKeyboardShortcuts()
   {
      openApplicationURL("docs/keyboard.htm");
   }
   
   private void showAgreement()
   {
      globalDisplay_.openWindow(server_.getApplicationURL("agreement"));
   }
   
   @Handler
   public void onRstudioSupport()
   {
      globalDisplay_.openRStudioLink("support");
   }
   
   @Handler
   public void onRstudioLicense()
   {
      showAgreement();
   }
   
   @Handler
   public void onRstudioAgreement()
   {
      showAgreement();
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
  
   // implementatin of quit session
   private Command doQuitSession_ = new Command()
   {
      public void execute()
      {
         if (Desktop.isDesktop())
         {
            Desktop.getFrame().close();
         }
         else
         {
            // quit session operation paramaterized by whether we save changes
            class QuitOperation implements ProgressOperation
            {
               QuitOperation(boolean saveChanges)
               {
                  saveChanges_ = saveChanges;
               }
               public void execute(ProgressIndicator indicator)
               {
                  indicator.onProgress("Quitting R Session...");
                  server_.quitSession(saveChanges_,
                                      new VoidServerRequestCallback(indicator));
               }
               private final boolean saveChanges_ ;
            }

            if (saveAction_.getAction() == SaveAction.SAVEASK) 
            {    
               // confirm quit and do it
               String prompt = "Save workspace image to " + 
                               workbenchContext_.getREnvironmentPath() + "?";
               globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
                                               "Quit R Session",
                                               prompt,
                                               true,
                                               new QuitOperation(true),
                                               new QuitOperation(false),
                                               true);
            }
            else
            {
               // do the quit without prompting 
               
               ProgressIndicator indicator =
                  globalDisplay_.getProgressIndicator("Error Quitting R");

               boolean save = saveAction_.getAction() == SaveAction.SAVE;
               new QuitOperation(save).execute(indicator);
            }
         } 
      } 
   };
   
   @Handler
   public void onQuitSession()
   {
      sourceShim_.saveChangesBeforeQuit(doQuitSession_);
   }

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
            final RequestLogVisualization viz = new RequestLogVisualization(
                  server_);
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
  
   public void onSaveActionChanged(SaveActionChangedEvent event)
   {
      saveAction_ = event.getAction();
   }
   
   
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
   
   public void onServerUnavailable(ServerUnavailableEvent event)
   {
      view_.hideSerializationProgress();
   }
   
   public void onQuit(QuitEvent event)
   {
      cleanupWorkbench();  
      
      // only show the quit state in server mode (in desktop mode the
      // window will close)
      if (!Desktop.isDesktop())
         view_.showApplicationQuit();
   }
   
   public void onSuicide(SuicideEvent event)
   { 
      cleanupWorkbench();
      view_.showApplicationSuicide(event.getMessage());
   }
   
   public void onClientDisconnected(ClientDisconnectedEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationDisconnected();
   }
   
   public void onInvalidClientVersion(InvalidClientVersionEvent event)
   {
      cleanupWorkbench();
      view_.showApplicationUpdateRequired();
   }

   public void onSessionAbendWarning(SessionAbendWarningEvent event)
   {
      view_.showSessionAbendWarning();
   }
   
   public void onUncaughtException(Throwable e)
   {     
      try
      {
         // call the default handler if there is one
         if (defaultUncaughtExceptionHandler_ != null)
            defaultUncaughtExceptionHandler_.onUncaughtException(e);
         
         // NOTE: we use use | as the logical line delimiter because server log
         // entries cannont contain newlines)
         
         // uncaught exception
         StringBuilder message = new StringBuilder();
         message.append("Uncaught Exception: ");

         CsvWriter csv = new CsvWriter();
         csv.writeValue(GWT.getPermutationStrongName());
         csv.writeValue(e.toString());

         StringBuilder stackTrace = new StringBuilder();
         writeStackTrace(e, stackTrace, false);

         csv.writeValue(stackTrace.toString());

         message.append(csv.getValue());
         
         // log to server
         server_.log(LogEntryType.ERROR, 
                     message.toString(),
                     new VoidServerRequestCallback());
      }
      catch(Throwable throwable)
      {
         // make sure exceptions never escape the uncaught handler
      }
   }

   private void writeStackTrace(Throwable e,
                                StringBuilder stackTrace,
                                boolean includeMessage)
   {
      if (e == null)
         return;

      if (includeMessage)
         stackTrace.append("\n").append(e.toString()).append("\n");

      // stack frame
      StackTraceElement[] stack = e.getStackTrace();
      if (stack != null)
      {
         for (int i=0; i<stack.length; i++)
         {
            if (i > 0)
               stackTrace.append("\n");
            stackTrace.append("    at ");
            stackTrace.append(stack[i].toString());
         }
      }

      if (e instanceof UmbrellaException)
      {
         UmbrellaException ue = (UmbrellaException)e;
         for (Throwable t : ue.getCauses())
            writeStackTrace(t, stackTrace, true);
      }
   }


   private void verifyAgreement(SessionInfo sessionInfo,
                              final Operation verifiedOperation)
   {
      // get the agreeeent (if any)
      final Agreement agreement = sessionInfo.pendingAgreement();
      
      // if there is an agreement then prompt user for agreement (otherwise just
      // execute the verifiedOperation immediately)
      if (agreement != null)
      {
         // append updated to the title if necessary
         String title = agreement.getTitle();
         if (agreement.getUpdated())
            title += " (Updated)";
         
         view_.showApplicationAgreement(
            
            // title and contents   
            title,
            agreement.getContents(),
             
            // bail to sign in page if the user doesn't confirm
            new Operation()
            {
               public void execute()
               {
                  if (Desktop.isDesktop())
                  {
                     server_.quitSession(false,
                                         new SimpleRequestCallback<Void>());
                  }
                  else
                     navigateToSignIn();
               }
            },
        
            // user confirmed
            new Operation() {
               public void execute()
               {
                  // call verified operation
                  verifiedOperation.execute();
                  
                  // record agreement on server
                  server_.acceptAgreement(agreement, 
                                          new VoidServerRequestCallback());
               } 
            }
            
         );
         
      }
      else
      {
         // no agreement pending
         verifiedOperation.execute();
      }
   }
   
   
   private void navigateWindowTo(String relativeUrl)
   {
      String url = GWT.getHostPageBaseURL() + relativeUrl;
      Window.Location.replace(url);
   }
   
   private void openApplicationURL(String relativeURL)
   {
      String url = GWT.getHostPageBaseURL() + relativeURL;
      globalDisplay_.openWindow(url);
   }
   
   private void initializeWorkbench()
   {
      pAceThemes_.get();

      // subscribe to ClientDisconnected event (wait to do this until here
      // because there were spurious ClientDisconnected events occuring
      // after a session interrupt sequence. we couldn't figure out why,
      // and since this is a temporary hack why not add another temporary
      // hack to go with it here :-)
      // TOOD: move this back tot he constructor after we revise the
      // interrupt hack(s)
      events_.addHandler(ClientDisconnectedEvent.TYPE, this); 
      
      // create workbench
      Workbench wb = workbench_.get();
      eventBusProvider_.get().fireEvent(new SessionInitEvent()) ;

      // hide the agreement menu item if we don't have one
      if (!session_.getSessionInfo().hasAgreement())
         commands_.rstudioAgreement().setVisible(false);
      
      // show workbench
      view_.showWorkbenchView(wb.getMainView().toWidget());

      clientStateUpdaterInstance_ = clientStateUpdater_.get();
   }
      
   private void cleanupWorkbench()
   {
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
  
   private final ApplicationView view_ ;
   private final GlobalDisplay globalDisplay_ ;
   private final EventBus events_;
   private final Session session_;
   private final WorkbenchContext workbenchContext_;
   private final SourceShim sourceShim_;
   private final Commands commands_;
   private final Provider<ClientStateUpdater> clientStateUpdater_;
   private final Server server_;
   private final Provider<Workbench> workbench_;
   private final Provider<EventBus> eventBusProvider_;
   private final Provider<ApplicationClientInit> pClientInit_;
   private final Provider<AceThemes> pAceThemes_;

   private ClientStateUpdater clientStateUpdaterInstance_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
   
   private final UncaughtExceptionHandler defaultUncaughtExceptionHandler_ ;
}
