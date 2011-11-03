package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class CreateKeyResult extends JavaScriptObject
{
   protected CreateKeyResult()
   {
   }
   
   // check this value first to see if the operation failed
   // due to the key already existing
   public native final boolean getFailedKeyExists() /*-{
      return this.failed_key_exists;
   }-*/;
   
   public native final int getExitStatus() /*-{
      return this.exit_status;
   }-*/;

   public native final String getOutput() /*-{
      return this.output;
   }-*/; 
}
