package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class LaunchShinyGadgetDialog extends ModalDialogBase
{

   @Override
   protected Widget createMainWidget()
   {
      return new FlowPanel();
   }

}
