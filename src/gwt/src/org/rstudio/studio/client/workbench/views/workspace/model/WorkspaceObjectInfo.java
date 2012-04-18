/*
 * WorkspaceObjectInfo.java
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
package org.rstudio.studio.client.workbench.views.workspace.model;

import com.google.gwt.core.client.JavaScriptObject;

public class WorkspaceObjectInfo extends JavaScriptObject
{
   protected WorkspaceObjectInfo()
   {
   }
   
   public final boolean isHidden()
   {
      return getName().startsWith(".");
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getType() /*-{
      return this.type;
   }-*/;
   
   public final native int getLength() /*-{
      return this.len;
   }-*/;

   public final native String getValue() /*-{
      return this.value;
   }-*/;

   public final native String getExtra() /*-{
      return this.extra;
   }-*/;
}
