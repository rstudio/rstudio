package org.rstudio.studio.client.panmirror;

import org.rstudio.core.client.CommandWithArg;

import com.google.gwt.core.client.JavaScriptObject;


public class PanmirrorEditor extends JavaScriptObject
{  
   
   protected PanmirrorEditor()
   {
   }
   
   public final native void destroy() /*-{
      return this.destroy();
   }-*/;

   public final native void setTitle(String title) /*-{
      this.setTitle(title);
   }-*/;
   
   public final native String getTitle() /*-{
      return this.getTitle();
   }-*/;

   public final native void setMarkdown(String markdown, boolean emitUpdate) /*-{
      this.setMarkdown(markdown, emitUpdate);
   }-*/;
   
   public final native void getMarkdown(CommandWithArg<String> command) /*-{
      this.getMarkdown().then($entry(function(markdown) {
         command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(markdown);
      }));
   }-*/;      
}
