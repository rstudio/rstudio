package org.rstudio.studio.client.panmirror;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.inject.Inject;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIDisplay {
   
  
   public PanmirrorUIDisplay() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      this.globalDisplay_ = globalDisplay;
   }
   
   public void openURL(String url) 
   {
      globalDisplay_.openWindow(url);
   }
   
   private GlobalDisplay globalDisplay_;
}
