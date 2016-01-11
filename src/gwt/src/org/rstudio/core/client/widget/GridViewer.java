/*
 * GridViewer.java
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

package org.rstudio.core.client.widget;

import java.util.Date;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportOptions;
import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.thirdparty.guava.common.util.concurrent.CycleDetectingLockFactory.WithExplicitOrdering;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.core.client.JsArray;

public class GridViewer extends Composite
{
   private static GridViewerUiBinder uiBinder = GWT
         .create(GridViewerUiBinder.class);

   interface GridViewerUiBinder extends UiBinder<Widget, GridViewer>
   {
   }

   public GridViewer()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setData(JsObject data)
   {
      gridViewerFrame_.setData(data);
   }
   
   @UiField
   GridViewerFrame gridViewerFrame_;
}
