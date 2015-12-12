package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.core.client.JavaScriptObject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsMap;

public class Addins
{
   public static class RAddin extends JavaScriptObject
   {
      protected RAddin() {}
      
      public static final native RAddin create() /*-{ return {}; }-*/;
      public static final native RAddin create(String name,
                                               String pkg,
                                               String title,
                                               String description,
                                               String binding)
      /*-{
         return {
            "name": name,
            "package": pkg,
            "title": title,
            "description": description,
            "binding": binding
         };
      }-*/;
      
      public final native String getName() /*-{ return this["name"]; }-*/;
      public final native String getPackage() /*-{ return this["package"]; }-*/;
      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getDescription() /*-{ return this["description"]; }-*/;
      public final native String getBinding() /*-{ return this["binding"]; }-*/;
      
      public final String getId()
      {
         return getPackage() + "::" + getBinding();
      }
      
      public final static String encode(RAddin addin)
      {
         return StringUtil.join(
               DELIMITER,
               addin.getName(),
               addin.getPackage(),
               addin.getTitle(),
               addin.getDescription(),
               addin.getBinding());
      }
      
      public final static RAddin decode(String decoded)
      {
         String[] splat = decoded.split(PATTERN);
         if (splat.length != 5)
         {
            Debug.log("Unexpected RAddin format");
            return RAddin.create();
         }
         
         return RAddin.create(
               splat[0],
               splat[1],
               splat[2],
               splat[3],
               splat[4]);
      }
   }
   
   public static class RAddins extends JsMap<RAddin>
   {
      protected RAddins() {}
   }
   
   private static final String DELIMITER = "|||";
   private static final String PATTERN = "\\|\\|\\|";
}
