/*
 * BranchToolbarButton.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.model.VcsState;

public class BranchToolbarButton extends ToolbarButton
{
   @Inject
   public BranchToolbarButton(final VcsState vcsState,
                              final VCSServerOperations server)
   {
      super("",
            StandardIcons.INSTANCE.empty_command(),
            new ToolbarPopupMenu());

      setTitle("Switch branch");

      vcsState.bindRefreshHandler(this, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            String activeBranch = vcsState.getBranchInfo()
                  .getActiveBranch();
            if (activeBranch == null)
               activeBranch = "(No branch)";
            setText(activeBranch);

            ToolbarPopupMenu menu = getMenu();
            menu.clearItems();
            JsArrayString branches = vcsState.getBranchInfo()
                  .getBranches();
            for (int i = 0; i < branches.length(); i++)
            {
               final String branch = branches.get(i);
               menu.addItem(new MenuItem(branch, new Command()
               {
                  @Override
                  public void execute()
                  {
                     server.vcsCheckout(branch,
                                        new SimpleRequestCallback<Void>());
                  }
               }));
            }
         }
      });
   }
}
