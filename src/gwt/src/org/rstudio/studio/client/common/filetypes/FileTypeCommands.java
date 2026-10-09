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
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
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

      // HTML package capabilities are optional during client initialization,
      // so the main window probes as soon as session info exists: at once
      // when this singleton is created after it, otherwise on SessionInitEvent.
      // Preview and Compile Report then usually stay synchronous (a window
      // opened only after a round trip can be popup-blocked). Satellites ask
      // when an editor first needs the answer.
      if (!Satellite.isCurrentWindowSatellite())
      {
         session.withSessionInfo(info ->
         {
            if (htmlCapabilities_ == null && !htmlCapabilitiesRequestPending_)
               refreshHTMLCapabilities();
         });
      }
      // A package change supersedes any probe still in flight.
      eventBus.addHandler(PackageStateChangedEvent.TYPE, event -> refreshHTMLCapabilities());
   }

   private void refreshHTMLCapabilities()
   {
      htmlCapabilitiesRetry_.cancel();
      final int generation = ++htmlCapabilitiesGeneration_;
      htmlCapabilitiesRequestPending_ = true;
      server_.getHTMLCapabilities(new ServerRequestCallback<HTMLCapabilities>()
      {
         @Override
         public void onResponseReceived(HTMLCapabilities caps)
         {
            if (generation != htmlCapabilitiesGeneration_)
               return;

            htmlCapabilitiesRequestPending_ = false;
            htmlCapabilitiesRetries_ = 0;
            setHTMLCapabilities(caps);
         }

         @Override
         public void onError(ServerError error)
         {
            if (generation != htmlCapabilitiesGeneration_)
               return;

            htmlCapabilitiesRequestPending_ = false;
            Debug.logError(error);

            // Restored editors are waiting on this answer, so retry a few
            // times; afterwards the next editor or package change asks again.
            if (htmlCapabilitiesRetries_ < MAX_HTML_CAPABILITIES_RETRIES)
            {
               htmlCapabilitiesRetries_++;
               htmlCapabilitiesRetry_.schedule(HTML_CAPABILITIES_RETRY_MS * htmlCapabilitiesRetries_);
               return;
            }

            // Nothing will answer these; dropping them keeps a later probe
            // from replaying every action attempted during the outage.
            htmlCapabilitiesCallbacks_.clear();
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

   // Until the probe answers this is the session's empty placeholder, which
   // reports nothing as supported; decisions should go through
   // withHTMLCapabilities() instead.
   public HTMLCapabilities getHTMLCapabiliites()
   {
      if (htmlCapabilities_ == null)
         return session_.getSessionInfo().getHTMLCapabilities();

      return htmlCapabilities_;
   }

   public boolean hasHTMLCapabilities()
   {
      return htmlCapabilities_ != null;
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
   private int htmlCapabilitiesGeneration_;
   private boolean htmlCapabilitiesRequestPending_;
   private int htmlCapabilitiesRetries_;
   private final Timer htmlCapabilitiesRetry_ = new Timer()
   {
      @Override
      public void run()
      {
         refreshHTMLCapabilities();
      }
   };
   private final List<CommandWithArg<HTMLCapabilities>> htmlCapabilitiesCallbacks_ = new ArrayList<>();

   public static final int HTML_CAPABILITIES_RETRY_MS = 1000;
   public static final int MAX_HTML_CAPABILITIES_RETRIES = 3;
}
