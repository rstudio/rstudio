/*
 * BuildTab.java
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
package org.rstudio.studio.client.workbench.views.build;

import com.google.inject.Inject;

import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class BuildTab extends DelayLoadWorkbenchTab<Build>
{
   public abstract static class Shim extends DelayLoadTabShim<Build, BuildTab> {}

   @Inject
   public BuildTab(Shim shim, Session session)
   {
      super("Build", shim);
      session_ = session;
   }
   
   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().getBuildToolsEnabled();
   }

   private Session session_;
}
