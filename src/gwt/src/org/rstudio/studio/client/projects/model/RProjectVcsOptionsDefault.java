package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectVcsOptionsDefault extends JavaScriptObject
{
   protected RProjectVcsOptionsDefault()
   {
   }

   public native final String getActiveVcs() /*-{
      return this.active_vcs;
   }-*/;
   
   public native final String getSshKeyPath() /*-{
      return this.ssh_key_path;
   }-*/;
}
