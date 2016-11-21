/*
 * TerminalSessionsPanel.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.studio.client.common.shell.ShellSecureInput;
import com.google.gwt.user.client.ui.DeckLayoutPanel;

/**
 * A panel holding zero or more TerminalSession objects; one is visible at a time.
 */
public class TerminalSessionsPanel extends DeckLayoutPanel
{
   /**
    * @return Number of terminal sessions
    */
   public int getTerminalCount()
   {
      return getWidgetCount();
   }
  
   /**
    * Create a new terminal session.
    */
   public void createTerminal()
   {
      if (secureInput_ == null)
      {
         secureInput_ = new ShellSecureInput();  
      }
      
      TerminalSession newSession = new TerminalSession(secureInput_);
      newSession.connect();
      add(newSession);
      showWidget(newSession);
   }
   
   private ShellSecureInput secureInput_;
}
