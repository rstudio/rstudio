package org.rstudio.studio.client.workbench.views.vcs;

import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class VCSTab extends DelayLoadWorkbenchTab<VCS>
{
   public abstract static class VCSShim extends DelayLoadTabShim<VCS, VCSTab>
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
