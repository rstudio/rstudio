package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.VCS.Display;

public class VCSPane extends WorkbenchPane implements Display
{
   @Inject
   public VCSPane(Session session)
   {
      super(session.getSessionInfo().getVcsName());
   }

   @Override
   protected Widget createMainWidget()
   {
      return new HTML();
   }
}
