package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public class VCS extends BasePresenter implements Widgetable
{
   public interface Display extends WorkbenchView, Widgetable
   {
   }

   @Inject
   public VCS(Display view)
   {
      super(view);
      view_ = view;
   }

   @Override
   public Widget toWidget()
   {
      return view_.toWidget();
   }

   private final Display view_;
}
