/*
 * EditingPrefs.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class EditingPrefs extends JavaScriptObject
{
   public static final int LINEENDINGS_WINDOWS = 0;
   public static final int LINEENDINGS_POSIX = 1;
   public static final int LINEENDINGS_NATIVE = 2;
   public static final int LINEENDINGS_PASSTHROUGH = 3;
   
   protected EditingPrefs() {}

   public static final native EditingPrefs create(int lineEndings) /*-{
      var prefs = new Object();
      prefs.line_endings = lineEndings;
      return prefs ;
   }-*/;

   
   public native final int getLineEndings() /*-{
      return this.line_endings;
   }-*/;
}
