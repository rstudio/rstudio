/*
 * ConnectionObjectType.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.model;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;

public class ConnectionObjectType extends JavaScriptObject
{
   protected ConnectionObjectType()
   {
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getContains() /*-{
      return this.contains;
   }-*/;
   
   public final native String getIconData() /*-{
      return this.icon_data;
   }-*/;
   
   public final boolean isDataType()
   {
      return StringUtil.isNullOrEmpty(getContains()) ||
             getContains() == "data";
   }
}
