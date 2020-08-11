/*
 * DesktopApplicationHeader.java
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
package org.rstudio.studio.client.application.ui.impl;

import java.util.ArrayList;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationQuit.QuitContext;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopHooks;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.application.IgnoredUpdates;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.LogoutRequestedEvent;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.UpdateCheckResult;
import org.rstudio.studio.client.application.ui.ApplicationHeader;
import org.rstudio.studio.client.application.ui.GlobalToolbar;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.debugging.ErrorManager;
import org.rstudio.studio.client.events.EditEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderEvent;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
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
      session_ = session;
      eventBus_= events;
      pUIPrefs_ = pUIPrefs;
      globalDisplay_ = globalDisplay;
      ignoredUpdates_ = IgnoredUpdates.create();
      server_ = server;
      appQuit_ = appQuit;
      binder_.bind(commands, this);
      overlay_ = new WebApplicationHeaderOverlay();

      commands.mainMenu(new DesktopMenuCallback());

      pDesktopHooks.get();

      if (!Desktop.isRemoteDesktop())
      {
         commands.uploadFile().remove();
         commands.exportFiles().remove();
      }
      commands.updateCredentials().remove();

      commands.checkForUpdates().setVisible(true);
      commands.showLogFiles().setVisible(true);
      commands.diagnosticsReport().setVisible(true);
      commands.showFolder().setVisible(true);

      events.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         final SessionInfo sessionInfo = session.getSessionInfo();

         isFlatTheme_ = RStudioThemes.isFlat(pUIPrefs_.get());

         if (Desktop.isRemoteDesktop())
            addSignoutToolbar();

         overlay_.addConnectionStatusToolbar(DesktopApplicationHeader.this);

         toolbar_.completeInitialization(sessionInfo);

         if (Desktop.isRemoteDesktop())
         {
            overlay_.addRVersionsToolbar(DesktopApplicationHeader.this);
            overlay_.addSessionsToolbar(DesktopApplicationHeader.this);
            addQuitSessionButton(commands);
         }

         new JSObjectStateValue(
               "updates",
               "ignoredUpdates",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false)
         {
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
         };

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

      events.addHandler(ShowFolderEvent.TYPE, new ShowFolderHandler()
      {
         public void onShowFolder(ShowFolderEvent event)
         {
            Desktop.getFrame().showFolder(StringUtil.notNull(event.getPath().getPath()));
         }
      });

      toolbar_ = new GlobalToolbar(commands, pCodeSearch);
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      toolbar_.getWrapper().addStyleName(styles.desktopGlobalToolbarWrapper());
      toolbar_.addStyleName(styles.desktopGlobalToolbar());
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

   private void addSignoutToolbar()
   {

      if (session_.getSessionInfo().getShowIdentity() && session_.getSessionInfo().getAllowFullUI())
      {
         String userIdentity = session_.getSessionInfo().getUserIdentity();
         ToolbarLabel usernameLabel = new ToolbarLabel();
         usernameLabel.setTitle(userIdentity);
         userIdentity = userIdentity.split("@")[0];
         usernameLabel.setText(userIdentity);

         addRightCommand(usernameLabel);

         ToolbarButton signOutButton = new ToolbarButton(
               ToolbarButton.NoText,
               "Sign out",
               new ImageResource2x(RESOURCES.signOut2x()),
               event -> eventBus_.fireEvent(new LogoutRequestedEvent()));


         addRightCommand(signOutButton);
         addRightCommandSeparator();
      }
   }

   private void addQuitSessionButton(Commands commands)
   {
      if (session_.getSessionInfo().getAllowFullUI())
      {
         addRightCommandSeparator();
         addRightCommand(commands.quitSession().createToolbarButton());
      }

   }

   interface Resources extends ClientBundle
   {
      @Source("signOut_2x.png")
      ImageResource signOut2x();
   }

   private static final DesktopApplicationHeader.Resources RESOURCES =  (DesktopApplicationHeader.Resources) GWT.create(DesktopApplicationHeader.Resources.class);

   @Handler
   void onUndoDummy()
   {
      Desktop.getFrame().undo();
   }

   @Handler
   void onRedoDummy()
   {
      Desktop.getFrame().redo();
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
               "Error Opening Devtools",
               "The Chromium devtools server could not be activated.");
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
         return isFlatTheme_ ? 29 : 32;
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
            globalDisplay_.showErrorMessage("Error Checking for Updates",
                  "An error occurred while checking for updates: "
                  + error.getMessage());
         }
      });
   }

   private void respondToUpdateCheck(final UpdateCheckResult result,
                                     boolean manual)
   {
      boolean ignoredUpdate = false;
      if (result.getUpdateVersion().length() > 0)
      {
         JsArrayString ignoredUpdates = ignoredUpdates_.getIgnoredUpdates();
         for (int i = 0; i < ignoredUpdates.length(); i++)
         {
            if (ignoredUpdates.get(i) == result.getUpdateVersion())
            {
               ignoredUpdate = true;
            }
         }
      }
      if (result.getUpdateVersion().length() > 0 &&
          !ignoredUpdate)
      {
         ArrayList<String> buttonLabels = new ArrayList<>();
         ArrayList<String> elementIds = new ArrayList<>();
         ArrayList<Operation> buttonOperations = new ArrayList<>();

         buttonLabels.add("Quit and Download...");
         elementIds.add(ElementIds.DIALOG_YES_BUTTON);
         buttonOperations.add(new Operation() {
            @Override
            public void execute()
            {
               appQuit_.prepareForQuit("Update RStudio", new QuitContext()
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

         buttonLabels.add("Remind Later");
         elementIds.add(ElementIds.DIALOG_NO_BUTTON);
         buttonOperations.add(new Operation() {
            @Override
            public void execute()
            {
               // Don't do anything here; the prompt will re-appear the next
               // time we do an update check
            }
         });

         // Only provide the option to ignore the update if it's not urgent.
         if (result.getUpdateUrgency() == 0)
         {
            buttonLabels.add("Ignore Update");
            elementIds.add(ElementIds.DIALOG_CANCEL_BUTTON);
            buttonOperations.add(new Operation() {
               @Override
               public void execute()
               {
                  ignoredUpdates_.addIgnoredUpdate(result.getUpdateVersion());
                  ignoredUpdatesDirty_ = true;
               }
            });
         }

         globalDisplay_.showGenericDialog(GlobalDisplay.MSG_QUESTION,
               "Update Available",
               result.getUpdateMessage(),
               buttonLabels,
               elementIds,
               buttonOperations, 0);
      }
      else if (manual)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                              "No Update Available",
                              "You're using the newest version of RStudio.");
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

   @SuppressWarnings("unused")
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

   public interface Binder
         extends CommandBinder<Commands, DesktopApplicationHeader>
   {
   }

   private static Binder binder_ = GWT.create(Binder.class);
   private Session session_;
   private EventBus eventBus_;
   private GlobalToolbar toolbar_;
   private GlobalDisplay globalDisplay_;
   Provider<UserPrefs> pUIPrefs_;
   private ApplicationServerOperations server_;
   private IgnoredUpdates ignoredUpdates_;
   private boolean ignoredUpdatesDirty_ = false;
   private ApplicationQuit appQuit_;
   private Boolean isFlatTheme_ = false;
   private WebApplicationHeaderOverlay overlay_;
}
