package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RPrefs extends JavaScriptObject
{
   protected RPrefs() {}

   public native final int getSaveAction() /*-{
      return this.save_action;
   }-*/;

   public native final boolean getLoadRData() /*-{
      return this.load_rdata;
   }-*/;

   public native final String getInitialWorkingDirectory() /*-{
      return this.initial_working_dir;
   }-*/;
}
