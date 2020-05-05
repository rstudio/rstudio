package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.PanmirrorRmdChunk;
import org.rstudio.studio.client.panmirror.command.PanmirrorMenuItem;

import com.google.inject.Inject;

import jsinterop.annotations.JsFunction;
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

   public ShowContextMenu showContextMenu;   
   public ExecuteRmdChunk executeRmdChunk;
   
   @JsFunction
   public interface ShowContextMenu
   {
      void show(PanmirrorMenuItem[] items, int clientX, int clientY);
   }
   
   @JsFunction
   public interface ExecuteRmdChunk
   {
      void execute(PanmirrorRmdChunk chunk);
   }
   
   private GlobalDisplay globalDisplay_;
}
