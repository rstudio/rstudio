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

import org.rstudio.core.client.theme.ThemeColorExtractor;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.chat.events.ChatReturnToMainEvent;
import org.rstudio.studio.client.workbench.views.chat.events.ChatSatelliteActionEvent;
import org.rstudio.studio.client.workbench.views.chat.model.ChatSatelliteParams;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
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
                               Commands commands)
   {
      super(pEventBus, pFSManager);
      pEventBus_ = pEventBus;
      commands_ = commands;
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

      // Create suspend overlay (shown during session suspend to block interaction)
      suspendedOverlay_ = new HTML();
      suspendedOverlay_.setSize("100%", "100%");
      updateSuspendedOverlayStyle();

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
         var recognized = [
            'install-now', 'remind-later', 'restart-backend',
            'open-global-options'
         ];
         if (recognized.indexOf(event.data) !== -1) {
            self.@org.rstudio.studio.client.workbench.views.chat.ChatSatelliteWindow::onIframeAction(Ljava/lang/String;)(event.data);
         }
      });
   }-*/;

   private void onIframeAction(String action)
   {
      pEventBus_.get().fireEvent(new ChatSatelliteActionEvent(action));
   }

   private RStudioThemedFrame frame_;
   private HTML suspendedOverlay_;
   private final Provider<EventBus> pEventBus_;
   private final Commands commands_;

   private static final ChatConstants constants_ =
      GWT.create(ChatConstants.class);
}
