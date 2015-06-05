package org.rstudio.studio.client.application.ui.impl;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class WebApplicationHeaderAddIns
{
   public interface Context
   {
      void addCommand(Widget widget);
      void addCommandSeparator();
   }
   
   @Inject
   public WebApplicationHeaderAddIns()
   {  
   }
   
   public void initialize(Context context)
   {
   }
}
