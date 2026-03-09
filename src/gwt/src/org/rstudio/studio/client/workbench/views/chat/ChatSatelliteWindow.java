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

import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.chat.events.ChatReturnToMainEvent;
import org.rstudio.studio.client.workbench.views.chat.model.ChatSatelliteParams;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
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
      String chatUrl = chatParams.getChatUrl();

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
      frame_.setUrl(chatUrl);

      // Add toolbar and frame to main panel
      mainPanel.add(toolbar);
      mainPanel.setWidgetTopHeight(toolbar, 0, Unit.PX, 29, Unit.PX);
      mainPanel.setWidgetLeftRight(toolbar, 0, Unit.PX, 0, Unit.PX);

      mainPanel.add(frame_);
      mainPanel.setWidgetTopBottom(frame_, 29, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
      if (params != null)
      {
         ChatSatelliteParams chatParams = params.cast();
         String chatUrl = chatParams.getChatUrl();
         if (frame_ != null)
         {
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

   private RStudioThemedFrame frame_;
   private final Provider<EventBus> pEventBus_;
   private final Commands commands_;

   private static final ChatConstants constants_ =
      GWT.create(ChatConstants.class);
}
