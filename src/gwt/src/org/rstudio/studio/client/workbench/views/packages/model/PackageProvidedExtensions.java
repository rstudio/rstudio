package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.packages.events.PackageExtensionIndexingCompletedEvent;
import org.rstudio.studio.client.projects.events.ProjectTemplateRegistryUpdatedEvent;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PackageProvidedExtensions
      implements SessionInitHandler,
                 PackageExtensionIndexingCompletedEvent.Handler

{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native RAddins getAddinsRegistry()
      /*-{
         return this["addins_registry"];
      }-*/;
      
      public final native ProjectTemplateRegistry getProjectTemplateRegistry()
      /*-{
         return this["project_templates_registry"];
      }-*/;
   }
   
   @Inject
   public PackageProvidedExtensions(Session session,
                                    EventBus events)
   {
      session_ = session;
      events_ = events;
      
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(PackageExtensionIndexingCompletedEvent.TYPE, this);
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      update(session_.getSessionInfo().getPackageProvidedExtensions());
   }
   
   @Override
   public void onPackageExtensionIndexingCompleted(PackageExtensionIndexingCompletedEvent event)
   {
      update(event.getData());
   }
   
   private void update(PackageProvidedExtensions.Data data)
   {
      // update addins
      RAddins addinRegistry = data.getAddinsRegistry();
      if (addinRegistry != null)
         events_.fireEvent(new AddinRegistryUpdatedEvent(addinRegistry));

      // update project templates
      ProjectTemplateRegistry ptRegistry = data.getProjectTemplateRegistry();
      if (ptRegistry != null)
         events_.fireEvent(new ProjectTemplateRegistryUpdatedEvent(ptRegistry));
   }
   
   // Injected ----
   private final Session session_;
   private final EventBus events_;
}
