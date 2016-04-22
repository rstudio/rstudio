/*
 * NotebookProgressWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class NotebookProgressWidget extends Composite
{

   private static NotebookProgressWidgetUiBinder uiBinder = GWT
         .create(NotebookProgressWidgetUiBinder.class);

   interface NotebookProgressWidgetUiBinder
         extends UiBinder<Widget, NotebookProgressWidget>
   {
   }
   
   public void setPercent(int percent, String chunkName)
   {
      String color = "24, 163, 82";
      progressBar_.getElement().getStyle().setBackgroundImage(
            "linear-gradient(to right, " +
              "rgba(" + color + ", 1.0), " +
              "rgba(" + color + ", 1.0) " + percent + "%, " +
              "rgba(" + color + ", 0.3) " + percent + "%, " +
              "rgba(" + color + ", 0.3) 100%");
      
      chunkName_.setText(chunkName);
   }

   public NotebookProgressWidget()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   @UiField HTMLPanel progressBar_;
   @UiField Anchor chunkName_;
}
