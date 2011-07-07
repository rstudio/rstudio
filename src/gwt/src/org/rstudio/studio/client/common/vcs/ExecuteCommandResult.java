package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class ExecuteCommandResult extends JavaScriptObject
{
   protected ExecuteCommandResult()
   {}

   public native final String getOutput() /*-{
      return this.output;
   }-*/;

   public final boolean isError()
   {
      return isErrorNative() != 0;
   }

   private native int isErrorNative() /*-{
      return this.error ? 1 : 0;
   }-*/;
}
