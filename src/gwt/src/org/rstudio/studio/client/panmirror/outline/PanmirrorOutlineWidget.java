package org.rstudio.studio.client.panmirror.outline;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

public class PanmirrorOutlineWidget extends Composite
{
   public PanmirrorOutlineWidget()
   {
      initWidget(new Label(""));
      addStyleName(RES.styles().outline());
   }
   
   
   
   private final static PanmirrorOutlineResources RES = PanmirrorOutlineResources.INSTANCE;

}
