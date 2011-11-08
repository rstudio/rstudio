package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectOptions extends JavaScriptObject
{
   protected RProjectOptions()
   {
   }
   
   public static final RProjectOptions createEmpty()
   {
      return create(RProjectConfig.createEmpty(), 
                    RProjectVcsOptions.createEmpty());
   }
   
   public native static final RProjectOptions create(
                                           RProjectConfig config,
                                           RProjectVcsOptions vcsOptions) /*-{
      var options = new Object();
      options.config = config;
      options.vcs_options = vcsOptions;
      options.vcs_options_default = new Object();
      return options;
   }-*/;
   
   public native final RProjectConfig getConfig() /*-{
      return this.config;
   }-*/;
   
   public native final RProjectVcsOptions getVcsOptions() /*-{
      return this.vcs_options;
   }-*/;

   public native final RProjectVcsOptionsDefault getVcsOptionsDefault() /*-{
      return this.vcs_options_default;
   }-*/;
}
