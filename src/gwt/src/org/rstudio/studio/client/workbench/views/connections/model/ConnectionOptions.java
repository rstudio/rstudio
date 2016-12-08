package org.rstudio.studio.client.workbench.views.connections.model;

import com.google.gwt.core.client.JavaScriptObject;

// extends JavaScriptObject for easy serialization (as client state)
public class ConnectionOptions extends JavaScriptObject
{
   protected ConnectionOptions() {}
   
   public static final ConnectionOptions create()
   {
      return create(null, null);
   }
   
   public static final native ConnectionOptions create(
                                            String connectCode,
                                            String connectVia)
   /*-{
      return {
         "connect_code": connectCode,
         "connect_via": connectVia
      };
   }-*/;
   
   public final native String getConnectCode() /*-{ return this.connect_code; }-*/;
   public final native String getConnectVia() /*-{ return this.connect_via; }-*/;
   
   public static String CONNECT_R_CONSOLE = "connect-r-console";
   public static String CONNECT_NEW_R_SCRIPT = "connect-new-r-script";
   public static String CONNECT_NEW_R_NOTEBOOK = "connect-new-r-notebook";
   public static String CONNECT_COPY_TO_CLIPBOARD = "connect-copy-to-clipboard";
}