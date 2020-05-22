/*
 * TerminalHelper.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalBusyEvent;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TerminalHelper
{
   @Inject
   TerminalHelper(EventBus events,
                  GlobalDisplay globalDisplay)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;

      // track busy terminals
      events_.addHandler(TerminalBusyEvent.TYPE,
            event -> warnBeforeClosing_ = event.isBusy());
   }

   public boolean warnBeforeClosing(String busyMode)
   {
      if (busyMode == UserPrefs.BUSY_DETECTION_NEVER)
         warnBeforeClosing_ = false;
      
      return warnBeforeClosing_;
   }

   public void warnBusyTerminalBeforeCommand(final Command command, 
                                             String caption,
                                             String question,
                                             String busyMode)
   {
      if (!warnBeforeClosing(busyMode))
      {
         command.execute();
         return;
      }

      globalDisplay_.showYesNoMessage(
            MessageDialog.QUESTION,
            caption, 
            "The terminal is currently busy. " + question,
            command::execute,
            true);
   }

   private boolean warnBeforeClosing_;

   // Injected ----  
   private GlobalDisplay globalDisplay_;
   private EventBus events_;
 }
