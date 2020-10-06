/*
 * PostMessageEvent.java
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

package org.rstudio.core.client.dom;

import com.google.gwt.core.client.JavaScriptObject;

public class PostMessageEvent extends JavaScriptObject
{
   protected PostMessageEvent() {}

   public final native <T> T getData() /*-{
      return this.data;
   }-*/;
   public final native String getOrigin() /*-{
      return this.origin;
   }-*/;
   public final native WindowEx getSource() /*-{
      return this.source;
   }-*/;
}
