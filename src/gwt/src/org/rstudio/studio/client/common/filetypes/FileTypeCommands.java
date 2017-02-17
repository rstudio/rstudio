/*
 * FileTypeCommands.java
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
package org.rstudio.studio.client.common.filetypes;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileTypeCommands
{
   public static class CommandWithId
   {
      private CommandWithId(String commandId, AppCommand command)
      {
         this.commandId = commandId;
         this.command = command;
      }

      public final String commandId;
      public final AppCommand command;
   }

   @Inject
   public FileTypeCommands(Session session,
                           EventBus eventBus,
                           final HTMLPreviewServerOperations server)
   {
      session_ = session;
          
      eventBus.addHandler(PackageStateChangedEvent.TYPE,
                          new PackageStateChangedHandler() {
         @Override
         public void onPackageStateChanged(PackageStateChangedEvent e)
         {
            server.getHTMLCapabilities(
                  new ServerRequestCallback<HTMLCapabilities>() {

                     @Override
                     public void onResponseReceived(HTMLCapabilities caps)
                     {
                        setHTMLCapabilities(caps);
                     }
                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                     }
                  });
         }
      });
   }
   
   public TextFileType[] statusBarFileTypes()
   {
      ArrayList<TextFileType> types = new ArrayList<TextFileType>();
      types.add(FileTypeRegistry.R);
      types.add(FileTypeRegistry.RMARKDOWN);
      types.add(FileTypeRegistry.SWEAVE);
      types.add(FileTypeRegistry.RHTML);
      types.add(FileTypeRegistry.RPRESENTATION);
      types.add(FileTypeRegistry.RD);
      types.add(FileTypeRegistry.TEXT);
      types.add(FileTypeRegistry.TEX);
      types.add(FileTypeRegistry.MARKDOWN);
      types.add(FileTypeRegistry.XML);
      types.add(FileTypeRegistry.YAML);
      types.add(FileTypeRegistry.DCF);
      types.add(FileTypeRegistry.SH);
      types.add(FileTypeRegistry.HTML);
      types.add(FileTypeRegistry.CSS);
      types.add(FileTypeRegistry.JS);
      types.add(FileTypeRegistry.CPP);
      types.add(FileTypeRegistry.PYTHON);
      types.add(FileTypeRegistry.SQL);
      types.add(FileTypeRegistry.STAN);

      
      return (TextFileType[])types.toArray(new TextFileType[0]);
   }
   
   public HTMLCapabilities getHTMLCapabiliites()
   {
      if (htmlCapabilities_ == null)
         setHTMLCapabilities(session_.getSessionInfo().getHTMLCapabilities());
      
      return htmlCapabilities_;
   }
   
   public void setHTMLCapabilities(HTMLCapabilities caps)
   {
      htmlCapabilities_ = caps;
   }
   
   private final Session session_;
   
   private HTMLCapabilities htmlCapabilities_;

}
