/*
 * BaseVcsPresenter.java
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

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.model.GitHubViewRequest;

public abstract class BaseVcsPresenter extends BasePresenter
{
   protected BaseVcsPresenter(WorkbenchView view)
   {
      super(view);
   }
   
   public abstract void onVcsCommit();

   public abstract void onVcsShowHistory();

   public abstract void onVcsPull();
   
   public abstract void onVcsPullRebase();

   public abstract void onVcsPush();
   
   public abstract void onVcsCleanup();
   
   public abstract void onVcsIgnore();
      
   public abstract void showHistory(FileSystemItem fileFilter);
   
   public abstract void showDiff(FileSystemItem file);
   
   public abstract void revertFile(FileSystemItem file);
   
   public abstract void viewOnGitHub(GitHubViewRequest viewRequest);
   
}
