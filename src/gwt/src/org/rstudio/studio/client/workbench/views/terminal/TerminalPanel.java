/*
 * TerminalPanel.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

/**
 * Panel to hold a TerminalSession (xterm3) widget. Xterm3 requires a parent element
 * already visible in the DOM (has dimensions) before it can be created.
 */
public class TerminalPanel extends SimpleLayoutPanel
{
   public TerminalPanel()
   {
      setStyleName(ConsoleResources.INSTANCE.consoleStyles().console());
      getElement().setTabIndex(0);
      getElement().getStyle().setMargin(0, Unit.PX);
      getElement().addClassName(ThemeStyles.INSTANCE.selectableText());
      getElement().addClassName("ace_editor");
   }

   public void setTerminalSession(TerminalSession session)
   {
      super.add(session);
   }

   public TerminalSession getTerminalSession()
   {
      Widget session = getWidget();
      if (session != null)
      {
         return (TerminalSession) session;
      }
      return null;
   }

}
