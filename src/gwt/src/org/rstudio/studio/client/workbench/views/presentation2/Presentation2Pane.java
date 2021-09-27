/*
 * Presentation2Pane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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



package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.widget.HorizontalCenterPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Presentation2Pane extends WorkbenchPane implements Presentation2.Display
{
   @Inject
   public Presentation2Pane()
   {
      super("Presentation");
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Presentation toolbar");
      return toolbar;
   }
   
   
   @Override
   protected Widget createMainWidget()
   {
      Label label = new Label("Under Construction");
      label.getElement().getStyle().setColor("#888");
      return new HorizontalCenterPanel(label, 100);
   }

}
