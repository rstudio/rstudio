package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectVcsOptions extends JavaScriptObject
{
   protected RProjectVcsOptions()
   {
   }
   
   public native static final RProjectVcsOptions createEmpty() /*-{
      var options = new Object();
      return options;
   }-*/;
   
   public native final String getActiveVcsOverride() /*-{
      return this.active_vcs_override;
   }-*/;
   
   public native final void setActiveVcsOverride(String override) /*-{
      this.active_vcs_override = override;
   }-*/;

   public native final String getSshKeyPathOverride() /*-{
      return this.ssh_key_path_override;
   }-*/;

   public native final void setSshKeyPathOverride(String override) /*-{
      this.ssh_key_path_override = override;
   }-*/;
   
}
