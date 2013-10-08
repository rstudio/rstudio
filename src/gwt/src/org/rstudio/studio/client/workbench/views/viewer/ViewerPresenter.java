/*
 * ViewerPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.inject.Inject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;

public class ViewerPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void navigate(String url);
   }
   
   @Inject
   public ViewerPresenter(Display display, EventBus eventBus)
   {
      super(display);
      display_ = display;
   }
   
   public void onViewerNavigate(ViewerNavigateEvent event)
   {
      display_.navigate(event.getURL());
   }
   
   private final Display display_ ;

   
}