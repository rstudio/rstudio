/*
 * DesktopApplicationHeader.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui.impl;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationQuit.QuitContext;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopHooks;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.application.IgnoredUpdates;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.events.DesktopMouseNavigateEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.UpdateCheckResult;
import org.rstudio.studio.client.application.ui.ApplicationHeader;
import org.rstudio.studio.client.application.ui.GlobalToolbar;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.debugging.ErrorManager;
import org.rstudio.studio.client.events.EditEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.PushClientStateEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class DesktopApplicationHeader implements ApplicationHeader,
                                      WebApplicationHeaderOverlay.Context
{
   public DesktopApplicationHeader()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(final Commands commands,
                          EventBus events,
                          final Session session,
                          ApplicationServerOperations server,
                          Provider<DesktopHooks> pDesktopHooks,
                          Provider<CodeSearch> pCodeSearch,
                          Provider<UserPrefs> pUIPrefs,
                          ErrorManager errorManager,
                          GlobalDisplay globalDisplay,
                          ApplicationQuit appQuit)
   {
      commands_ = commands;
      session_ = session;
      eventBus_= events;
      pUIPrefs_ = pUIPrefs;
      globalDisplay_ = globalDisplay;
      server_ = server;
      appQuit_ = appQuit;
      binder_.bind(commands, this);
      overlay_ = new WebApplicationHeaderOverlay();

      commands.mainMenu(new DesktopMenuCallback());

      pDesktopHooks.get();

      commands.uploadFile().remove();
      commands.exportFiles().remove();
      commands.updateCredentials().remove();

      commands.checkForUpdates().setVisible(true);
      commands.showLogFiles().setVisible(true);
      commands.diagnosticsReport().setVisible(true);
      commands.showFolder().setVisible(true);
      
      if (BrowseCap.isElectron())
      {
         // we use a preview listener because we need to ensure the event
         // is handled before anything else in the IDE surface might try
         // to handle the mouse event
         addBackForwardMouseDownHandlers();
      }
      else
      {
         commands.showA11yDiagnostics().remove();
      }
      
      events.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         final SessionInfo sessionInfo = session.getSessionInfo();

         toolbar_.completeInitialization(sessionInfo);

         ignoredUpdatesState_ = new IgnoredUpdatesStateValue(sessionInfo.getClientState());

         Scheduler.get().scheduleFinally(new ScheduledCommand()
         {
            public void execute()
            {
               Desktop.getFrame().onWorkbenchInitialized(
                     StringUtil.notNull(sessionInfo.getScratchDir()));

               if (sessionInfo.getDisableCheckForUpdates())
                  commands.checkForUpdates().remove();

               if (!sessionInfo.getDisableCheckForUpdates() &&
                   pUIPrefs_.get().checkForUpdates().getValue())
               {
                  checkForUpdates(false);
               }
            }
         });
      });

      if (BrowseCap.isMacintoshDesktop() && BrowseCap.isElectron()) {
         Desktop.getFrame().detectRosetta();
      }

      events.addHandler(ShowFolderEvent.TYPE, new ShowFolderEvent.Handler()
      {
         public void onShowFolder(ShowFolderEvent event)
         {
            Desktop.getFrame().showFolder(StringUtil.notNull(event.getPath().getPath()));
         }
      });

      toolbar_ = new GlobalToolbar(commands, pCodeSearch);
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      toolbar_.getWrapper().addStyleName(styles.desktopGlobalToolbarWrapper());
   }

   private class IgnoredUpdatesStateValue extends JSObjectStateValue
   {
      public IgnoredUpdatesStateValue(ClientInitState clientState)
      {
         super("updates",
               "ignoredUpdates",
               ClientState.PERSISTENT,
               clientState,
               false);
         finishInit(clientState);
      }

      @Override
      protected void onInit(JsObject value)
      {
         if (value != null)
            ignoredUpdates_ = value.cast();
      }

      @Override
      protected JsObject getValue()
      {
         ignoredUpdatesDirty_ = false;
         return ignoredUpdates_.cast();
      }

      @Override
      protected boolean hasChanged()
      {
         return ignoredUpdatesDirty_;
      }

      public JsArrayString getIgnoredUpdates()
      {
         return ignoredUpdates_.getIgnoredUpdates();
      }

      public void addIgnoredUpdate(String update) {
         ignoredUpdates_.addIgnoredUpdate(update);
         ignoredUpdatesDirty_ = true;
      }

      public void removeIgnoredUpdates() {
         ignoredUpdates_.removeIgnoredUpdates();
         ignoredUpdatesDirty_ = true;
      }

      private IgnoredUpdates ignoredUpdates_ = IgnoredUpdates.create();
      private boolean ignoredUpdatesDirty_ = false;
   }

   @Override
   public void showToolbar(boolean showToolbar)
   {
      toolbar_.setVisible(showToolbar);
   }

   @Override
   public boolean isToolbarVisible()
   {
      return toolbar_.isVisible();
   }

   @Override
   public void focusToolbar()
   {
      toolbar_.setFocus();
   }

   @Override
   public void focusGoToFunction()
   {
      toolbar_.focusGoToFunction();
   }

   private void fireEditEvent(final int type)
   {
      eventBus_.fireEvent(new EditEvent(true, type));
   }

   interface Resources extends ClientBundle
   {
      @Source("signOut_2x.png")
      ImageResource signOut2x();
   }

   private static final DesktopApplicationHeader.Resources RESOURCES =  (DesktopApplicationHeader.Resources) GWT.create(DesktopApplicationHeader.Resources.class);

   private void undoAce() {
      // Undo on the ACE editor has to be called manually since Electron cannot trigger it
      AceEditorNative editorNative = AceEditorNative.getEditor(DomUtils.getActiveElement());
      if (editorNative != null) {
         editorNative.execCommand("undo");
      } else {
         Desktop.getFrame().undo();
      }
   }

   private void redoAce() {
      // Redo on the ACE editor has to be called manually since Electron cannot trigger it
      AceEditorNative editorNative = AceEditorNative.getEditor(DomUtils.getActiveElement());
      if (editorNative != null) {
         editorNative.execCommand("redo");
      } else {
         Desktop.getFrame().redo();
      }
   }

   @Handler
   void onUndoDummy() {
      if (BrowseCap.isElectron()) {
         undoAce();
      } else {
         Desktop.getFrame().undo();
      }
   }

   @Handler
   void onRedoDummy()
   {
      if (BrowseCap.isElectron()) {
         redoAce();
      } else {
         Desktop.getFrame().redo();
      }
   }

   @Handler
   void onCutDummy()
   {
      if (isSelectionEmpty()) return;
      fireEditEvent(EditEvent.TYPE_CUT);
      Desktop.getFrame().clipboardCut();
   }

   @Handler
   void onCopyDummy()
   {
      if (isSelectionEmpty()) return;
      fireEditEvent(EditEvent.TYPE_COPY);
      Desktop.getFrame().clipboardCopy();
   }

   @Handler
   void onPasteDummy()
   {
      fireEditEvent(EditEvent.TYPE_PASTE);
      Desktop.getFrame().clipboardPaste();
   }

   @Handler
   void onPasteWithIndentDummy()
   {
      fireEditEvent(EditEvent.TYPE_PASTE_WITH_INDENT);
      Desktop.getFrame().clipboardPaste();
   }

   @Handler
   void onShowLogFiles()
   {
      Desktop.getFrame().showFolder(StringUtil.notNull(session_.getSessionInfo().getLogDir()));
   }

   @Handler
   void onDiagnosticsReport()
   {
      eventBus_.fireEvent(
         new SendToConsoleEvent("rstudioDiagnosticsReport()", true));

      new Timer() {
         @Override
         public void run()
         {
            Desktop.getFrame().showFolder("~/rstudio-diagnostics");
         }
      }.schedule(1000);

   }

   @Handler
   void onOpenDeveloperConsole()
   {
      int port = DesktopInfo.getChromiumDevtoolsPort();
      if (port == 0)
      {
         globalDisplay_.showErrorMessage(
               constants_.errorOpeningDevToolsCaption(),
               constants_.cannotActivateDevtoolsMessage());
      }
      else
      {
         globalDisplay_.openMinimalWindow(
               ("http://127.0.0.1:" + DesktopInfo.getChromiumDevtoolsPort()),
               Window.getClientWidth() - 20,
               Window.getClientHeight() - 20);
      }
   }

   @Handler
   void onShowGpuDiagnostics()
   {
      globalDisplay_.openMinimalWindow("chrome://gpu", 500, 400);
   }

   @Handler
   void onShowA11yDiagnostics()
   {
      globalDisplay_.openMinimalWindow("chrome://accessibility", 500, 400);
   }

   @Handler
   void onReloadUi()
   {
      WindowEx.get().reload();
   }

   @Handler
   void onCheckForUpdates()
   {
      checkForUpdates(true);
   }

   public int getPreferredHeight()
   {
      if (toolbar_.isVisible())
         return 29;
      else
         return 5;
   }

   public Widget asWidget()
   {
      return toolbar_;
   }

   private void checkForUpdates(final boolean manual)
   {
      server_.checkForUpdates(manual,
            new ServerRequestCallback<UpdateCheckResult>()
      {
         @Override
         public void onResponseReceived(UpdateCheckResult result)
         {
            respondToUpdateCheck(result, manual);
         }

         @Override
         public void onError(ServerError error)
         {
            // Only show the error message when manually checking for updates 
            if (manual)
            {
               globalDisplay_.showErrorMessage(constants_.errorCheckingUpdatesMessage(),
                     constants_.errorOccurredCheckingUpdatesMessage()
                     + error.getMessage()
                     + "\n\n"
                     + constants_.visitWebsiteForNewVersionText());
            }
         }
      });
   }

   private void respondToUpdateCheck(final UpdateCheckResult result,
                                     boolean manual)
   {
      boolean ignoredUpdate = false;
      String updateVersion = result.getUpdateVersion();
      boolean updateAvailable = updateVersion.length() > 0;
      if (updateAvailable)
      {
         JsArrayString ignoredUpdates = ignoredUpdatesState_.getIgnoredUpdates();
         for (int i = 0; i < ignoredUpdates.length(); i++)
         {
            if (ignoredUpdates.get(i) == updateVersion)
            {
               ignoredUpdate = true;
            }
         }
      }
      // Show dialog if there's an update available and either:
      // 1) The user is manually checking for this update (whether the update
      //    was previously ignored doesn't matter); or
      // 2) This is an automatic update check and the version wasn't previously
      //    ignored
      if (updateAvailable && (manual || !ignoredUpdate))
      {
         ArrayList<String> buttonLabels = new ArrayList<>();
         ArrayList<String> elementIds = new ArrayList<>();
         ArrayList<Operation> buttonOperations = new ArrayList<>();
         boolean isManualAndIgnored = manual && ignoredUpdate;

         buttonLabels.add(constants_.quitDownloadButtonLabel());
         elementIds.add(ElementIds.DIALOG_YES_BUTTON);
         buttonOperations.add(new Operation() {
            @Override
            public void execute()
            {
               appQuit_.prepareForQuit(constants_.updateRStudioCaption(), new QuitContext()
               {
                  @Override
                  public void onReadyToQuit(boolean saveChanges)
                  {
                     Desktop.getFrame().browseUrl(StringUtil.notNull(result.getUpdateUrl()));
                     appQuit_.performQuit(null, saveChanges);
                  }
               });
            }
         });

         // Only show "Remind Later" if the user isn't manually checking for an
         // update, which happens to be an ignored version. Essentially, it's not
         // possible for a user to un-ignore a version.
         if (!isManualAndIgnored) {
            buttonLabels.add(constants_.remindLaterButtonLabel());
            elementIds.add(ElementIds.DIALOG_NO_BUTTON);
            buttonOperations.add(new Operation() {
               @Override
               public void execute()
               {
                  // Don't do anything here; the prompt will re-appear the next
                  // time we do an update check
               }
            });
         }

         // If update has been ignored, give an option to stop ignoring it
         if (isManualAndIgnored)
         {
            buttonLabels.add(constants_.stopIgnoringUpdatesButtonLabel());
            elementIds.add(ElementIds.DIALOG_RETRY_BUTTON);
            buttonOperations.add(new Operation() {
               @Override
               public void execute()
               {
                  ignoredUpdatesState_.removeIgnoredUpdates();
                  // Trigger an update to the persistent updates state file
                  eventBus_.fireEvent(new PushClientStateEvent(true));

                  // Let user know what happened
                  globalDisplay_.showMessage(
                     MessageDialog.INFO,
                     constants_.autoUpdateReenabledCaption(),
                     constants_.autoUpdateReenabledMessage());
               }
            });
         }

         // Only provide the option to ignore the update if it's not urgent.
         if (result.getUpdateUrgency() == 0)
         {
            // We need to use a final variable here because we're using it in an
            // anonymous inner class
            final boolean finalIgnoredUpdate = ignoredUpdate;
            buttonLabels.add(constants_.ignoreUpdateButtonLabel());
            elementIds.add(ElementIds.DIALOG_CANCEL_BUTTON);
            buttonOperations.add(new Operation() {
               @Override
               public void execute()
               {
                  // only run the following code if we didn't already ignore the update
                  if (!finalIgnoredUpdate)
                  {
                     ignoredUpdatesState_.addIgnoredUpdate(result.getUpdateVersion());
                     // Trigger an update to the persistent updates state file
                     eventBus_.fireEvent(new PushClientStateEvent(true));
                  }
               }
            });
         }

         String updateMessage = isManualAndIgnored
               ? result.getUpdateMessage() + "\n\n" + constants_.updateDisabledForVersionText(updateVersion)
               : result.getUpdateMessage();

         globalDisplay_.showGenericDialog(GlobalDisplay.MSG_QUESTION,
               constants_.updateAvailableCaption(),
               updateMessage,
               buttonLabels,
               elementIds,
               buttonOperations, 0);
      }
      else if (manual)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                              constants_.noUpdateAvailableCaption(),
                              constants_.usingNewestVersionMessage());
      }
   }

   public static boolean isSelectionEmpty()
   {
      Element activeElement = DomUtils.getActiveElement();
      AceEditorNative editor = AceEditorNative.getEditor(activeElement);
      if (editor != null)
      {
         Selection selection = editor.getSession().getSelection();
         return selection.isEmpty();
      }

      // NOTE: we currently use this for managing copy + paste
      // behaviors, but copy + paste seems to do the right thing
      // regardless of whether the user has highlighted some text
      // or not _outside_ of an Ace instance, and we can't always
      // detect if the document has a selection (e.g. if an iframe
      // has focus)
      return false;
   }

   private static boolean isFocusInAceInstance()
   {
      Element focusElem = DomUtils.getActiveElement();
      if (focusElem != null)
         return focusElem.hasClassName("ace_text-input");
      return false;
   }

   @Override
   public void addCommand(Widget widget)
   {
      toolbar_.addRightWidget(widget);
   }

   @Override
   public Widget addCommandSeparator()
   {
      return toolbar_.addRightSeparator();
   }

   @Override
   public void addLeftCommand(Widget widget)
   {
      toolbar_.addLeftWidget(widget);
   }

   @Override
   public void addLeftCommand(Widget widget, String width)
   {
      toolbar_.addLeftWidget(widget);
   }

   @Override
   public void addRightCommand(Widget widget)
   {
      toolbar_.addRightWidget(widget);
   }

   @Override
   public Widget addRightCommandSeparator()
   {
      return toolbar_.addRightSeparator();
   }

   @Override
   public void addProjectCommand(Widget widget)
   {

   }

   @Override
   public Widget addProjectCommandSeparator()
   {
      return null;
   }

   @Override
   public void addProjectRightCommand(Widget widget)
   {
      toolbar_.addRightWidget(widget);
   }

   @Override
   public Widget addProjectRightCommandSeparator()
   {
      return toolbar_.addRightSeparator();
   }

   @Override
   public void addUserCommand(Widget widget)
   {

   }

   @Override
   public AppMenuBar getMainMenu()
   {
      return null;
   }
   
   private void onMouseForward(NativeEvent event)
   {
      eventBus_.fireEvent(new DesktopMouseNavigateEvent(true, event.getClientX(), event.getClientY()));
   }
   
   private void onMouseBack(NativeEvent event)
   {
      eventBus_.fireEvent(new DesktopMouseNavigateEvent(false, event.getClientX(), event.getClientY()));
   }
   
   private final native void addBackForwardMouseDownHandlers()
   /*-{
      
      var self = this;
      var eventTarget = null;
      
      // Suppress 'mousedown' clicks from the back / forward mouse buttons.
      // Otherwise, they might send focus just before attempting navigation,
      // which is annoying if the mouse is within an editable text field
      // (e.g. an editor in the Source pane).
      $doc.body.addEventListener("mousedown", $entry(function(event) {
         
         var button = event.button;
         if (button === 3 || button == 4)
         {
            // Save the event target, so we can detect if 'mousedown' and 'mouseup'
            // happened over the same click target.
            eventTarget = event.target;
            
            // Suppress events otherwise.
            event.stopPropagation();
            event.preventDefault();
         }
         
      }), true);
      
      // Handle navigation attempts in 'mouseup'.
      $doc.body.addEventListener("mouseup", $entry(function(event) {
         
         // Get the event targets.
         var oldEventTarget = eventTarget;
         var newEventTarget = event.target;
         
         // Clear the cached event target.
         eventTarget = null;
         
         // If the event target changed, nothing to do.
         var eventsMatch = oldEventTarget === newEventTarget;
         
         // Check and handle mouse back / forward buttons.
         var button = event.button;
         if (button === 3)
         {
            event.stopPropagation();
            event.preventDefault();
            if (eventsMatch)
            {
               self.@org.rstudio.studio.client.application.ui.impl.DesktopApplicationHeader::onMouseBack(*)(event);
            }
         }
         else if (button === 4)
         {
            event.stopPropagation();
            event.preventDefault();
            if (eventsMatch)
            {
               self.@org.rstudio.studio.client.application.ui.impl.DesktopApplicationHeader::onMouseForward(*)(event);
            }
         }
         
      }), true);
      
   }-*/;
   
   public interface Binder
         extends CommandBinder<Commands, DesktopApplicationHeader>
   {
   }

   private static Binder binder_ = GWT.create(Binder.class);
   private Commands commands_;
   private Session session_;
   private EventBus eventBus_;
   private GlobalToolbar toolbar_;
   private GlobalDisplay globalDisplay_;
   Provider<UserPrefs> pUIPrefs_;
   private ApplicationServerOperations server_;
   private IgnoredUpdatesStateValue ignoredUpdatesState_;
   private ApplicationQuit appQuit_;
   private WebApplicationHeaderOverlay overlay_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
