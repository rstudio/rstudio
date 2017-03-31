/*
 * ConsoleClearButton.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;

public class ConsoleClearButton extends Composite
{
   @Inject
   public ConsoleClearButton(final EventBus events, Commands commands)
   {
      // The SimplePanel wrapper is necessary for the toolbar button's "pushed"
      // effect to work.
      SimplePanel panel = new SimplePanel();
      panel.getElement().getStyle().setPosition(Position.RELATIVE);

      commands_ = commands;
      ImageResource icon = commands_.consoleClear().getImageResource();
      ToolbarButton button = new ToolbarButton(icon, commands.consoleClear());
      width_ = icon.getWidth() + 6;
      height_ = icon.getHeight();
      panel.setWidget(button);

      initWidget(panel);
      setVisible(true);
   }

   public int getWidth()
   {
      return width_;
   }

   public int getHeight()
   {
      return height_;
   }
   
   private final int width_;
   private final int height_;
   private final Commands commands_;
}
