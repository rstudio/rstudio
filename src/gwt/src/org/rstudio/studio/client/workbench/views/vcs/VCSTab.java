/*
 * VCSTab.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class VCSTab extends DelayLoadWorkbenchTab<GitPresenter>
{
   public abstract static class VCSShim extends DelayLoadTabShim<GitPresenter, VCSTab>
   {
   }

   @Inject
   protected VCSTab(VCSShim shim, Session session)
   {
      super(session.getSessionInfo().getVcsName(), shim);
      session_ = session;
   }

   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().isVcsEnabled();
   }

   private final Session session_;
}
