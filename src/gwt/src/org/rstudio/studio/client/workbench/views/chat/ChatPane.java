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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeColorExtractor;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.LocaleCookie;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ChatPane
      extends WorkbenchPane
      implements ChatPresenter.Display
{
   private enum NotificationType
   {
      NONE,
      UPDATING,
      UPDATE_COMPLETE,
      UPDATE_ERROR,
      UPDATE_CHECK_FAILURE,
      READLINE,
      CONNECTION_LOST
   }

   @Inject
   protected ChatPane(EventBus events,
                      Commands commands,
                      Provider<ProductEditionInfo> pEdition)
   {
      super(constants_.chatTitle(), events);

      events_ = events;
      commands_ = commands;
      pEdition_ = pEdition;

      ensureWidget();

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

      notificationIcon_ = new DecorativeImage(
         new ImageResource2x(MessageDialogImages.INSTANCE.dialog_info2x()));
      notificationIcon_.setPixelSize(24, 24);
      notificationIcon_.getElement().getStyle().setProperty("flexShrink", "0");
      notificationIcon_.setVisible(false);

      notificationContent_ = new FlowPanel();
      notificationContent_.setStyleName(RES.styles().chatNotificationContent());
      notificationContent_.add(notificationIcon_);
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
      suspendedOverlay_.getElement().getStyle().setBackgroundColor(
         ThemeColorExtractor.getEditorBackgroundColor("#fff"));
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
    * Wraps body HTML in a themed document shell with common CSS boilerplate.
    *
    * @param bodyHtml   Inner HTML for the body (inside a centered message div)
    * @param script     Optional JavaScript to append before closing body, or null
    * @param richContent When true, uses 40px padding and adds h2/p heading styles
    * @param includeButtonStyles Whether to include .chatIframeButton CSS
    * @param extraCss   Additional CSS to append inside the style block, or null
    */
   private String wrapInThemedHtml(String bodyHtml, String script,
                                   boolean richContent,
                                   boolean includeButtonStyles,
                                   String extraCss)
   {
      Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();
      if (colors == null)
         colors = new HashMap<>();

      String bgColor = colors.getOrDefault(
         "--rstudio-editor-background", "#fff");
      String fgColor = colors.getOrDefault(
         "--rstudio-editor-foreground", "#333");
      String disabledFgColor = colors.getOrDefault(
         "--rstudio-disabledForeground", "#666");
      String widgetBgColor = colors.getOrDefault(
         "--rstudio-editorWidget-background", "#f4f8f9");
      String borderColor = colors.getOrDefault(
         "--rstudio-panel-border", "#d6dadc");
      String hoverBgColor = colors.getOrDefault(
         "--rstudio-list-hoverBackground", "#d6dadc");

      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html lang='").append(LocaleCookie.getUiLanguage()).append("'>");
      html.append("<head><meta charset='UTF-8'><style>");

      // CSS reset and flex centering
      html.append("html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; }");
      html.append("body {");
      html.append("  display: flex; align-items: center; justify-content: center;");
      html.append("  font-family: ").append(ThemeFonts.getProportionalFont()).append(";");
      html.append("  color: var(--rstudio-editor-foreground, ").append(fgColor).append(");");
      html.append("  background-color: var(--rstudio-editor-background, ").append(bgColor).append(");");
      html.append("}");

      if (richContent)
      {
         html.append(".message { text-align: center; padding: 40px; }");
         html.append("h2 { color: var(--rstudio-editor-foreground, ").append(fgColor).append("); margin-bottom: 16px; }");
         html.append("p { color: var(--rstudio-disabledForeground, ").append(disabledFgColor).append("); margin: 0 0 24px 0; }");
      }
      else
      {
         html.append(".message { text-align: center; padding: 20px; }");
      }

      if (includeButtonStyles)
      {
         html.append(".chatIframeButton {");
         html.append("  padding: 10px 20px; font-size: 14px; cursor: pointer;");
         html.append("  background-color: var(--rstudio-editorWidget-background, ").append(widgetBgColor).append(");");
         html.append("  color: var(--rstudio-editor-foreground, ").append(fgColor).append(");");
         html.append("  border: 1px solid var(--rstudio-panel-border, ").append(borderColor).append(");");
         html.append("  border-radius: 4px;");
         html.append("}");
         html.append(".chatIframeButton:hover {");
         html.append("  background-color: var(--rstudio-list-hoverBackground, ").append(hoverBgColor).append(");");
         html.append("}");
      }

      if (extraCss != null)
      {
         html.append(extraCss);
      }

      html.append("</style></head><body>");
      html.append("<div class='message'>").append(bodyHtml).append("</div>");

      if (script != null)
      {
         html.append("<script>").append(script).append("</script>");
      }

      html.append("</body></html>");
      return html.toString();
   }

   private String generateMessageHTML(String message)
   {
      return wrapInThemedHtml(message, null, false, false,
         "body { font-size: 12px; }");
   }

   /**
    * Replaces the iframe content with the given HTML.
    *
    * @param html The HTML content to display
    */
   private void updateFrameContent(String html)
   {
      // Remove suspended overlay if present — new content needs to be
      // visible and interactive (e.g., error messages with retry buttons).
      if (suspendedOverlay_.getParent() == mainPanel_)
      {
         mainPanel_.remove(suspendedOverlay_);
      }

      // Force a complete iframe reload by navigating to about:blank first
      // This kills the existing JavaScript context (stops WebSocket reconnection attempts)

      // Set up a one-time load action to write content after about:blank loads
      frame_.setOnLoadAction(() -> {
         frame_.setFrameContent(html);
         injectThemeVariablesDelayed(frame_);
      });

      // Navigate to about:blank - the load action will fire when it completes
      frame_.setUrl("about:blank");
   }

   /**
    * Injects theme variables into the frame after a short delay.
    * This ensures the HTML content is fully parsed before variables are applied.
    */
   private void injectThemeVariablesDelayed(RStudioThemedFrame frame)
   {
      // Delay allows HTML to be parsed before theme variable injection
      Timers.singleShot(100, () -> {
         frame.injectThemeVariables();
      });
   }

   @Override
   public void loadUrl(String url)
   {
      if (suspendedOverlay_.getParent() == mainPanel_)
      {
         loadUrlFromSuspended(url);
      }
      else
      {
         frame_.setOnLoadAction(null);
         frame_.setUrl(url);
      }
   }

   private void loadUrlFromSuspended(String url)
   {
      if (loadTimeoutTimer_ != null)
      {
         loadTimeoutTimer_.cancel();
         loadTimeoutTimer_ = null;
      }

      if (pendingSwapTimer_ != null)
      {
         pendingSwapTimer_.cancel();
         pendingSwapTimer_ = null;
      }

      if (pendingFrame_ != null && pendingFrame_.getParent() == mainPanel_)
      {
         mainPanel_.remove(pendingFrame_);
      }

      final int savedScrollTop = getFrameScrollTop(frame_);

      RStudioThemedFrame newFrame = new RStudioThemedFrame(constants_.chatTitle());
      newFrame.setSize("100%", "100%");
      pendingFrame_ = newFrame;

      newFrame.getElement().getStyle().setVisibility(Visibility.HIDDEN);

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

      loadTimeoutTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            loadTimeoutTimer_ = null;
            if (newFrame.getParent() == mainPanel_)
            {
               mainPanel_.remove(newFrame);
            }
            if (pendingFrame_ == newFrame)
            {
               pendingFrame_ = null;
            }
            if (suspendedOverlay_.getParent() == mainPanel_)
            {
               mainPanel_.remove(suspendedOverlay_);
            }
            frame_.setUrl(url);
         }
      };
      loadTimeoutTimer_.schedule(FRAME_LOAD_TIMEOUT_MS);

      newFrame.setOnLoadAction(() -> {
         if (loadTimeoutTimer_ != null)
         {
            loadTimeoutTimer_.cancel();
            loadTimeoutTimer_ = null;
         }
         pendingSwapTimer_ = Timers.singleShot(FRAME_SWAP_DELAY_MS, () -> {
            pendingSwapTimer_ = null;
            if (mainPanel_.isAttached() &&
                newFrame.getParent() == mainPanel_ &&
                suspendedOverlay_.getParent() == mainPanel_)
            {
               newFrame.getElement().getStyle().setVisibility(Visibility.VISIBLE);
               frame_.setUrl("about:blank");
               mainPanel_.remove(frame_);
               mainPanel_.remove(suspendedOverlay_);
               frame_ = newFrame;
               pendingFrame_ = null;
               setFrameScrollTop(newFrame, savedScrollTop);
            }
            else
            {
               if (newFrame.getParent() == mainPanel_)
               {
                  mainPanel_.remove(newFrame);
               }
               if (pendingFrame_ == newFrame)
               {
                  pendingFrame_ = null;
               }
            }
         });
      });
      newFrame.setUrl(url);
   }

   private native int getFrameScrollTop(RStudioThemedFrame frame) /*-{
      try {
         var win = frame.@org.rstudio.core.client.widget.RStudioFrame::getWindow()();
         if (!win || !win.document) return 0;
         var els = win.document.querySelectorAll('*');
         var target = null;
         var maxScrollable = 0;
         for (var i = 0; i < els.length; i++) {
            var scrollable = els[i].scrollHeight - els[i].clientHeight;
            if (scrollable > maxScrollable) {
               maxScrollable = scrollable;
               target = els[i];
            }
         }
         return target ? target.scrollTop : 0;
      } catch (e) {
         @org.rstudio.core.client.Debug::logWarning(Ljava/lang/String;)(
            "Error reading frame scroll position: " + e.message);
         return 0;
      }
   }-*/;

   private native void setFrameScrollTop(RStudioThemedFrame frame, int scrollTop) /*-{
      try {
         if (scrollTop <= 0) return;
         var win = frame.@org.rstudio.core.client.widget.RStudioFrame::getWindow()();
         if (!win || !win.document) return;
         var els = win.document.querySelectorAll('*');
         var target = null;
         var maxScrollable = 0;
         for (var i = 0; i < els.length; i++) {
            var scrollable = els[i].scrollHeight - els[i].clientHeight;
            if (scrollable > maxScrollable) {
               maxScrollable = scrollable;
               target = els[i];
            }
         }
         if (target) target.scrollTop = scrollTop;
      } catch (e) {
         @org.rstudio.core.client.Debug::logWarning(Ljava/lang/String;)(
            "Error restoring frame scroll position: " + e.message);
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
   public void showUpdatingStatus()
   {
      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_info2x());
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
      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_info2x());
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
      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_error2x());
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
      notificationIcon_.setVisible(false);
      updateNotificationPanel_.setVisible(false);
      updateFrameLayout();
   }

   private void setNotificationIcon(com.google.gwt.resources.client.ImageResource resource)
   {
      notificationIcon_.setResource(new ImageResource2x(resource));
      notificationIcon_.setPixelSize(24, 24);
      notificationIcon_.setVisible(true);
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
   public void showConnectionLostNotification(String message)
   {
      // Don't overwrite higher-priority update notifications
      if (currentNotificationType_ == NotificationType.UPDATING ||
          currentNotificationType_ == NotificationType.UPDATE_ERROR ||
          currentNotificationType_ == NotificationType.UPDATE_COMPLETE ||
          currentNotificationType_ == NotificationType.UPDATE_CHECK_FAILURE)
      {
         return;
      }

      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_error2x());
      updateMessageLabel_.setHTML(SafeHtmlUtils.htmlEscape(message));

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear()
         .addButton(constants_.chatRestartButton(), () -> {
            if (observer_ != null)
            {
               observer_.onRestartBackend();
            }
         })
         .addButton(constants_.chatDismiss(), () -> hideUpdateNotification());

      currentNotificationType_ = NotificationType.CONNECTION_LOST;
      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void hideConnectionLostNotification()
   {
      if (currentNotificationType_ == NotificationType.CONNECTION_LOST)
      {
         hideUpdateNotification();
      }
   }

   @Override
   public void showReadlineNotification()
   {
      // Don't overwrite higher-priority update notifications
      if (currentNotificationType_ == NotificationType.UPDATING ||
          currentNotificationType_ == NotificationType.UPDATE_ERROR ||
          currentNotificationType_ == NotificationType.UPDATE_COMPLETE ||
          currentNotificationType_ == NotificationType.UPDATE_CHECK_FAILURE)
      {
         return;
      }

      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_info2x());
      updateMessageLabel_.setHTML(constants_.chatReadlineWaiting());

      new NotificationBuilder(updateButtonPanel_, RES.styles().chatNotificationButton())
         .clear();

      currentNotificationType_ = NotificationType.READLINE;
      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void hideReadlineNotification()
   {
      if (currentNotificationType_ == NotificationType.READLINE)
      {
         hideUpdateNotification();
      }
   }

   @Override
   public void showUpdateCheckFailure()
   {
      setNotificationIcon(MessageDialogImages.INSTANCE.dialog_error2x());
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
   public void showUnsupportedVersionUpgradeRequired(
       String currentVersion, String newVersion)
   {
      String html = generateUnsupportedVersionUpgradeHTML(
          currentVersion, newVersion);
      updateFrameContent(html);
   }

   @Override
   public void showUnsupportedProtocol()
   {
      showMessage(constants_.chatUnsupportedProtocolMessage());
   }

   @Override
   public void showManifestUnavailable(String errorMessage)
   {
      String html = generateManifestUnavailableHTML(errorMessage);
      updateFrameContent(html);
   }

   @Override
   public void showUnsupportedVersionNoUpdate(String currentVersion)
   {
      showMessage(constants_.chatUnsupportedVersionNoUpdateMessage(currentVersion));
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

   private boolean isWorkbench()
   {
      return pEdition_.get() != null &&
             pEdition_.get().proLicense() &&
             !Desktop.isDesktop();
   }

   private String generateNotInstalledWithInstallHTML(String newVersion)
   {
      String description = isWorkbench() ?
         constants_.chatNotInstalledDescriptionWorkbench() :
         constants_.chatNotInstalledDescription();

      String body =
         "<h2>" + constants_.chatNotInstalledTitle() + "</h2>" +
         "<p>" + constants_.chatNotInstalledWithVersionMessage(newVersion) + "</p>" +
         "<hr>" +
         "<p class='detail'>" + description + "</p>" +
         "<p class='detail'>" +
         "<a href='https://posit.ai' target='_blank' rel='noopener noreferrer'>" +
         constants_.chatLearnMore() + "</a></p>" +
         "<hr>" +
         "<button id='install-btn' class='chatIframeButton'>" +
         constants_.chatInstallButton() + "</button>" +
         "<p class='detail'>" + constants_.chatInstallTermsOfUse() + "</p>";

      String script =
         "document.getElementById('install-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('install-now', '*');" +
         "});";

      String extraCss =
         "hr { border: none; border-top: 1px solid var(--rstudio-panel-border); margin: 24px 0; }" +
         ".detail { font-size: 12px; }" +
         ".chatIframeButton { margin-bottom: 16px; }";

      return wrapInThemedHtml(body, script, true, true, extraCss);
   }

   private String generateUpdateAvailableHTML(String currentVersion, String newVersion)
   {
      String body =
         "<h2>" + constants_.chatUpdateAvailableTitle() + "</h2>" +
         "<p>" + constants_.chatUpdateAvailableWithVersionsMessage(currentVersion, newVersion) + "</p>" +
         "<button id='update-btn' class='chatIframeButton'>" +
         constants_.chatUpdateButton() + "</button>" +
         "<button id='ignore-btn' class='chatIframeButton'>" +
         constants_.chatIgnore() + "</button>";

      String script =
         "document.getElementById('update-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('install-now', '*');" +
         "});" +
         "document.getElementById('ignore-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('remind-later', '*');" +
         "});";

      return wrapInThemedHtml(body, script, true, true,
         ".chatIframeButton { margin: 0 8px; }");
   }

   private String generateUnsupportedVersionUpgradeHTML(
       String currentVersion, String newVersion)
   {
      String body =
         "<h2>" + constants_.chatUpdateRequiredTitle() + "</h2>" +
         "<p>" + constants_.chatUnsupportedVersionMessage(currentVersion, newVersion) + "</p>" +
         "<button id='update-btn' class='chatIframeButton'>" +
         constants_.chatUpdateButton() + "</button>";

      String script =
         "document.getElementById('update-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('install-now', '*');" +
         "});";

      return wrapInThemedHtml(body, script, true, true,
         ".chatIframeButton { margin: 0 8px; }");
   }

   private void showAssistantNotEnabled()
   {
      String html = generateAssistantNotEnabledHTML();
      updateFrameContent(html);
   }

   private String generateAssistantNotEnabledHTML()
   {
      String body =
         "<h2>" + constants_.chatAssistantNotEnabledTitle() + "</h2>" +
         "<p>" + constants_.chatAssistantNotEnabledMessage() + "</p>" +
         "<button id='options-btn' class='chatIframeButton'>" +
         constants_.chatGlobalOptionsButton() + "</button>";

      String script =
         "document.getElementById('options-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('open-global-options', '*');" +
         "});";

      return wrapInThemedHtml(body, script, true, true, null);
   }

   @Override
   public void setStatus(ChatPresenter.Display.Status status)
   {
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
      // Add a semi-transparent overlay to dim the chat UI and block interaction.
      // We intentionally do NOT navigate the iframe to about:blank here;
      // the overlay blocks interaction, and the iframe content will be
      // replaced when loadUrl() is called on session resume.
      frame_.setOnLoadAction(null);

      if (loadTimeoutTimer_ != null)
      {
         loadTimeoutTimer_.cancel();
         loadTimeoutTimer_ = null;
      }

      if (pendingSwapTimer_ != null)
      {
         pendingSwapTimer_.cancel();
         pendingSwapTimer_ = null;
      }

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

   @Override
   public void showPoppedOutPlaceholder()
   {
      // Remove suspended overlay if present (e.g. after session resume)
      if (suspendedOverlay_.getParent() == mainPanel_)
      {
         mainPanel_.remove(suspendedOverlay_);
      }

      String html = generatePoppedOutPlaceholderHTML();
      updateFrameContent(html);
   }

   @Override
   public void hidePoppedOutPlaceholder()
   {
      // Content will be replaced by loadUrl() when chat returns to main
   }

   private String generatePoppedOutPlaceholderHTML()
   {
      String body =
         "<p>" + constants_.chatPoppedOutMessage() + "</p>" +
         "<div class='button-group'>" +
         "<button id='bring-to-front-btn'>" +
         constants_.chatBringToFrontButton() + "</button>" +
         "<button id='return-to-main-btn'>" +
         constants_.chatReturnHereButton() + "</button>" +
         "</div>";

      String script =
         "document.getElementById('bring-to-front-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('bring-chat-to-front', '*');" +
         "});" +
         "document.getElementById('return-to-main-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('return-chat-to-main', '*');" +
         "});";

      String extraCss =
         ".message { padding: 40px; }" +
         "p { font-size: 13px; margin-bottom: 20px; }" +
         ".button-group { display: flex; gap: 8px; justify-content: center; }" +
         "button {" +
         "  padding: 6px 16px; font-size: 12px;" +
         "  border: 1px solid var(--rstudio-panel-border, #d6dadc);" +
         "  border-radius: 4px;" +
         "  background-color: var(--rstudio-editorWidget-background, #f4f8f9);" +
         "  color: var(--rstudio-editor-foreground, #333);" +
         "  cursor: pointer;" +
         "}" +
         "button:hover {" +
         "  background-color: var(--rstudio-list-hoverBackground, #d6dadc);" +
         "}";

      return wrapInThemedHtml(body, script, false, false, extraCss);
   }

   private String generateCrashedMessageHTML(int exitCode)
   {
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

      return generateCrashedOrErrorHTML(title, message);
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.chatTabLabel());

      popOutButton_ = commands_.popOutChat().createToolbarButton();
      toolbar_.addRightWidget(popOutButton_);

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

      // Trigger observer to start initialization flow.
      // The ChatPresenter handles preference checks and update checks,
      // showing appropriate UI based on whether Posit Assistant is enabled/installed.
      if (observer_ != null)
      {
         observer_.onPaneReady();
      }
   }

   private native void setupMessageListener() /*-{
      var self = this;

      // Listen for postMessage from our iframe only (source + origin check)
      $wnd.addEventListener('message', function(event) {
         var frame = self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::getFrameElement()();
         if (!frame || event.source !== frame.contentWindow) return;
         if (event.origin !== $wnd.location.origin) return;

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
         else if (event.data === 'bring-chat-to-front') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleBringToFrontRequest()();
         }
         else if (event.data === 'return-chat-to-main') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleReturnToMainRequest()();
         }
         else if (event.data === 'retry-manifest') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleRetryManifestRequest()();
         }
         else if (event.data && event.data.type === 'assistant-error') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleIframeError(Ljava/lang/String;)(event.data.message || '');
         }
         else if (event.data && event.data.type === 'assistant-warning') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleIframeWarning(Ljava/lang/String;)(event.data.message || '');
         }
         else if (event.data && event.data.type === 'assistant-connected') {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatPane::handleIframeConnected()();
         }
      });
   }-*/;

   private com.google.gwt.dom.client.Element getFrameElement()
   {
      return (frame_ != null) ? frame_.getElement() : null;
   }

   private void handleRestartRequest()
   {
      if (observer_ != null)
      {
         observer_.onRestartBackend();
      }
   }

   private void handleRetryManifestRequest()
   {
      if (observer_ != null)
      {
         observer_.onRetryManifest();
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

   private void handleBringToFrontRequest()
   {
      if (observer_ != null)
      {
         observer_.onActivateChat();
      }
   }

   private void handleReturnToMainRequest()
   {
      if (observer_ != null)
      {
         observer_.onReturnChatToMain();
      }
   }

   private void handleIframeError(String message)
   {
      if (observer_ != null)
      {
         observer_.onIframeError(message);
      }
   }

   private void handleIframeWarning(String message)
   {
      if (observer_ != null)
      {
         observer_.onIframeWarning(message);
      }
   }

   private void handleIframeConnected()
   {
      if (observer_ != null)
      {
         observer_.onIframeConnected();
      }
   }

   @Override
   public String getNotInstalledWithInstallHTML(String newVersion)
   {
      return generateNotInstalledWithInstallHTML(newVersion);
   }

   @Override
   public String getUpdateAvailableWithVersionsHTML(
      String currentVersion, String newVersion)
   {
      return generateUpdateAvailableHTML(currentVersion, newVersion);
   }

   @Override
   public String getMessageHTML(String message)
   {
      return generateMessageHTML(message);
   }

   @Override
   public String getIncompatibleVersionHTML()
   {
      return generateMessageHTML(constants_.chatIncompatibleVersion());
   }

   @Override
   public String getUnsupportedVersionUpgradeHTML(
      String currentVersion, String newVersion)
   {
      return generateUnsupportedVersionUpgradeHTML(
         currentVersion, newVersion);
   }

   @Override
   public String getUnsupportedVersionNoUpdateHTML(String currentVersion)
   {
      return generateMessageHTML(
         constants_.chatUnsupportedVersionNoUpdateMessage(currentVersion));
   }

   @Override
   public String getUnsupportedProtocolHTML()
   {
      return generateMessageHTML(
         constants_.chatUnsupportedProtocolMessage());
   }

   @Override
   public String getManifestUnavailableHTML(String errorMessage)
   {
      return generateManifestUnavailableHTML(errorMessage);
   }

   @Override
   public String getErrorHTML(String errorMessage)
   {
      return generateCrashedMessageHTML(errorMessage);
   }

   private String generateCrashedMessageHTML(String errorMessage)
   {
      return generateCrashedOrErrorHTML(
         constants_.chatProcessExitedTitle(), errorMessage);
   }

   private String generateCrashedOrErrorHTML(String title, String message)
   {
      String safeTitle = SafeHtmlUtils.htmlEscape(title);
      String safeMessage = SafeHtmlUtils.htmlEscape(message);

      String body =
         "<h2>" + safeTitle + "</h2>" +
         "<p>" + safeMessage + "</p>" +
         "<button id='restart-btn' class='chatIframeButton'>" +
         constants_.chatRestartButton() + "</button>";

      String script =
         "document.getElementById('restart-btn').addEventListener('click', function() {" +
         "  window.parent.postMessage('restart-backend', '*');" +
         "});";

      return wrapInThemedHtml(body, script, true, true, null);
   }

   private String generateManifestUnavailableHTML(String errorMessage)
   {
      String safeTitle = SafeHtmlUtils.htmlEscape(
         constants_.chatManifestUnavailableTitle());
      String safeMessage = SafeHtmlUtils.htmlEscape(
         constants_.chatManifestUnavailableMessage());

      String errorDetail = "";
      String copyScript = "";
      if (errorMessage != null && !errorMessage.isEmpty())
      {
         String safeError = SafeHtmlUtils.htmlEscape(errorMessage);
         String safeCopyLabel = SafeHtmlUtils.htmlEscape(
            constants_.chatCopyError());
         String safeCopiedLabel = SafeHtmlUtils.htmlEscape(
            constants_.chatCopiedError());
         String safeFailedLabel = SafeHtmlUtils.htmlEscape(
            constants_.chatCopyFailed());
         errorDetail =
            "<div style='text-align: center; margin: 12px 0 4px 0;'>" +
            "<button id='copy-error-btn' data-copy-label='" +
            safeCopyLabel + "' data-copied-label='" +
            safeCopiedLabel + "' data-failed-label='" +
            safeFailedLabel + "' style='padding: 4px 12px; " +
            "font-size: 12px; cursor: pointer; border: 1px solid " +
            "var(--rstudio-panel-border, #ccc); border-radius: 3px; " +
            "background: var(--rstudio-editorWidget-background, #fff); " +
            "color: var(--rstudio-editor-foreground, #333);'>" +
            safeCopyLabel + "</button>" +
            "</div>" +
            "<pre id='error-detail' style='margin: 0 0 12px 0; padding: 8px; " +
            "background: var(--rstudio-editorWidget-background, #f4f8f9); " +
            "color: var(--rstudio-editor-foreground, #333); " +
            "border-radius: 4px; font-size: 12px; " +
            "white-space: pre-wrap; word-break: break-word;'>" +
            safeError + "</pre>";
         copyScript =
            "document.getElementById('copy-error-btn')" +
            ".addEventListener('click', function() {" +
            "  var text = document.getElementById('error-detail').textContent;" +
            "  var btn = document.getElementById('copy-error-btn');" +
            "  navigator.clipboard.writeText(text).then(function() {" +
            "    btn.textContent = btn.dataset.copiedLabel;" +
            "    setTimeout(function() { btn.textContent = btn.dataset.copyLabel; }, 2000);" +
            "  }).catch(function() {" +
            "    btn.textContent = btn.dataset.failedLabel;" +
            "    setTimeout(function() { btn.textContent = btn.dataset.copyLabel; }, 2000);" +
            "  });" +
            "});";
      }

      String body =
         "<h2>" + safeTitle + "</h2>" +
         "<p>" + safeMessage + "</p>" +
         errorDetail +
         "<button id='retry-manifest-btn' class='chatIframeButton'>" +
         constants_.chatRetry() + "</button>";

      String script =
         "document.getElementById('retry-manifest-btn')" +
         ".addEventListener('click', function() {" +
         "  this.disabled = true;" +
         "  window.parent.postMessage('retry-manifest', '*');" +
         "});" +
         copyScript;

      return wrapInThemedHtml(body, script, true, true, null);
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
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
   private Timer pendingSwapTimer_;
   private Timer loadTimeoutTimer_;
   private HTML suspendedOverlay_;
   private Toolbar toolbar_;
   private ToolbarButton popOutButton_;
   private boolean listenerSetup_ = false;
   private String pendingMessage_ = null;
   private ChatPresenter.Display.Observer observer_;
   private ChatPresenter.Display.UpdateObserver updateObserver_;

   private NotificationType currentNotificationType_ = NotificationType.NONE;

   // Update notification UI components
   private FlowPanel updateNotificationPanel_;
   private FlowPanel notificationContent_;
   private HTML updateMessageLabel_;
   private FlowPanel updateButtonPanel_;
   private DecorativeImage notificationIcon_;

   // Injected ----
   private final EventBus events_;
   private final Commands commands_;
   private final Provider<ProductEditionInfo> pEdition_;

   private static final int FRAME_SWAP_DELAY_MS = 350;
   private static final int FRAME_LOAD_TIMEOUT_MS = 15000;
   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
