/*
 * Void.java
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

// Type returned to ServerRequestCallback::onResponseReceived when 
// the remote method conceptually has a "void" return type

public class Void extends JavaScriptObject
{
   public static final native Void create() /*-{
      return new Object();
   }-*/;
   
   protected Void()
   {
   }
}
