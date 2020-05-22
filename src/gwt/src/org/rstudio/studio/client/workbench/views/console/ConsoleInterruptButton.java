/*
 * ConsoleInterruptButton.java
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.layout.DelayFadeInHelper;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.NotebookQueueState;

public class ConsoleInterruptButton extends Composite
{
   @Inject
   public ConsoleInterruptButton(final EventBus events,
                                 Commands commands)
   {
      fadeInHelper_ = new DelayFadeInHelper(this);

      // The SimplePanel wrapper is necessary for the toolbar button's "pushed"
      // effect to work.
      SimplePanel panel = new SimplePanel();
      panel.getElement().getStyle().setPosition(Position.RELATIVE);

      commands_ = commands;
      AppCommand interruptCommand = commands_.interruptR();
      ImageResource icon = interruptCommand.getImageResource();
      ToolbarButton button = new ToolbarButton(
            ToolbarButton.NoText,
            interruptCommand.getTooltip(),
            icon,
            interruptCommand);
      width_ = icon.getWidth() + 6;
      height_ = icon.getHeight();
      panel.setWidget(button);

      initWidget(panel);
      setVisible(false);

      events.addHandler(BusyEvent.TYPE, event ->
      {
         if (event.isBusy())
            events.fireEvent(new ConsoleBusyEvent(true));
      });
      
      events.addHandler(ConsoleBusyEvent.TYPE, event ->
      {
         if (event.isBusy())
            fadeInHelper_.beginShow();
         else
            fadeInHelper_.hide();
         commands_.interruptR().setEnabled(event.isBusy());
      });

      /*
      JJ says:
      It is possible that the client could miss the busy = false event (if the
      client goes out of network coverage and then the server suspends before
      it can come back into coverage). For this reason I think that the icon's
      controller logic should subscribe to the ConsolePromptEvent and clear it
      whenever a prompt occurs.
      */
      events.addHandler(ConsolePromptEvent.TYPE, event ->
      {
         // if any notebook is currently feeding the console, wait for it
         // to complete
         if (NotebookQueueState.anyQueuesExecuting())
            return;

         events.fireEvent(new ConsoleBusyEvent(false));
      });
   }

   public int getWidth()
   {
      return width_;
   }

   public int getHeight()
   {
      return height_;
   }

   private final DelayFadeInHelper fadeInHelper_;
   private final int width_;
   private final int height_;
   private final Commands commands_;
}
