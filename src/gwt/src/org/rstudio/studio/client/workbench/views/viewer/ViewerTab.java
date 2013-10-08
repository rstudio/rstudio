/*
 * ViewerTab.java
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

import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class ViewerTab extends DelayLoadWorkbenchTab<ViewerPresenter>
{
   public abstract static class Shim 
        extends DelayLoadTabShim<ViewerPresenter, ViewerTab> {}

   @Inject
   public ViewerTab(Shim shim, Session session)
   {
      super("Viewer", shim);
      session_ = session;
   }
   
   @SuppressWarnings("unused")
   private Session session_;
}