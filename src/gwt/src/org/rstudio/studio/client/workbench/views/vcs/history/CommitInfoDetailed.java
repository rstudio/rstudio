package org.rstudio.studio.client.workbench.views.vcs.history;

public class CommitInfoDetailed extends CommitInfo
{
   protected CommitInfoDetailed() {}

   public native final String getDescription() /*-{
      return this.description;
   }-*/;
}
