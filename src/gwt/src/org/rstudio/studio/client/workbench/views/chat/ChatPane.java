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

import java.util.Map;

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
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations.ChatVerifyInstalledResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
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

   private enum NotificationType
   {
      NONE,
      UPDATING,
      UPDATE_COMPLETE,
      UPDATE_ERROR,
      UPDATE_CHECK_FAILURE
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

            // Update suspended overlay color for new theme
            if (suspendedOverlay_ != null)
            {
               updateSuspendedOverlayStyle();
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
      updateButtonPanel_ = new FlowPanel();
      updateButtonPanel_.setStyleName(RES.styles().chatNotificationButtonPanel());

      notificationContent_ = new FlowPanel();
      notificationContent_.setStyleName(RES.styles().chatNotificationContent());
      notificationContent_.add(updateMessageLabel_);
      notificationContent_.add(updateButtonPanel_);
      updateNotificationPanel_.add(notificationContent_);

      frame_ = new RStudioThemedFrame(constants_.chatTitle());
      frame_.setSize("100%", "100%");

      // Store initial message to show after frame loads
      pendingMessage_ = generateMessageHTML(constants_.checkingInstallationMessage());

      // Create suspended overlay (added/removed from panel on suspend/resume)
      suspendedOverlay_ = new HTML();
      suspendedOverlay_.setSize("100%", "100%");
      updateSuspendedOverlayStyle();

      // Add update notification and frame to layout
      mainPanel_.add(updateNotificationPanel_);
      mainPanel_.add(frame_);

      // Position update notification at top (starts at 0 height since hidden)
      mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(updateNotificationPanel_, 0, Unit.PX, 0, Unit.PX);

      // Position frame below notification (initially at 0 since notification is hidden)
      updateFrameLayout();

      return mainPanel_;
   }

   private void updateSuspendedOverlayStyle()
   {
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      suspendedOverlay_.getElement().getStyle().setBackgroundColor(bgColor);
   }

   private void updateFrameLayout()
   {
      if (updateNotificationPanel_.isVisible())
      {
         // Set minimal temporary height so content overflows, then measure scrollHeight
         mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, 1, Unit.PX);
         mainPanel_.forceLayout();

         // Measure actual content height (including padding) after layout completes
         Scheduler.get().scheduleDeferred(() -> {
            int height = updateNotificationPanel_.getElement().getScrollHeight();
            mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, height, Unit.PX);
            mainPanel_.setWidgetTopBottom(frame_, height, Unit.PX, 0, Unit.PX);
            mainPanel_.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);
         });
      }
      else
      {
         // Notification hidden, frame takes full height
         mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, 0, Unit.PX);
         mainPanel_.setWidgetTopBottom(frame_, 0, Unit.PX, 0, Unit.PX);
         mainPanel_.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);
      }
   }

   /**
    * Generates centered HTML content for display in the iframe.
    *
    * @param message The message text to display
    * @return HTML string with proper styling for centered content
    */
   private String generateMessageHTML(String message)
   {
      // Get current theme colors for CSS fallbacks to avoid flash of wrong theme
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault("--rstudio-editor-foreground", "#000");

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
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  background-color: var(--rstudio-editor-background, " + bgColor + ");");
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

      // Set up a one-time load action to write content after about:blank loads
      frame_.setOnLoadAction(() -> {
         setFrameContent(frame_, html);
         injectThemeVariablesDelayed(frame_);
      });

      // Navigate to about:blank - the load action will fire when it completes
      frame_.setUrl("about:blank");
   }

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
      contentType_ = ContentType.URL;
      currentUrl_ = url;
      currentContent_ = null;

      if (suspendedOverlay_.getParent() == mainPanel_)
      {
         if (pendingFrame_ != null && pendingFrame_.getParent() == mainPanel_)
         {
            mainPanel_.remove(pendingFrame_);
         }

         RStudioThemedFrame newFrame = new RStudioThemedFrame(constants_.chatTitle());
         newFrame.setSize("100%", "100%");
         pendingFrame_ = newFrame;

         newFrame.getElement().getStyle().setVisibility(
            com.google.gwt.dom.client.Style.Visibility.HIDDEN);

         mainPanel_.add(newFrame);

         if (updateNotificationPanel_.isVisible())
         {
            int height = updateNotificationPanel_.getElement().getScrollHeight();
            mainPanel_.setWidgetTopBottom(newFrame, height, Unit.PX, 0, Unit.PX);
         }
         else
         {
            mainPanel_.setWidgetTopBottom(newFrame, 0, Unit.PX, 0, Unit.PX);
         }
         mainPanel_.setWidgetLeftRight(newFrame, 0, Unit.PX, 0, Unit.PX);

         newFrame.setOnLoadAction(() -> {
            Timers.singleShot(350, () -> {
               if (suspendedOverlay_.getParent() == mainPanel_)
               {
                  newFrame.getElement().getStyle().setVisibility(
                     com.google.gwt.dom.client.Style.Visibility.VISIBLE);
                  mainPanel_.remove(frame_);
                  mainPanel_.remove(suspendedOverlay_);
                  frame_ = newFrame;
                  pendingFrame_ = null;
               }
            });
         });
         newFrame.setUrl(url);
      }
      else
      {
         frame_.setOnLoadAction(null);
         frame_.setUrl(url);
      }
   }

   @Override
   public void updateCachedUrl(String url)
   {
      if (contentType_ == ContentType.URL)
      {
         currentUrl_ = url;
      }
   }

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
   public void showUpdatingStatus()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdating());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear();

      currentNotificationType_ = NotificationType.UPDATING;
      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdateComplete()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateComplete());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear();

      currentNotificationType_ = NotificationType.UPDATE_COMPLETE;
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

      currentNotificationType_ = NotificationType.UPDATE_ERROR;
      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void hideUpdateNotification()
   {
      currentNotificationType_ = NotificationType.NONE;
      updateNotificationPanel_.setVisible(false);
      updateFrameLayout();
   }

   @Override
   public void hideErrorNotification()
   {
      if (currentNotificationType_ == NotificationType.UPDATE_ERROR ||
          currentNotificationType_ == NotificationType.UPDATE_CHECK_FAILURE)
      {
         hideUpdateNotification();
      }
   }

   @Override
   public void showUpdateCheckFailure()
   {
      updateMessageLabel_.setHTML(constants_.chatUpdateCheckFailed());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatDismiss(), () -> hideUpdateNotification());

      currentNotificationType_ = NotificationType.UPDATE_CHECK_FAILURE;
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
   public void showNotInstalledWithInstall(String newVersion)
   {
      String html = generateNotInstalledWithInstallHTML(newVersion);
      updateFrameContent(html);
   }

   @Override
   public void showUpdateAvailableWithVersions(String currentVersion, String newVersion)
   {
      String html = generateUpdateAvailableHTML(currentVersion, newVersion);
      updateFrameContent(html);
   }

   private String generateNotInstalledWithInstallHTML(String newVersion)
   {
      // Get current theme colors for CSS fallbacks to avoid flash of wrong theme
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault("--rstudio-editor-foreground", "#333");
      String disabledFgColor = colors.getOrDefault("--rstudio-disabledForeground", "#666");
      String widgetBgColor = colors.getOrDefault("--rstudio-editorWidget-background", "#f4f8f9");
      String borderColor = colors.getOrDefault("--rstudio-panel-border", "#d6dadc");
      String hoverBgColor = colors.getOrDefault("--rstudio-list-hoverBackground", "#d6dadc");

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
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  background-color: var(--rstudio-editor-background, " + bgColor + ");");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, " + disabledFgColor + ");");
      html.append("  margin: 0 0 24px 0;");
      html.append("}");
      html.append("hr {");
      html.append("  border: none;");
      html.append("  border-top: 1px solid var(--rstudio-panel-border, " + borderColor + ");");
      html.append("  margin: 24px 0;");
      html.append("}");
      html.append(".detail {");
      html.append("  font-size: 12px;");
      html.append("}");
      html.append(".chatIframeButton {");
      html.append("  padding: 10px 20px;");
      html.append("  font-size: 14px;");
      html.append("  cursor: pointer;");
      html.append("  background-color: var(--rstudio-editorWidget-background, " + widgetBgColor + ");");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  border: 1px solid var(--rstudio-panel-border, " + borderColor + ");");
      html.append("  border-radius: 4px;");
      html.append("}");
      html.append(".chatIframeButton:hover {");
      html.append("  background-color: var(--rstudio-list-hoverBackground, " + hoverBgColor + ");");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append("<h2>");
      html.append(constants_.chatNotInstalledTitle());
      html.append("</h2>");
      html.append("<p>");
      html.append(constants_.chatNotInstalledWithVersionMessage(newVersion));
      html.append("</p>");
      html.append("<hr>");
      html.append("<p class='detail'>");
      html.append(constants_.chatNotInstalledDescription());
      html.append("</p>");
      html.append("<p class='detail'>");
      html.append("<a href='https://posit.ai' target='_blank' rel='noopener noreferrer'>");
      html.append(constants_.chatLearnMore());
      html.append("</a>");
      html.append("</p>");
      html.append("<p class='detail'>");
      html.append(constants_.chatNotInstalledBetaNotice());
      html.append("</p>");
      html.append("<hr>");
      html.append("<button id='install-btn' class='chatIframeButton'>");
      html.append(constants_.chatInstallButton());
      html.append("</button>");
      html.append("</div>");
      html.append("<script>");
      html.append("document.getElementById('install-btn').addEventListener('click', function() {");
      html.append("  window.parent.postMessage('install-now', '*');");
      html.append("});");
      html.append("</script>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   private String generateUpdateAvailableHTML(String currentVersion, String newVersion)
   {
      // Get current theme colors for CSS fallbacks to avoid flash of wrong theme
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault("--rstudio-editor-foreground", "#333");
      String disabledFgColor = colors.getOrDefault("--rstudio-disabledForeground", "#666");
      String widgetBgColor = colors.getOrDefault("--rstudio-editorWidget-background", "#f4f8f9");
      String borderColor = colors.getOrDefault("--rstudio-panel-border", "#d6dadc");
      String hoverBgColor = colors.getOrDefault("--rstudio-list-hoverBackground", "#d6dadc");

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
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  background-color: var(--rstudio-editor-background, " + bgColor + ");");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, " + disabledFgColor + ");");
      html.append("  margin: 0 0 24px 0;");
      html.append("}");
      html.append(".chatIframeButton {");
      html.append("  padding: 10px 20px;");
      html.append("  font-size: 14px;");
      html.append("  cursor: pointer;");
      html.append("  background-color: var(--rstudio-editorWidget-background, " + widgetBgColor + ");");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  border: 1px solid var(--rstudio-panel-border, " + borderColor + ");");
      html.append("  border-radius: 4px;");
      html.append("  margin: 0 8px;");
      html.append("}");
      html.append(".chatIframeButton:hover {");
      html.append("  background-color: var(--rstudio-list-hoverBackground, " + hoverBgColor + ");");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append("<h2>");
      html.append(constants_.chatUpdateAvailableTitle());
      html.append("</h2>");
      html.append("<p>");
      html.append(constants_.chatUpdateAvailableWithVersionsMessage(currentVersion, newVersion));
      html.append("</p>");
      html.append("<button id='update-btn' class='chatIframeButton'>");
      html.append(constants_.chatUpdateButton());
      html.append("</button>");
      html.append("<button id='ignore-btn' class='chatIframeButton'>");
      html.append(constants_.chatIgnore());
      html.append("</button>");
      html.append("</div>");
      html.append("<script>");
      html.append("document.getElementById('update-btn').addEventListener('click', function() {");
      html.append("  window.parent.postMessage('install-now', '*');");
      html.append("});");
      html.append("document.getElementById('ignore-btn').addEventListener('click', function() {");
      html.append("  window.parent.postMessage('remind-later', '*');");
      html.append("});");
      html.append("</script>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   private void showAssistantNotEnabled()
   {
      String html = generateAssistantNotEnabledHTML();
      updateFrameContent(html);
   }

   private String generateAssistantNotEnabledHTML()
   {
      // Get current theme colors for CSS fallbacks to avoid flash of wrong theme
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault("--rstudio-editor-foreground", "#333");
      String disabledFgColor = colors.getOrDefault("--rstudio-disabledForeground", "#666");
      String widgetBgColor = colors.getOrDefault("--rstudio-editorWidget-background", "#f4f8f9");
      String borderColor = colors.getOrDefault("--rstudio-panel-border", "#d6dadc");
      String hoverBgColor = colors.getOrDefault("--rstudio-list-hoverBackground", "#d6dadc");

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
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  background-color: var(--rstudio-editor-background, " + bgColor + ");");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, " + disabledFgColor + ");");
      html.append("  margin: 0 0 24px 0;");
      html.append("}");
      html.append(".chatIframeButton {");
      html.append("  padding: 10px 20px;");
      html.append("  font-size: 14px;");
      html.append("  cursor: pointer;");
      html.append("  background-color: var(--rstudio-editorWidget-background, " + widgetBgColor + ");");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  border: 1px solid var(--rstudio-panel-border, " + borderColor + ");");
      html.append("  border-radius: 4px;");
      html.append("}");
      html.append(".chatIframeButton:hover {");
      html.append("  background-color: var(--rstudio-list-hoverBackground, " + hoverBgColor + ");");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append("<h2>");
      html.append(constants_.chatAssistantNotEnabledTitle());
      html.append("</h2>");
      html.append("<p>");
      html.append(constants_.chatAssistantNotEnabledMessage());
      html.append("</p>");
      html.append("<button id='options-btn' class='chatIframeButton'>");
      html.append(constants_.chatGlobalOptionsButton());
      html.append("</button>");
      html.append("</div>");
      html.append("<script>");
      html.append("document.getElementById('options-btn').addEventListener('click', function() {");
      html.append("  window.parent.postMessage('open-global-options', '*');");
      html.append("});");
      html.append("</script>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
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
            showAssistantNotEnabled();
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
      // Add overlay on top of the iframe to gray out the chat UI.
      // We intentionally do NOT navigate the iframe to about:blank here;
      // the overlay blocks interaction, and the iframe content will be
      // replaced when loadUrl() is called on session resume.
      if (pendingFrame_ != null && pendingFrame_.getParent() == mainPanel_)
      {
         mainPanel_.remove(pendingFrame_);
         pendingFrame_ = null;
      }

      suspendedOverlay_.getElement().getStyle().setOpacity(0.5);

      if (suspendedOverlay_.getParent() != mainPanel_)
      {
         mainPanel_.add(suspendedOverlay_);
         mainPanel_.setWidgetLeftRight(suspendedOverlay_, 0, Unit.PX, 0, Unit.PX);
         mainPanel_.setWidgetTopBottom(suspendedOverlay_, 0, Unit.PX, 0, Unit.PX);
      }
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

      // Get current theme colors for CSS fallbacks to avoid flash of wrong theme
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      String bgColor = colors.getOrDefault("--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault("--rstudio-editor-foreground", "#333");
      String disabledFgColor = colors.getOrDefault("--rstudio-disabledForeground", "#666");
      String widgetBgColor = colors.getOrDefault("--rstudio-editorWidget-background", "#f4f8f9");
      String borderColor = colors.getOrDefault("--rstudio-panel-border", "#d6dadc");
      String hoverBgColor = colors.getOrDefault("--rstudio-list-hoverBackground", "#d6dadc");

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
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  background-color: var(--rstudio-editor-background, " + bgColor + ");");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 40px;");
      html.append("}");
      html.append("h2 {");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  margin-bottom: 16px;");
      html.append("}");
      html.append("p {");
      html.append("  color: var(--rstudio-disabledForeground, " + disabledFgColor + ");");
      html.append("  margin: 0 0 24px 0;");
      html.append("}");
      html.append(".chatIframeButton {");
      html.append("  padding: 10px 20px;");
      html.append("  font-size: 14px;");
      html.append("  cursor: pointer;");
      html.append("  background-color: var(--rstudio-editorWidget-background, " + widgetBgColor + ");");
      html.append("  color: var(--rstudio-editor-foreground, " + fgColor + ");");
      html.append("  border: 1px solid var(--rstudio-panel-border, " + borderColor + ");");
      html.append("  border-radius: 4px;");
      html.append("}");
      html.append(".chatIframeButton:hover {");
      html.append("  background-color: var(--rstudio-list-hoverBackground, " + hoverBgColor + ");");
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

      // Check if Posit Assistant is installed, then trigger observer with result
      server_.chatVerifyInstalled(new ServerRequestCallback<ChatVerifyInstalledResponse>()
      {
         @Override
         public void onResponseReceived(ChatVerifyInstalledResponse result)
         {
            boolean installed = result.installed;
            String installedVersion = installed ? result.version : null;

            // Trigger observer to start initialization flow
            // The ChatPresenter will handle preference checks and update checks,
            // showing appropriate UI based on whether Posit AI is enabled/installed
            if (observer_ != null)
            {
               observer_.onPaneReady(installed, installedVersion);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            // On error, assume not installed and let the update check handle it
            if (observer_ != null)
            {
               observer_.onPaneReady(false, null);
            }
         }
      });
   }

   private native void setupMessageListener() /*-{
      var self = this;

      // Listen for button clicks via postMessage
      $wnd.addEventListener('message', function(event) {
         if (event.data === 'restart-backend') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleRestartRequest()();
         }
         else if (event.data === 'install-now') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleInstallNowRequest()();
         }
         else if (event.data === 'remind-later') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleRemindLaterRequest()();
         }
         else if (event.data === 'open-global-options') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleOpenGlobalOptionsRequest()();
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

   private void handleInstallNowRequest()
   {
      if (updateObserver_ != null)
      {
         updateObserver_.onUpdateNow();
      }
   }

   private void handleRemindLaterRequest()
   {
      if (updateObserver_ != null)
      {
         updateObserver_.onRemindLater();
      }
   }

   private void handleOpenGlobalOptionsRequest()
   {
      commands_.showAssistantOptions().execute();
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
      String chatNotificationContent();
      String chatNotificationButton();
      String chatNotificationButtonPanel();
      String chatUpdateMessage();
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   private LayoutPanel mainPanel_;
   private RStudioThemedFrame frame_;
   private RStudioThemedFrame pendingFrame_;
   private HTML suspendedOverlay_;
   private Toolbar toolbar_;
   private boolean listenerSetup_ = false;
   private String pendingMessage_ = null;
   private ContentType contentType_ = ContentType.HTML;
   private String currentContent_ = null;
   private String currentUrl_ = null;
   private ChatPresenter.Display.Observer observer_;
   private ChatPresenter.Display.UpdateObserver updateObserver_;
   private ChatPresenter.Display.Status currentStatus_ = null;
   private NotificationType currentNotificationType_ = NotificationType.NONE;

   // Update notification UI components
   private FlowPanel updateNotificationPanel_;
   private FlowPanel notificationContent_;
   private HTML updateMessageLabel_;
   private FlowPanel updateButtonPanel_;

   // Injected ----
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Session session_;
   private final ChatServerOperations server_;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
