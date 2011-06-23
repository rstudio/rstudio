package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class StatusAndPath extends JavaScriptObject
{
   protected StatusAndPath()
   {}

   public native final String getStatus() /*-{
      return this.status;
   }-*/;

   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getRawPath() /*-{
      return this.raw_path;
   }-*/;
}
