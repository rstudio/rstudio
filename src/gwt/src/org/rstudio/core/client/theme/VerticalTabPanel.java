/*
 * VerticalTabPanel.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.rstudio.core.client.ElementIds;

/**
 * A vertical layout panel marked up as an Aria tab panel.
 */
public class VerticalTabPanel extends VerticalPanel
{
   public VerticalTabPanel(String basePanelId)
   {
      basePanelId_ = basePanelId;
      Roles.getTabpanelRole().set(getElement());
      Roles.getTabpanelRole().setAriaLabelledbyProperty(getElement(),
            Id.of(ElementIds.getElementId(getTabId(basePanelId))));
      ElementIds.assignElementId(getElement(), getTabPanelId(basePanelId));
   }
   
   public String getBasePanelId()
   {
      return basePanelId_;
   }

   public static String getTabPanelId(String basePanelId)
   {
      return basePanelId + "_panel";
   }

   public static String getTabId(String basePanelId)
   {
      return basePanelId + "_tab";
   }
   
   /* ID used to generate both tab and panel ids */
   private String basePanelId_;
}
