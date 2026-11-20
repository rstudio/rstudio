/*
 * ChatSatelliteWindow.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.ApplicationCommandManager;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeColorExtractor;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.chat.events.ChatReturnToMainEvent;
import org.rstudio.studio.client.workbench.views.chat.events.ChatSatelliteActionEvent;
import org.rstudio.studio.client.workbench.views.chat.events.ChatSuspendedNoticeEvent;
import org.rstudio.studio.client.workbench.views.chat.model.ChatSatelliteParams;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ChatSatelliteWindow extends SatelliteWindow
                                  implements ChatSatelliteView
{
   @Inject
   public ChatSatelliteWindow(Provider<EventBus> pEventBus,
                               Provider<FontSizeManager> pFSManager,
                               Commands commands,
                               ApplicationCommandManager appCommandManager)
   {
      super(pEventBus, pFSManager);
      pEventBus_ = pEventBus;
      commands_ = commands;
      appCommandManager_ = appCommandManager;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel,
                                JavaScriptObject params)
   {
      ChatSatelliteParams chatParams = params.cast();

      Window.setTitle(constants_.chatSatelliteWindowTitle());

      // Create toolbar with Return to Main button
      Toolbar toolbar = new Toolbar(constants_.chatSatelliteWindowTitle());

      ToolbarButton returnButton = new ToolbarButton(
         ToolbarButton.NoText,
         commands_.returnChatToMain().getDesc(),
         commands_.returnChatToMain().getImageResource(),
         event -> pEventBus_.get().fireEvent(new ChatReturnToMainEvent()));
      ElementIds.assignElementId(returnButton, ElementIds.CHAT_RETURN_TO_MAIN_BUTTON);
      toolbar.addRightWidget(returnButton);

      // Create themed iframe for chat content
      frame_ = new RStudioThemedFrame(constants_.chatSatelliteWindowTitle());
      frame_.setSize("100%", "100%");

      // Set URL if loading a chat URL (HTML content is deferred below)
      String contentHtml = chatParams.getContentHtml();
      if (contentHtml == null)
      {
         frame_.setUrl(chatParams.getChatUrl());
      }

      // Forward postMessage from iframe to main window
      setupMessageForwarder();
      AssistantKeyForwarder.install();

      // Elsewhere the user's custom bindings are loaded when an Ace editor loads, which
      // never happens here, so load them for keys forwarded from PA
      appCommandManager_.loadBindings();

      // Create suspend overlay (shown during session suspend to block interaction)
      suspendedOverlay_ = new HTML();
      suspendedOverlay_.setSize("100%", "100%");
      updateSuspendedOverlayStyle();
      createSuspendedNotice();

      // Add toolbar and frame to main panel
      mainPanel.add(toolbar);
      mainPanel.setWidgetTopHeight(toolbar, 0, Unit.PX, 29, Unit.PX);
      mainPanel.setWidgetLeftRight(toolbar, 0, Unit.PX, 0, Unit.PX);

      mainPanel.add(frame_);
      mainPanel.setWidgetTopBottom(frame_, 29, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);

      // Write HTML content after frame is attached to the DOM.
      // Cannot use setOnLoadAction + setUrl("about:blank") here because
      // the load event for about:blank doesn't fire reliably on an
      // unattached iframe.
      if (contentHtml != null)
      {
         final String html = contentHtml;
         Scheduler.get().scheduleDeferred(() -> frame_.setFrameContent(html));
      }

      // Manage suspend overlay across session lifecycle
      pEventBus_.get().addHandler(
         SessionSerializationEvent.TYPE,
         event ->
         {
            int action = event.getAction().getType();
            if (action == SessionSerializationAction.SUSPEND_SESSION)
            {
               showSuspendOverlay();
            }
            else if (action == SessionSerializationAction.RESUME_SESSION)
            {
               hideSuspendOverlay();
            }
         });

      pEventBus_.get().addHandler(
         ChatSuspendedNoticeEvent.TYPE,
         event -> showSuspendedNotice(event.getNotice()));
   }

   private void createSuspendedNotice()
   {
      ChatPane.Styles styles = ChatPane.RES.styles();

      DecorativeImage icon = new DecorativeImage(
         new ImageResource2x(MessageDialogImages.INSTANCE.dialog_info2x()));
      icon.setPixelSize(24, 24);
      icon.getElement().getStyle().setProperty("flexShrink", "0");

      suspendedNoticeLabel_ = new Label();
      suspendedNoticeLabel_.setStyleName(styles.chatUpdateMessage());
      Roles.getStatusRole().set(suspendedNoticeLabel_.getElement());

      FlowPanel content = new FlowPanel();
      content.setStyleName(styles.chatNotificationContent());
      content.add(icon);
      content.add(suspendedNoticeLabel_);

      suspendedNotice_ = new FlowPanel();
      suspendedNotice_.setStyleName(styles.chatUpdateNotification());
      suspendedNotice_.add(content);
   }

   // Shown over the top of the dimmed frame, as the frame is inactive until
   // the session resumes and the chat page is reloaded
   private void showSuspendedNotice(String notice)
   {
      suspendedNoticeLabel_.setText(notice);

      LayoutPanel panel = getMainPanel();
      if (suspendedNotice_.getParent() != panel)
      {
         panel.add(suspendedNotice_);
         panel.setWidgetLeftRight(suspendedNotice_, 0, Unit.PX, 0, Unit.PX);
         panel.getWidgetContainerElement(suspendedNotice_).getStyle().setZIndex(1);
      }

      // Lay out at a minimal height so the content overflows, then measure it
      panel.setWidgetTopHeight(suspendedNotice_, 29, Unit.PX, 1, Unit.PX);
      panel.forceLayout();
      Scheduler.get().scheduleDeferred(() ->
      {
         if (suspendedNotice_.getParent() != panel)
            return;
         int height = suspendedNotice_.getElement().getScrollHeight();
         panel.setWidgetTopHeight(suspendedNotice_, 29, Unit.PX, height, Unit.PX);
      });
   }

   private void hideSuspendedNotice()
   {
      LayoutPanel panel = getMainPanel();
      if (suspendedNotice_.getParent() == panel)
         panel.remove(suspendedNotice_);
   }

   private void updateSuspendedOverlayStyle()
   {
      suspendedOverlay_.getElement().getStyle().setBackgroundColor(
         ThemeColorExtractor.getEditorBackgroundColor("#fff"));
   }

   private void showSuspendOverlay()
   {
      updateSuspendedOverlayStyle();
      suspendedOverlay_.getElement().getStyle().setOpacity(0.5);

      LayoutPanel panel = getMainPanel();
      if (suspendedOverlay_.getParent() != panel)
      {
         panel.add(suspendedOverlay_);
         panel.setWidgetTopBottom(suspendedOverlay_, 29, Unit.PX, 0, Unit.PX);
         panel.setWidgetLeftRight(suspendedOverlay_, 0, Unit.PX, 0, Unit.PX);
      }
   }

   private void hideSuspendOverlay()
   {
      hideSuspendedNotice();

      LayoutPanel panel = getMainPanel();
      if (suspendedOverlay_.getParent() == panel)
      {
         panel.remove(suspendedOverlay_);
      }
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
      hideSuspendOverlay();

      if (params != null && frame_ != null)
      {
         ChatSatelliteParams chatParams = params.cast();
         String contentHtml = chatParams.getContentHtml();
         if (contentHtml != null)
         {
            // Write directly — doc.open()/write()/close() replaces
            // any existing document content and stops running scripts.
            frame_.setFrameContent(contentHtml);
         }
         else
         {
            String chatUrl = chatParams.getChatUrl();
            frame_.setOnLoadAction(null);
            frame_.setUrl(chatUrl);
         }
      }
   }

   @Override
   public boolean supportsThemes()
   {
      return true;
   }

   @Override
   public Widget getWidget()
   {
      return this;
   }

   private native void setupMessageForwarder() /*-{
      var self = this;
      $wnd.addEventListener('message', function(event) {
         var frame = self.@org.rstudio.studio.client.workbench.views.chat.ChatSatelliteWindow::getFrameElement()();
         if (!frame || event.source !== frame.contentWindow) return;

         var recognized = [
            'install-now', 'remind-later', 'restart-backend',
            'open-global-options', 'retry-manifest'
         ];
         if (recognized.indexOf(event.data) !== -1) {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatSatelliteWindow::onIframeAction(Ljava/lang/String;)(event.data);
         }
      });
   }-*/;

   private com.google.gwt.dom.client.Element getFrameElement()
   {
      return (frame_ != null) ? frame_.getElement() : null;
   }

   private void onIframeAction(String action)
   {
      pEventBus_.get().fireEvent(new ChatSatelliteActionEvent(action));
   }

   private RStudioThemedFrame frame_;
   private HTML suspendedOverlay_;
   private FlowPanel suspendedNotice_;
   private Label suspendedNoticeLabel_;
   private final Provider<EventBus> pEventBus_;
   private final Commands commands_;
   private final ApplicationCommandManager appCommandManager_;

   private static final ChatConstants constants_ =
      GWT.create(ChatConstants.class);
}
