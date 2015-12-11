/*
 * AddinsMRUList.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import java.util.List;

@Singleton
public class AddinsMRUList extends MRUList
{
   @Inject
   public AddinsMRUList(final Commands commands,
                        final WorkbenchListManager listManager,
                        final EventBus events,
                        final Session session,
                        final GlobalDisplay display,
                        final AddinsServerOperations server)
   {
      super(
            listManager.getAddinsMruList(),
            new AppCommand[] {
               commands.addinsMru0(),
               commands.addinsMru1(),
               commands.addinsMru2(),
               commands.addinsMru3(),
               commands.addinsMru4(),
               commands.addinsMru5(),
               commands.addinsMru6(),
               commands.addinsMru7(),
               commands.addinsMru8(),
               commands.addinsMru9()
            },
            commands.clearAddinsMruList(),
            false,
            new OperationWithInput<String>()
            {
               @Override
               public void execute(final String encoded)
               {
                  final RAddin addin = RAddin.decode(encoded);
                  server.executeRAddin(
                        addin.getId(),
                        new SimpleRequestCallback<Void>("Error Executing Addin", true));
               }
            });
      
      events.addHandler(
            SessionInitEvent.TYPE,
            new SessionInitHandler()
            {
               @Override
               public void onSessionInit(SessionInitEvent event)
               {
                  // force initialization
                  listManager.getAddinsMruList();
               }
            });
      
      events.addHandler(
            AddinExecutionFailedEvent.TYPE,
            new AddinExecutionFailedEvent.Handler()
            {
               @Override
               public void onFailure(AddinExecutionFailedEvent event)
               {
                  remove(event.getData());
               }
            });
   }
   
   @Override
   protected void manageCommands(List<String> entries, AppCommand[] commands)
   {
      for (int i = 0; i < commands.length; i++)
      {
         if (i >= entries.size())
            commands[i].setVisible(false);
         else
         {
            String entry = entries.get(i);
            try
            {
               RAddin addin = RAddin.decode(entry);
               String label = addin.getName();
               String desc = addin.getDescription() +
                     " [" + addin.getPackage() + "::" + addin.getBinding() + "()]";
               commands[i].setVisible(true);
               commands[i].setMenuLabel(label);
               commands[i].setDesc(desc);
            }
            catch (Exception e)
            {
               Debug.logException(e);
               commands[i].setVisible(false);
            }
         }
      }
   }
   
   public static class AddinExecutionFailedEvent extends GwtEvent<AddinExecutionFailedEvent.Handler>
   {
      public AddinExecutionFailedEvent(String data) { data_ = data; }
      public String getData() { return data_; }
      private final String data_;
      
      // Boilerplate ----
      
      public interface Handler extends EventHandler
      {
         void onFailure(AddinExecutionFailedEvent event);
      }

      @Override
      public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
      {
         return TYPE;
      }
      
      @Override
      protected void dispatch(Handler handler)
      {
         handler.onFailure(this);
      }

      public static final Type<Handler> TYPE = new Type<Handler>();
   }
}
