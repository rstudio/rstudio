package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.core.client.JavaScriptObject;

import org.rstudio.core.client.js.JsMap;

public class Addins
{
   public static class RAddin extends JavaScriptObject
   {
      protected RAddin() {}
      
      public final native String getName() /*-{ return this["name"]; }-*/;
      public final native String getPackage() /*-{ return this["package"]; }-*/;
      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getDescription() /*-{ return this["description"]; }-*/;
      public final native String getBinding() /*-{ return this["binding"]; }-*/;
      
      public final String getId()
      {
         return getPackage() + "::" + getBinding();
      }
   }
   
   public static class RAddins extends JsMap<RAddin>
   {
      protected RAddins() {}
   }
}
