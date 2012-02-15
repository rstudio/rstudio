/*
 * CompilePdfError.java
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

package org.rstudio.studio.client.workbench.views.output.compilepdf.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;

public class CompilePdfError extends JavaScriptObject
{
   public static final int ERROR = 0;
   public static final int WARNING = 1;
   public static final int BOX = 2;
   
   protected CompilePdfError()
   {
   }
   
   public final native int getType() /*-{
      return this.type;
   }-*/;
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
   public final native int getLine() /*-{
      return this.line;
   }-*/;
   
   public final native String getMessage() /*-{
      return this.message;
   }-*/;

   public final String asString()
   {
      FileSystemItem fsi = FileSystemItem.createFile(getPath());
      return fsi.getName() + " (line " + getLine() + "): " + getMessage();
   }
}
