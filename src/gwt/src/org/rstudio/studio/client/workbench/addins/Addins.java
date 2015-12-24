package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.AddinsMRUList;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

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
                                               boolean interactive,
                                               String binding)
      /*-{
         return {
            "name": name,
            "package": pkg,
            "title": title,
            "description": description,
            "interactive": interactive,
            "binding": binding
         };
      }-*/;
      
      public final native String getName() /*-{ return this["name"]; }-*/;
      public final native String getPackage() /*-{ return this["package"]; }-*/;
      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getDescription() /*-{ return this["description"]; }-*/;
      public final native boolean isInteractive() /*-{ return this["interactive"]; }-*/;
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
               addin.isInteractive() ? "true" : "false",
               addin.getBinding());
      }
      
      public final static RAddin decode(String decoded)
      {
         String[] splat = decoded.split(PATTERN);
         if (splat.length != 6)
         {
            Debug.log("Unexpected RAddin format");
            return RAddin.create();
         }
         
         return RAddin.create(
               splat[0],
               splat[1],
               splat[2],
               splat[3],
               splat[4] == "false" ? false : true,
               splat[5]);
      }
   }
   
   public static class RAddins extends JsMap<RAddin>
   {
      protected RAddins() {}
   }
   
   public static class AddinExecutor
   {
      public AddinExecutor()
      {
      }
      
      @Inject
      private void initialize(AddinsServerOperations server,
                              EventBus events,
                              AddinsMRUList mruList,
                              DependencyManager dependencyManager)
      {
         server_ = server;
         events_ = events;
         mruList_ = mruList;
         dependencyManager_ = dependencyManager;
      }
      
      public void execute(final RAddin addin)
      {
         if (!injected_)
         {
            RStudioGinjector.INSTANCE.injectMembers(this);
            injected_ = true;
         }
         
         if (addin.isInteractive())
         {
            dependencyManager_.withShinyAddins(new Command() {

               @Override
               public void execute()
               {
                  String code = addin.getPackage() + ":::" + addin.getBinding() + "()";
                  events_.fireEvent(new SendToConsoleEvent(code, true, false, false));
               }
            });
         }
         else
         {
            server_.executeRAddinNonInteractively(
                  addin.getId(),
                  new SimpleRequestCallback<Void>("Error Executing Addin", true));
         }
         
         mruList_.add(addin);
      }
      
      private boolean injected_ = false;
      
      // Injected ----
      private AddinsServerOperations server_;
      private EventBus events_;
      private AddinsMRUList mruList_;
      private DependencyManager dependencyManager_;
   }
   
   private static final String DELIMITER = "|||";
   private static final String PATTERN = "\\|\\|\\|";
}
