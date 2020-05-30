/*
 * HistoryBranchToolbarButton.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

public class HistoryBranchToolbarButton extends BranchToolbarButton
{
   @Inject
   public HistoryBranchToolbarButton(final Provider<GitState> vcsState)
   {
      super(vcsState);
   }

   @Override
   public void onVcsRefresh(VcsRefreshEvent event)
   {
      super.onVcsRefresh(event);

      // one time initialization of our caption (need to do it here
      // because vcsState.getBranchInfo is null when we are created)
      if (!initialized_)
      {
         initialized_ = true;
         setBranchCaption(pVcsState_.get().getBranchInfo().getActiveBranch());
      }
   }

   @Override
   protected void onBeforePopulateMenu(ToolbarPopupMenu rootMenu)
   {
      super.onBeforePopulateMenu(rootMenu);
      
      String label = "(all branches)";
      rootMenu.addItem(
            new MenuItem(label, new SwitchBranchCommand(label, "--all")));
   }

   private boolean initialized_ = false;

}
