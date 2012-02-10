/*
 * CompilePdfOutputPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display
{
   @Inject
   public CompilePdfOutputPane()
   {
      super("Compile PDF");
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      SimplePanel panel = new SimplePanel();
      panel.setWidget(new Button("Close", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            fireEvent(new EnsureHiddenEvent());
         }
      }));
      return panel;
   }
}
