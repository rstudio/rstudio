package org.rstudio.studio.client.panmirror.toolbar;

import org.rstudio.core.client.widget.SecondaryToolbar;

public class PanmirrorToolbar extends SecondaryToolbar
{
   public PanmirrorToolbar()
   {
      super(false, "Panmirror Editor Toolbar");
   }
   
   @Override
   public int getHeight()
   {
      return 22;
   }
   
   

}
