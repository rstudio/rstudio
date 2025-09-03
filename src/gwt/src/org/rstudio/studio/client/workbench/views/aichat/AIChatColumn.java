/*
 * AIChatColumn.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.aichat;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

@Singleton
public class AIChatColumn
{
   @Inject
   public AIChatColumn(EventBus events,
                       Session session,
                       UserPrefs userPrefs,
                       Commands commands)
   {
      events_ = events;
      session_ = session;
      userPrefs_ = userPrefs;
      commands_ = commands;
      
      // Create the display
      display_ = new AIChatPanel();
   }
   
   public Widget asWidget()
   {
      return display_.asWidget();
   }
   
   public void setVisible(boolean visible)
   {
      display_.setVisible(visible);
      visible_ = visible;
   }
   
   public boolean isVisible()
   {
      return visible_;
   }
   
   public void focus()
   {
      if (visible_)
         display_.focus();
   }
   
   public void onResize()
   {
      display_.onResize();
   }
   
   public int getPreferredWidth()
   {
      // Default width for the AI Chat pane (can be adjusted via preferences)
      return 400;
   }
   
   public void setWidth(int width)
   {
      display_.setWidth(width);
   }
   
   public int getWidth()
   {
      return display_.getWidth();
   }
   
   public AIChatDisplay getDisplay()
   {
      return display_;
   }
   
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs userPrefs_;
   private final Commands commands_;
   private final AIChatDisplay display_;
   private boolean visible_ = false;
}