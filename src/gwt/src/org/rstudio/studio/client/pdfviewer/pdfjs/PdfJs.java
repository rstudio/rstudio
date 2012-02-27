/*
 * PdfJs.java
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
      pdfjs_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            if (!initialized_)
            {
               PdfJsResources resources = PdfJsResources.INSTANCE;

               LinkElement styleLink = Document.get().createLinkElement();
               styleLink.setType("text/css");
               styleLink.setRel("stylesheet");
               styleLink.setHref(resources.viewerCss().getSafeUri().asString());
               Document.get().getElementsByTagName("head").getItem(0)
                     .appendChild(styleLink);

               initialize(resources.pdfjs().getSafeUri().asString());

               initialized_ = true;
            }

            if (command != null)
               command.execute();
         }
      });
   }

   private static native void initialize(String pdfjsUrl) /*-{
      $wnd.PDFJS.workerSrc = pdfjsUrl;
   }-*/;

   private static boolean initialized_;
   private static final ExternalJavaScriptLoader pdfjs_ = new ExternalJavaScriptLoader(
         PdfJsResources.INSTANCE.pdfjs().getSafeUri().asString());
}
