/*
 * DataOutputPane.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.data;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.data.events.DataOutputCompletedEvent;

public class DataOutputPane extends WorkbenchPane
      implements DataOutputPresenter.Display
{
   @Inject
   public DataOutputPane()
   {
      super("Data Output");
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   { 
      gridViewer_ = new GridViewerFrame();
      return gridViewer_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      dataOutputFile_ = new Label();
      dataOutputFile_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(dataOutputFile_);

      return toolbar;
   }

   public void setDataFile(String dataFile)
   {
      dataOutputFile_.setText(dataFile);
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }

   public void outputStarted(String fileName)
   {
   }

   public void clearAll()
   {
   }
   
   public void showOutput()
   {
   }

   public void outputCompleted(final DataOutputCompletedEvent response)
   {
      setDataFile(response.getTitle());

      new Timer() {
         @Override
         public void run()
         {
            if (!gridViewer_.isReady())
            {
               this.schedule(200);
            }
            else
            {
               gridViewer_.setOption("nullsAsNAs", "true");
               gridViewer_.setOption("ordering", "false");
               gridViewer_.setOption("rowNumbers", "false");
               gridViewer_.setData(response.getData());
            }
         }
      }.schedule(100);
   }
   
   GridViewerFrame gridViewer_;
   Label dataOutputFile_;
}