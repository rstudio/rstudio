/*
 * RequestLogDetail.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.HttpLogEntry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import java.util.ArrayList;
import java.util.HashSet;

public class RequestLogDetail extends Composite
{
   public RequestLogDetail(final ApplicationServerOperations server,
                           RequestLogEntry entry)
   {
      server_ = server;
      entry_ = entry;

      String req = entry.getRequestData();
      String resp = entry.getResponseData();

      final FlowPanel panel = new FlowPanel();
      panel.getElement().getStyle().setOverflow(Overflow.AUTO);

      HTML html = new HTML();
      html.setText("Request ID: " + entry.getRequestId() + "\n\n"
                   + "== REQUEST ======\n"
                   + req
                   + "\n\n"
                   + "== RESPONSE ======\n"
                   + resp
                   + "\n\n"
                   + "== SERVER TIMES ======\n");
      html.getElement().getStyle().setProperty("whiteSpace", "pre-wrap");

      panel.add(html);

      if (!refreshNeeded())
         show(panel);
      else
      {
         final Button button = new Button();
         button.setText("Fetch");
         panel.add(button);
         button.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               button.removeFromParent();
               if (!refreshNeeded())
                  show(panel);
               else
               {
                  updateCache(server_, new Command()
                  {
                     public void execute()
                     {
                        show(panel);
                     }
                  });
               }
            }
         });
      }

      initWidget(panel);
   }

   private boolean refreshNeeded()
   {
      int matches = 0;
      for (HttpLogEntry entry : cachedEntries_)
         if (entry.getRequestId().equals(entry_.getRequestId()))
            matches++;
      return matches < 3;
   }

   private void show(FlowPanel panel)
   {
      Long last = null;
      StringBuffer output = new StringBuffer();
      for (HttpLogEntry entry : cachedEntries_)
      {
         if (!entry.getRequestId().equals(entry_.getRequestId()))
            continue;

         long time = entry.getTimestamp().getTime();
         output.append(entry.getTypeAsString())
               .append(":")
               .append(time);
         if (last != null)
            output.append(" (+")
                  .append(time - last)
                  .append("ms)");
         last = time;

         output.append("\n");
      }
      HTML serverLog = new HTML();
      if (output.length() == 0)
         output.append("?\n");
      serverLog.setText(output.toString());
      serverLog.getElement().getStyle().setProperty("whiteSpace", "pre-wrap");
      panel.add(serverLog);
   }

   private void updateCache(ApplicationServerOperations server,
                            final Command command)
   {
      server.httpLog(new ServerRequestCallback<JsArray<HttpLogEntry>>()
      {
         @Override
         public void onError(ServerError error)
         {
            if (command != null)
               command.execute();

            Window.alert(error.getUserMessage());
         }

         @Override
         public void onResponseReceived(JsArray<HttpLogEntry> response)
         {
            for (int i = 0; i < response.length(); i++)
            {
               HttpLogEntry entry = response.get(i);
               String signature = entry.getRequestId() + ":" + entry.getType();
               if (!cachedKeys_.contains(signature))
               {
                  cachedKeys_.add(signature);
                  cachedEntries_.add(entry);
               }
            }
            if (command != null)
               command.execute();
         }
      });

   }

   private ApplicationServerOperations server_;
   private RequestLogEntry entry_;

   private static HashSet<String> cachedKeys_ = new HashSet<String>();
   private static ArrayList<HttpLogEntry> cachedEntries_ =
         new ArrayList<HttpLogEntry>();
}
