/*
 * MarkersOutputPane.java
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
package org.rstudio.studio.client.workbench.views.output.markers;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersSet;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

public class MarkersOutputPane extends WorkbenchPane
      implements MarkersOutputPresenter.Display
{
   @Inject
   public MarkersOutputPane()
   {
      super("Markers");
      markerList_ = new SourceMarkerList();
      ensureWidget();
   }
   
   @Override
   public void initialize(MarkersState markerState)
   {
      if (markerState.isVisible())
         ensureVisible(false);
   }

   @Override
   public void showMarkersSet(MarkersSet markerSet)
   {
      markerList_.clear();
      markerList_.showMarkers(markerSet.getTargetFile(),
                              markerSet.getBasePath(),
                              markerSet.getMarkers(), 
                              markerSet.getAutoSelect());
   }


   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();


      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      return markerList_;
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return markerList_.addSelectionCommitHandler(handler);
   }

   
   private SourceMarkerList markerList_;
}
