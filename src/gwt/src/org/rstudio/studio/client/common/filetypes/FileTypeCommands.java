/*
 * FileTypeCommands.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;

import com.google.gwt.event.shared.HandlerRegistration;
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
      server_ = server;

      // HTML package capabilities are optional during client initialization.
      eventBus.addHandler(SessionInitEvent.TYPE, event ->
      {
         if (htmlCapabilities_ == null && !htmlCapabilitiesRequestPending_)
            refreshHTMLCapabilities();
      });
      eventBus.addHandler(PackageStateChangedEvent.TYPE, event -> refreshHTMLCapabilities());
   }

   private void refreshHTMLCapabilities()
   {
      if (htmlCapabilitiesRequestPending_)
      {
         htmlCapabilitiesRefreshRequested_ = true;
         return;
      }

      htmlCapabilitiesRequestPending_ = true;
      server_.getHTMLCapabilities(new ServerRequestCallback<HTMLCapabilities>()
      {
         @Override
         public void onResponseReceived(HTMLCapabilities caps)
         {
            htmlCapabilitiesRequestPending_ = false;
            if (htmlCapabilitiesRefreshRequested_)
               refreshHTMLCapabilitiesIfRequested();
            else
               setHTMLCapabilities(caps);
         }

         @Override
         public void onError(ServerError error)
         {
            htmlCapabilitiesRequestPending_ = false;
            Debug.logError(error);
            refreshHTMLCapabilitiesIfRequested();
         }
      });
   }

   private void refreshHTMLCapabilitiesIfRequested()
   {
      if (htmlCapabilitiesRefreshRequested_)
      {
         htmlCapabilitiesRefreshRequested_ = false;
         refreshHTMLCapabilities();
      }
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
         FileTypeRegistry.LESS,
         FileTypeRegistry.JS,
         FileTypeRegistry.JSON,
         FileTypeRegistry.C,
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
         return session_.getSessionInfo().getHTMLCapabilities();

      return htmlCapabilities_;
   }

   public HandlerRegistration withHTMLCapabilities(CommandWithArg<HTMLCapabilities> callback)
   {
      if (htmlCapabilities_ != null)
      {
         callback.execute(htmlCapabilities_);
         return () -> {};
      }

      htmlCapabilitiesCallbacks_.add(callback);
      if (!htmlCapabilitiesRequestPending_)
         refreshHTMLCapabilities();

      return () -> htmlCapabilitiesCallbacks_.remove(callback);
   }

   public void setHTMLCapabilities(HTMLCapabilities caps)
   {
      htmlCapabilities_ = caps;

      List<CommandWithArg<HTMLCapabilities>> callbacks = new ArrayList<>(htmlCapabilitiesCallbacks_);
      htmlCapabilitiesCallbacks_.clear();
      for (CommandWithArg<HTMLCapabilities> callback : callbacks)
         callback.execute(caps);
   }

   private final Session session_;
   private final HTMLPreviewServerOperations server_;

   private HTMLCapabilities htmlCapabilities_;
   private boolean htmlCapabilitiesRequestPending_;
   private boolean htmlCapabilitiesRefreshRequested_;
   private final List<CommandWithArg<HTMLCapabilities>> htmlCapabilitiesCallbacks_ = new ArrayList<>();

}
