/*
 * PdfJs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.pdfviewer.pdfjs;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;

public class PdfJs
{
   public static void preload()
   {
      load(null);
   }

   public static void load(final Command command)
   {
      // TODO: This got less robust when we started loading multiple JS files.
      // ExternalJavaScriptLoader.loadSequentially does not defend against
      // multiple invocations the way ExternalJavaScriptLoader.addCallback does.
      // Should figure out a generic mechanism to deal with this (i.e.
      // ExternalJavaScriptLoader takes a variable number of URLs, not just
      // one).

      final PdfJsResources res = PdfJsResources.INSTANCE;
      ExternalJavaScriptLoader.loadSequentially(
            new String[] {
                  res.compatibilityJs().getSafeUri().asString(),
                  res.pdfjs().getSafeUri().asString(),
                  //res.debuggerJs().getSafeUri().asString(),
                  res.viewerJs().getSafeUri().asString(),
            },
            new Callback()
            {
               @Override
               public void onLoaded()
               {
                  if (!initialized_)
                  {
                     PDFView.initializeEvents();

                     LinkElement styleLink = Document.get().createLinkElement();
                     styleLink.setType("text/css");
                     styleLink.setRel("stylesheet");
                     styleLink.setHref(res.viewerCss().getSafeUri().asString());
                     Document.get().getElementsByTagName("head").getItem(0)
                           .appendChild(styleLink);

                     initialize(res.pdfjs().getSafeUri().asString());

                     initialized_ = true;
                  }

                  if (command != null)
                     command.execute();
               }
            }
      );

   }

   private static native void initialize(String pdfjsUrl) /*-{
      $wnd.PDFJS.workerSrc = pdfjsUrl;
   }-*/;

   private static boolean initialized_;
   private static final ExternalJavaScriptLoader pdfjs_ = new ExternalJavaScriptLoader(
         PdfJsResources.INSTANCE.pdfjs().getSafeUri().asString());
}
