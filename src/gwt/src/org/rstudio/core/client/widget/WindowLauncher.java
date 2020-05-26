/*
 * WindowLauncher.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.core.client.widget;

public class WindowLauncher
{
   public static native void showPreformattedContent(String title, String content) /*-{
      var win = window.open("", "_blank", "width=600,height=800,menubar=0,toolbar=0,location=0,status=0,scrollbars=1,resizable=1,directories=0");
      win.document.write("<head><title>" + title + "</title></head><body><pre>" + content + "</pre></body>");
   }-*/;
    
}
