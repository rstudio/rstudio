package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
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
                                               String binding,
                                               int ordinal)
      /*-{
         return {
            "name": name,
            "package": pkg,
            "title": title,
            "description": description,
            "interactive": interactive,
            "binding": binding,
            "ordinal": ordinal
         };
      }-*/;
      
      public final native String getName() /*-{ return this["name"]; }-*/;
      public final native String getPackage() /*-{ return this["package"]; }-*/;
      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getDescription() /*-{ return this["description"]; }-*/;
      public final native boolean isInteractive() /*-{ return this["interactive"]; }-*/;
      public final native String getBinding() /*-{ return this["binding"]; }-*/;
      public final native int getOrdinal() /*-{ return this["ordinal"] || 0; }-*/;
      
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
               addin.getBinding(),
               "" + addin.getOrdinal());
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
               splat[5],
               StringUtil.parseInt(splat[6], 0));
      }
   }
   
   public static class RAddins extends JsMap<RAddin>
   {
      protected RAddins() {}
      public static final native RAddins createDefault() /*-{ return {}; }-*/;
   }
   
   public static class AddinExecutor
   {
      public AddinExecutor()
      {
      }
      
      @Inject
      private void initialize(AddinsServerOperations server,
                              EventBus events,
                              GlobalDisplay globalDisplay,
                              WorkbenchContext workbenchContext,
                              DependencyManager dependencyManager)
      {
         server_ = server;
         events_ = events;
         globalDisplay_ = globalDisplay;
         workbenchContext_ = workbenchContext;
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
            // check if we are executing
            if (workbenchContext_.isServerBusy())
            {
               globalDisplay_.showMessage(
                  MessageDialog.WARNING, 
                  addin.getName(),
                  constants_.isServerBusyMessage(addin.getName()));
            }
            else
            {
               dependencyManager_.withShinyAddins(new Command() {
   
                  @Override
                  public void execute()
                  {
                     String code = addin.getPackage() + ":::" + 
                                   addin.getBinding() + "()";
                     
                     if (BrowseCap.isMacintoshDesktopMojave())
                     {
                        server_.prepareForAddin(new ServerRequestCallback<Void>()
                        {
                           @Override
                           public void onResponseReceived(Void response)
                           {
                              SendToConsoleEvent event = new SendToConsoleEvent(code, true, false, false);
                              events_.fireEvent(event);
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                           }
                        });
                     }
                     else
                     {
                        SendToConsoleEvent event = new SendToConsoleEvent(code, true, false, false);
                        events_.fireEvent(event);
                     }
                        
                  }
               });
            }
         }
         else
         {
            server_.executeRAddinNonInteractively(
                  addin.getId(),
                  new SimpleRequestCallback<>(constants_.executingAddinError(), true));
         }
      }
      
      private boolean injected_ = false;
      
      // Injected ----
      private AddinsServerOperations server_;
      private EventBus events_;
      private GlobalDisplay globalDisplay_;
      private WorkbenchContext workbenchContext_;
      private DependencyManager dependencyManager_;
   }
   
   private static final String DELIMITER = "|||";
   private static final String PATTERN = "\\|\\|\\|";
   private static final AddinsConstants constants_ = GWT.create(AddinsConstants.class);
}
