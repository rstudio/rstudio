package org.rstudio.studio.client.workbench.views.connections.model;

import com.google.gwt.core.client.JavaScriptObject;

// extends JavaScriptObject for easy serialization (as client state)
public class ConnectionOptions extends JavaScriptObject
{
   protected ConnectionOptions() {}
   
   public static final ConnectionOptions create()
   {
      return create(null, false, null, null, null);
   }
   
   public static final native ConnectionOptions create(
                                            String master,
                                            boolean remote,
                                            SparkVersion sparkVersion,
                                            String connectCode,
                                            String connectVia)
   /*-{
      return {
         "master": master,
         "remote": remote,
         "spark_version": sparkVersion,
         "connect_code": connectCode,
         "connect_via": connectVia
      };
   }-*/;
   
   public final native String getMaster() /*-{ return this.master; }-*/;
   public final native boolean getRemote() /*-{ return this.remote; }-*/;
   public final native SparkVersion getSparkVersion() /*-{ return this.spark_version; }-*/;
   public final native String getConnectCode() /*-{ return this.connect_code; }-*/;
   public final native String getConnectVia() /*-{ return this.connect_via; }-*/;
   
   public static String CONNECT_R_CONSOLE = "connect-r-console";
   public static String CONNECT_NEW_R_SCRIPT = "connect-new-r-script";
   public static String CONNECT_NEW_R_NOTEBOOK = "connect-new-r-notebook";
   public static String CONNECT_COPY_TO_CLIPBOARD = "connect-copy-to-clipboard";
    
   public static String MASTER_LOCAL = NewSparkConnectionContext.MASTER_LOCAL;
   
   public final static String DB_INTERFACE_DPLYR = "dplyr";
   public final static String DB_INTERFACE_NONE = "(None)";
}