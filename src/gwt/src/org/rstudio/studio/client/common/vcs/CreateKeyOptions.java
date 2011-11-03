package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class CreateKeyOptions extends JavaScriptObject
{
   protected CreateKeyOptions()
   {
   }
   
   public native static final CreateKeyOptions create(String path,
                                                      String type,
                                                      String passphrase) /*-{
      var options = new Object();
      options.path = path;
      options.type = type;
      options.passphrase = passphrase;                              
      return options;
   }-*/;
   
   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;
   
   public native final String getPassphrase() /*-{
      return this.passphrase;
   }-*/;   
}
