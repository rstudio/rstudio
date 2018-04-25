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
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

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
      return toolbar;
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

   public void outputCompleted()
   {
   }
   
   GridViewerFrame gridViewer_;
}