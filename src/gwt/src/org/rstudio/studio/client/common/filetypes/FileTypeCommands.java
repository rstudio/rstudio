/*
 * FileTypeCommands.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;

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
                          new PackageStateChangedEvent.Handler() {
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

   public List<TextFileType> statusBarFileTypes()
   {
      ArrayList<TextFileType> fileTypes = new ArrayList<TextFileType>(Arrays.asList(
         FileTypeRegistry.TEXT,
         FileTypeRegistry.R,
         FileTypeRegistry.RMARKDOWN,
         FileTypeRegistry.SWEAVE,
         FileTypeRegistry.RHTML,
         FileTypeRegistry.RPRESENTATION,
         FileTypeRegistry.RD,
         FileTypeRegistry.TEX,
         FileTypeRegistry.MARKDOWN,
         FileTypeRegistry.XML,
         FileTypeRegistry.YAML,
         FileTypeRegistry.DCF,
         FileTypeRegistry.SH,
         FileTypeRegistry.HTML,
         FileTypeRegistry.CSS,
         FileTypeRegistry.SASS,
         FileTypeRegistry.SCSS,
         FileTypeRegistry.JS,
         FileTypeRegistry.JSON,
         FileTypeRegistry.CPP,
         FileTypeRegistry.PYTHON,
         FileTypeRegistry.SQL,
         FileTypeRegistry.STAN
      ));
      if (session_.getSessionInfo().getQuartoConfig().enabled) 
      {
         fileTypes.add(fileTypes.indexOf(FileTypeRegistry.TEX), FileTypeRegistry.QUARTO);
      }
      
      return fileTypes;
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
