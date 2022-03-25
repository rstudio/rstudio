/*
 * ConsoleTracebackFrame.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

package org.rstudio.studio.client.common.debugging.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ConsoleTracebackFrame extends JavaScriptObject
{
   protected ConsoleTracebackFrame() {}
   
   public final native String getCall() /*-{
      return this.call;
   }-*/;

   public final native int getParent() /*-{
      return this.parent;
   }-*/;

   public final native boolean getVisible() /*-{
      return this.visible;
   }-*/;

   public final native String getNamespace() /*-{
      return this.namespace;
   }-*/;

   public final native String getScope() /*-{
      return this.scope;
   }-*/;

}
