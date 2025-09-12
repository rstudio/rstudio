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

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ChatPane
      extends WorkbenchPane
      implements ChatPresenter.Display
{
   @Inject
   protected ChatPane(GlobalDisplay globalDisplay,
                      EventBus events,
                      Commands commands,
                      Session session,
                      ChatServerOperations server)
   {
      super(constants_.chatTitle(), events);

      globalDisplay_ = globalDisplay;
      commands_ = commands;
      session_ = session;
      server_ = server;

      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new LayoutPanel();
      mainPanel_.addStyleName("ace_editor_theme");
      VerticalPanel vpanel = new VerticalPanel();
      vpanel.setSize("100%", "100%");
      mainPanel_.add(vpanel);

      Label label = new Label("Chat coming soon!");
      vpanel.add(label);
      vpanel.setCellHorizontalAlignment(label, VerticalPanel.ALIGN_CENTER);
      vpanel.setCellVerticalAlignment(label, VerticalPanel.ALIGN_MIDDLE);
      mainPanel_.setWidgetTopHeight(vpanel, 0, Unit.PCT, 100, Unit.PCT);
      mainPanel_.setWidgetLeftWidth(vpanel, 0, Unit.PCT, 100, Unit.PCT);

      return mainPanel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.chatTabLabel());

      return toolbar_;
   }

   // Resources ----
   public interface Resources extends ClientBundle
   {
      @Source("ChatPane.css")
      CssResource styles();
   }


   private LayoutPanel mainPanel_;
   private Toolbar toolbar_;

   // Injected ----
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final Session session_;
   @SuppressWarnings("unused")
   private final ChatServerOperations server_;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
