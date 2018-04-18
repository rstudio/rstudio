/*
 * CodeNavigationListener.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.core.client;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CodeNavigationListener
{
   @Inject
   public CodeNavigationListener(FileTypeRegistry fileTypeRegistry)
   {
      codeNavigationListener_ = this;
      fileTypeRegistry_ = fileTypeRegistry;
      
      initializeMessageListeners();
   }

   private static String getDomainFromUrl(String url)
   {
      RegExp reg = RegExp.compile("https?://[^/]+");
      MatchResult result = reg.exec(url);
      if (result != null)
      {
         return result.getGroup(0);
      }

      return "";
   }

   private native static String getOrigin() /*-{
     return $wnd.location.origin;
   }-*/;
   
   public void setUrl(String url)
   {
      url_ = url;
   }
   
   public static String getCurrentDomain()
   {
      if (codeNavigationListener_ == null) return "";

      return getDomainFromUrl(codeNavigationListener_.url_);
   }
   
   private void openFileFromMessage(final String file,
                                    final int line,
                                    final int column)
   {

      FilePosition filePosition = FilePosition.create(line, column);
      CodeNavigationTarget navigationTarget = new CodeNavigationTarget(file, filePosition);

      fileTypeRegistry_.editFile(
         FileSystemItem.createFile(navigationTarget.getFile()),
         filePosition);
   }

   public static void onOpenFileFromMessage(final String file, int line, int column)
   {
      if (codeNavigationListener_ != null)
      {
         codeNavigationListener_.openFileFromMessage(file, line, column);
      }
   }
   
   private native static void initializeMessageListeners() /*-{
      var handler = $entry(function(e) {
         var domain = @org.rstudio.core.client.CodeNavigationListener::getCurrentDomain()();
         if (typeof e.data != 'object')
            return;
         if (e.origin != $wnd.location.origin && e.origin != domain)
            return;
         if (e.data.message != "openfile")
            return;
         if (e.data.source != "r2d3")
            return;
            
         @org.rstudio.core.client.CodeNavigationListener::onOpenFileFromMessage(Ljava/lang/String;II)(
            e.data.file,
            parseInt(e.data.line),
            parseInt(e.data.column)
         );
      });
      $wnd.addEventListener("message", handler, true);
   }-*/;
   
   public String getOriginDomain()
   {
      return getDomainFromUrl(getOrigin());
   }
   
   private final FileTypeRegistry fileTypeRegistry_;
   private static CodeNavigationListener codeNavigationListener_;
   private String url_;
}
