/*
 * Null.java
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

package org.rstudio.studio.client.server;

import com.google.gwt.core.client.JavaScriptObject;

// Like Void, but doesn't have annoying name collisions with java.lang.Void.

public class Null extends JavaScriptObject
{
   protected Null()
   {
   }
   
   public static final native Null create()
   /*-{
      return {};
   }-*/;
}
