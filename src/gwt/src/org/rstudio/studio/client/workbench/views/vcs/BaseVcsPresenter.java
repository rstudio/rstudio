package org.rstudio.studio.client.workbench.views.vcs;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public abstract class BaseVcsPresenter extends BasePresenter
{
   protected BaseVcsPresenter(WorkbenchView view)
   {
      super(view);
   }
   
   public abstract void onVcsCommit();

   public abstract void onVcsShowHistory();

   public abstract void onVcsPull();

   public abstract void onVcsPush();
   
   public abstract void onVcsCreateBranch();
   
   public abstract void showHistory(FileSystemItem fileFilter);
   
   public abstract void showDiff(FileSystemItem file);
   
   public abstract void revertFile(FileSystemItem file);
}
