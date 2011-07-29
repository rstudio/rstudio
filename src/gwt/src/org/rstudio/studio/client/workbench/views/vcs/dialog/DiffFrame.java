package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;

public class DiffFrame extends Composite
{
   interface Binder extends UiBinder<Widget, DiffFrame>
   {}

   public DiffFrame(ImageResource icon,
                    String filename1,
                    String filename2,
                    LineTableView diff)
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      headerLabel_.setText(filename1);
      container_.add(diff);
   }

   @UiField
   FlowPanel container_;
   @UiField
   Label headerLabel_;
}
