/*
 * ChatPane.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.theme.ThemeColorExtractor;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.LocaleCookie;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ChatPane
      extends WorkbenchPane
      implements ChatPresenter.Display
{
   private enum ContentType
   {
      HTML,
      URL
   }

   @Inject
   protected ChatPane(GlobalDisplay globalDisplay,
                      EventBus events,
                      Commands commands,
                      Session session,
                      ChatServerOperations server)
   {
      super(constants_.chatTitle(), events);

      globalDisplay_ = globalDisplay;
      events_ = events;
      commands_ = commands;
      session_ = session;
      server_ = server;

      ensureWidget();

      // Hide empty toolbar until we have content to show
      setMainToolbarVisible(false);

      // Listen for theme changes to update iframe content
      events_.addHandler(ThemeChangedEvent.TYPE, new ThemeChangedEvent.Handler()
      {
         @Override
         public void onThemeChanged(ThemeChangedEvent event)
         {
            // Clear the color cache so fresh colors are extracted
            ThemeColorExtractor.clearCache();

            // Re-inject theme variables into current iframe content
            if (frame_ != null)
            {
               frame_.injectThemeVariables();
            }
         }
      });
   }

   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new LayoutPanel();
      mainPanel_.addStyleName("ace_editor_theme");

      // Create update notification bar
      updateNotificationPanel_ = new FlowPanel();
      updateNotificationPanel_.setVisible(false);
      updateNotificationPanel_.setStyleName(RES.styles().chatUpdateNotification());

      updateMessageLabel_ = new HTML();
      updateMessageLabel_.setStyleName(RES.styles().chatUpdateMessage());
      updateButtonPanel_ = new HorizontalPanel();
      updateButtonPanel_.getElement().getStyle().setMarginLeft(10, Unit.PX);

      HorizontalPanel notificationContent = new HorizontalPanel();
      notificationContent.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      notificationContent.add(updateMessageLabel_);
      notificationContent.add(updateButtonPanel_);
      updateNotificationPanel_.add(notificationContent);

      frame_ = new RStudioThemedFrame(constants_.chatTitle());
      frame_.setSize("100%", "100%");

      // Store initial message to show after frame loads
      pendingMessage_ = generateMessageHTML(constants_.checkingInstallationMessage());

      // Add update notification and frame to layout
      mainPanel_.add(updateNotificationPanel_);
      mainPanel_.add(frame_);

      // Position update notification at top
      mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, 40, Unit.PX);
      mainPanel_.setWidgetLeftRight(updateNotificationPanel_, 0, Unit.PX, 0, Unit.PX);

      // Position frame below notification (initially at 0 since notification is hidden)
      updateFrameLayout();

      return mainPanel_;
   }

   private void updateFrameLayout()
   {
      if (updateNotificationPanel_.isVisible())
      {
         // Notification is showing, frame starts below it
         mainPanel_.setWidgetTopBottom(frame_, 40, Unit.PX, 0, Unit.PX);
      }
      else
      {
         // Notification hidden, frame takes full height
         mainPanel_.setWidgetTopBottom(frame_, 0, Unit.PX, 0, Unit.PX);
      }
      mainPanel_.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);
   }

   /**
    * Generates centered HTML content for display in the iframe.
    *
    * @param message The message text to display
    * @return HTML string with proper styling for centered content
    */
   private String generateMessageHTML(String message)
   {
      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html lang='");
      html.append(LocaleCookie.getUiLanguage());
      html.append("'>");
      html.append("<head>");
      html.append("<meta charset='UTF-8'>");
      html.append("<style>");
      html.append("html, body {");
      html.append("  margin: 0;");
      html.append("  padding: 0;");
      html.append("  width: 100%;");
      html.append("  height: 100%;");
      html.append("  overflow: hidden;");
      html.append("}");
      html.append("body {");
      html.append("  display: flex;");
      html.append("  align-items: center;");
      html.append("  justify-content: center;");
      html.append("  font-family: ");
      html.append(ThemeFonts.getProportionalFont());
      html.append(";");
      html.append("  font-size: 12px;");
      html.append("  color: var(--rstudio-editor-foreground, #000);");
      html.append("  background-color: var(--rstudio-editor-background, #fff);");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 20px;");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append(message);
      html.append("</div>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   /**
    * Sets the HTML content of the iframe dynamically.
    *
    * @param frame The RStudioThemedFrame to update
    * @param html The HTML content to write to the iframe
    */
   private native void setFrameContent(RStudioThemedFrame frame, String html) /*-{
      try {
         var doc = frame.@org.rstudio.core.client.widget.RStudioFrame::getWindow()().document;
         doc.open();
         doc.write(html);
         doc.close();
      } catch (e) {
         console.error("Error setting frame content:", e);
      }
   }-*/;

   /**
    * Updates the iframe content and stores it for later refresh.
    *
    * @param html The HTML content to display
    */
   private void updateFrameContent(String html)
   {
      contentType_ = ContentType.HTML;
      currentContent_ = html;
      currentUrl_ = null;

      // Force a complete iframe reload by navigating to about:blank first
      // This kills the existing JavaScript context (stops WebSocket reconnection attempts)

      // Set up a one-time load handler to write content after about:blank loads
      setupOneShotLoadHandler(frame_, html);

      // Navigate to about:blank
      frame_.setUrl("about:blank");
   }

   private native void setupOneShotLoadHandler(RStudioThemedFrame frame, String html) /*-{
      var self = this;
      var iframe = frame.@org.rstudio.core.client.widget.RStudioFrame::getElement()();

      // Remove any existing pending handler
      if (iframe._rstudioPendingLoadHandler) {
         iframe.removeEventListener('load', iframe._rstudioPendingLoadHandler);
         console.log("ChatPane: Removed previous pending load handler");
      }

      var handler = function() {
         console.log("ChatPane: Load handler fired, writing content");
         // Remove this handler so it only fires once
         iframe.removeEventListener('load', handler);
         iframe._rstudioPendingLoadHandler = null;

         // Now write the content
         self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::setFrameContent(Lorg/rstudio/core/client/widget/RStudioThemedFrame;Ljava/lang/String;)(frame, html);

         // Inject theme variables after writing content
         self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::injectThemeVariablesDelayed(Lorg/rstudio/core/client/widget/RStudioThemedFrame;)(frame);
      };

      // Store reference so we can cancel it if needed
      iframe._rstudioPendingLoadHandler = handler;
      iframe.addEventListener('load', handler);
      console.log("ChatPane: Set up load handler, html length: " + html.length);
   }-*/;

   /**
    * Injects theme variables into the frame after a short delay.
    * This ensures the HTML content is fully parsed before variables are applied.
    * Uses 100ms delay here, followed by RStudioThemedFrame's 1000ms delay (1100ms total).
    */
   private void injectThemeVariablesDelayed(RStudioThemedFrame frame)
   {
      // Delay allows HTML to be parsed before theme variable injection
      Timers.singleShot(100, () -> {
         frame.injectThemeVariables();
      });
   }

   /**
    * Loads a URL in the iframe and stores it for later refresh.
    *
    * @param url The URL to load
    */
   @Override
   public void loadUrl(String url)
   {
      // Cancel any pending load handler from updateFrameContent() to prevent
      // it from overwriting the URL content when it fires
      cancelPendingLoadHandler(frame_);

      contentType_ = ContentType.URL;
      currentUrl_ = url;
      currentContent_ = null;
      frame_.setUrl(url);
   }

   private native void cancelPendingLoadHandler(RStudioThemedFrame frame) /*-{
      var iframe = frame.@org.rstudio.core.client.widget.RStudioFrame::getElement()();
      if (iframe._rstudioPendingLoadHandler) {
         iframe.removeEventListener('load', iframe._rstudioPendingLoadHandler);
         iframe._rstudioPendingLoadHandler = null;
         console.log("ChatPane: Cancelled pending load handler before loading URL");
      }
   }-*/;

   @Override
   public void setObserver(ChatPresenter.Display.Observer observer)
   {
      observer_ = observer;
   }

   @Override
   public void setUpdateObserver(ChatPresenter.Display.UpdateObserver observer)
   {
      updateObserver_ = observer;
   }

   @Override
   public void showUpdateNotification(String newVersion)
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateAvailable(newVersion));

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatUpdate(), () -> {
            if (updateObserver_ != null)
            {
               updateObserver_.onUpdateNow();
            }
         })
         .addButton(constants_.chatIgnore(), () -> {
            if (updateObserver_ != null)
            {
               updateObserver_.onRemindLater();
            }
         });

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showInstallNotification(String newVersion)
   {
      updateMessageLabel_.setHTML(constants_.chatInstallAvailable(newVersion));

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatInstallNow(), () -> {
            if (updateObserver_ != null)
            {
               updateObserver_.onUpdateNow();
            }
         })
         .addButton(constants_.chatIgnore(), () -> {
            if (updateObserver_ != null)
            {
               updateObserver_.onRemindLater();
            }
         });

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdatingStatus()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdating());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear();

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdateComplete()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateComplete());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear();

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdateError(String errorMessage)
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateFailed(errorMessage));

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatRetry(), () -> {
            if (updateObserver_ != null)
            {
               updateObserver_.onRetryUpdate();
            }
         })
         .addButton(constants_.chatDismiss(), () -> hideUpdateNotification());

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void hideUpdateNotification()
   {
      updateNotificationPanel_.setVisible(false);
      updateFrameLayout();
   }

   @Override
   public void showUpdateCheckFailure()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateCheckFailed());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatDismiss(), () -> hideUpdateNotification());

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showIncompatibleVersion()
   {
      String message = constants_.chatIncompatibleVersion();
      showMessage(message);
   }

   @Override
   public void setStatus(ChatPresenter.Display.Status status)
   {
      currentStatus_ = status;
      switch (status)
      {
         case STARTING:
            showMessage(constants_.startingChatMessage());
            break;
         case RESTARTING:
            showMessage(constants_.restartingChatMessage());
            break;
         case NOT_INSTALLED:
            showMessage(constants_.chatNotInstalledMessage());
            break;
         case ERROR:
            // Error message will be shown via showError()
            break;
         case READY:
            // UI is loaded, hide messages
            hideMessage();
            break;
         case ASSISTANT_NOT_SELECTED:
            updateFrameContent(generateMessageHTML(
               constants_.chatAssistantNotSelected()));
            break;
      }
   }

   @Override
   public void showError(String errorMessage)
   {
      showMessage(constants_.chatErrorPrefix(errorMessage));
   }

   private void showMessage(String message)
   {
      updateFrameContent(generateMessageHTML(message));
   }

   private void hideMessage()
   {
      // Content will be replaced by iframe loading
   }

   @Override
   public void showCrashedMessage(int exitCode)
   {
      String html = generateCrashedMessageHTML(exitCode);
      updateFrameContent(html);
   }

   @Override
   public void showSuspendedMessage()
   {
      String html = generateSuspendedMessageHTML();
      updateFrameContent(html);
   }

   private String generateSuspendedMessageHTML()
   {
      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html lang='");
      html.append(LocaleCookie.getUiLanguage());
      html.append("'>");
      html.append("<head>");
      html.append("<meta charset='UTF-8'>");
      html.append("<style>");
      html.append("html, body {");
      html.append("  margin: 0;");
      html.append("  padding: 0;");
      html.append("  width: 100%;");
      html.append("  height: 100%;");
      html.append("  overflow: hidden;");
      html.append("}");
      html.append("body {");
      html.append("  display: flex;");
      html.append("  align-items: center;");
      html.append("  justify-content: center;");
      html.append("  font-family: ");
      html.append(ThemeFonts.getProportionalFont());
      html.append(";");
      html.append("  color: var(--rstudio-editor-foreground, #333);");
      html.append("  background-color: var(--rstudio-editor-background, #fff);");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, #333);");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, #666);");
      html.append("  margin: 8px 0;");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append("<h2>");
      html.append(constants_.chatSessionSuspendedTitle());
      html.append("</h2>");
      html.append("<p>");
      html.append(constants_.chatSessionSuspendedMessage1());
      html.append("</p>");
      html.append("<p>");
      html.append(constants_.chatSessionSuspendedMessage2());
      html.append("</p>");
      html.append("</div>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   private String generateCrashedMessageHTML(int exitCode)
   {
      // Determine title and message based on exit code
      String title;
      String message;

      if (exitCode == 76) // EXIT_CODE_PROTOCOL_SERVER_TOO_OLD
      {
         title = constants_.chatUpdateRequiredTitle();
         message = constants_.chatRStudioTooOldMessage();
      }
      else if (exitCode == 77) // EXIT_CODE_PROTOCOL_CLIENT_TOO_OLD
      {
         title = constants_.chatUpdateRequiredTitle();
         message = constants_.chatAssistantTooOldMessage();
      }
      else
      {
         title = constants_.chatProcessExitedTitle();
         message = constants_.chatProcessExitedMessage();
      }

      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html lang='");
      html.append(LocaleCookie.getUiLanguage());
      html.append("'>");
      html.append("<head>");
      html.append("<meta charset='UTF-8'>");
      html.append("<style>");
      html.append("html, body {");
      html.append("  margin: 0;");
      html.append("  padding: 0;");
      html.append("  width: 100%;");
      html.append("  height: 100%;");
      html.append("  overflow: hidden;");
      html.append("}");
      html.append("body {");
      html.append("  display: flex;");
      html.append("  align-items: center;");
      html.append("  justify-content: center;");
      html.append("  font-family: ");
      html.append(ThemeFonts.getProportionalFont());
      html.append(";");
      html.append("  color: var(--rstudio-editor-foreground, #333);");
      html.append("  background-color: var(--rstudio-editor-background, #fff);");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, #333);");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, #666);");
      html.append("  margin: 0 0 24px 0;");
      html.append("}");
      html.append(".chatIframeButton {");
      html.append("  padding: 10px 20px;");
      html.append("  font-size: 14px;");
      html.append("  cursor: pointer;");
      html.append("  background-color: var(--rstudio-editorWidget-background, #f4f8f9);");
      html.append("  color: var(--rstudio-editor-foreground, #333);");
      html.append("  border: 1px solid var(--rstudio-panel-border, #d6dadc);");
      html.append("  border-radius: 4px;");
      html.append("}");
      html.append(".chatIframeButton:hover {");
      html.append("  background-color: var(--rstudio-list-hoverBackground, #d6dadc);");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append("<h2>");
      html.append(title);
      html.append("</h2>");
      html.append("<p>");
      html.append(message);
      html.append("</p>");
      html.append("<button id='restart-btn' class='chatIframeButton'>");
      html.append(constants_.chatRestartButton());
      html.append("</button>");
      html.append("</div>");
      html.append("<script>");
      html.append("document.getElementById('restart-btn').addEventListener('click', function() {");
      html.append("  window.parent.postMessage('restart-backend', '*');");
      html.append("});");
      html.append("</script>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.chatTabLabel());

      return toolbar_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      // Set up message listener once (guards against duplicate listeners)
      if (!listenerSetup_)
      {
         listenerSetup_ = true;
         setupMessageListener();
      }

      // Show pending message now that frame is loaded
      if (pendingMessage_ != null)
      {
         updateFrameContent(pendingMessage_);
         pendingMessage_ = null;
      }

      // Check if Chat features are installed (this triggers the initialization flow)
      server_.chatVerifyInstalled(new ServerRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean result)
         {
            // Don't call setStatus("not_installed") here - let the update check
            // flow determine what message to show. If noCompatibleVersion is true,
            // showIncompatibleVersion() will be called. If an update/install is
            // available, that notification will be shown. This avoids competing
            // updateFrameContent() calls that cause rendering issues.

            // Trigger observer so update/install check can happen
            if (observer_ != null)
            {
               observer_.onPaneReady();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            updateFrameContent(generateMessageHTML(constants_.errorDetectingInstallationMessage()));
         }
      });
   }

   private native void setupMessageListener() /*-{
      var self = this;

      // Listen for restart button clicks via postMessage
      $wnd.addEventListener('message', function(event) {
         if (event.data === 'restart-backend') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleRestartRequest()();
         }
      });
   }-*/;

   private void handleRestartRequest()
   {
      if (observer_ != null)
      {
         observer_.onRestartBackend();
      }
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
      // Only refresh URL content (actual chat UI) when pane becomes visible
      // HTML status messages are already displayed and don't need refresh
      // Refreshing HTML content can cause timing issues with pending load handlers
      if (currentStatus_ == ChatPresenter.Display.Status.READY &&
          contentType_ == ContentType.URL &&
          currentUrl_ != null)
      {
         frame_.setUrl(currentUrl_);
      }
   }

   // Resources ----
   public interface Resources extends ClientBundle
   {
      @Source("ChatPane.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String chatUpdateNotification();
      String chatNotificationButton();
      String chatUpdateMessage();
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   private LayoutPanel mainPanel_;
   private RStudioThemedFrame frame_;
   private Toolbar toolbar_;
   private boolean listenerSetup_ = false;
   private String pendingMessage_ = null;
   private ContentType contentType_ = ContentType.HTML;
   private String currentContent_ = null;
   private String currentUrl_ = null;
   private ChatPresenter.Display.Observer observer_;
   private ChatPresenter.Display.UpdateObserver updateObserver_;
   private ChatPresenter.Display.Status currentStatus_ = null;

   // Update notification UI components
   private FlowPanel updateNotificationPanel_;
   private HTML updateMessageLabel_;
   private HorizontalPanel updateButtonPanel_;

   // Injected ----
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Session session_;
   private final ChatServerOperations server_;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
