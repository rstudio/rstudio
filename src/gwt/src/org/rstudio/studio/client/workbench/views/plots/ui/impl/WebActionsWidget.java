/*
 * WebActionsWidget.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.impl;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.rstudio.studio.client.workbench.views.plots.ui.ActionsWidget;
import org.rstudio.studio.client.workbench.views.plots.ui.ExportDialog;

public class WebActionsWidget extends ActionsWidget
{
   public WebActionsWidget()
   {
      // actions panel
      VerticalPanel actionsPanel = new VerticalPanel();
      actionsPanel.setSpacing(7);
      actionsPanel.setStylePrimaryName(
            ExportDialog.RESOURCES.styles().actionsPanel());

      HTML clipboardHTML = new HTML(
            "<b>Export to Clipboard</b>: Right-click the Plot and select Copy Image");
      actionsPanel.add(clipboardHTML);

      downloadHTML_ = new HTML();
      actionsPanel.add(downloadHTML_);

      initWidget(actionsPanel);
   }

   @Override
   public void onPlotChanged(String plotDownloadUrl, int width, int height)
   {
      downloadHTML_.setHTML(
            "<b>Export to Local File</b>: <a href=\"" + plotDownloadUrl + 
            "\" target=\"_blank\">Download PNG</a>");
   }

   @Override
   public boolean shouldPositionOnTopRight()
   {
      return false;
   }

   private HTML downloadHTML_;
}
