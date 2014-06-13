/*
 * ActionWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.packages.ui.actions;

import org.rstudio.studio.client.workbench.views.packages.Packages;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class ActionWidget extends Composite
{
   public ActionWidget(final Packages.Action action)
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.addStyleName(STYLES.actionWidget());
      
      Label label = new Label(action.getMessage());
      label.setWordWrap(false);
      label.addStyleName(STYLES.actionLabel());
      panel.add(label);
      
      ActionButton button = new ActionButton(action.getButtonText());
      button.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            action.getOnExecute().execute();
         }
      });
      panel.add(button);
      
      initWidget(panel);
   }
   
   ActionCenter.Styles STYLES = ActionCenter.RESOURCES.styles();
}
