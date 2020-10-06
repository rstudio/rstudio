/*
 * ShinyCapabilities.java
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
package org.rstudio.studio.client.common.shiny.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ShinyCapabilities extends JavaScriptObject
{
   protected ShinyCapabilities()
   {
   }
   
   public static final native ShinyCapabilities createDefault()  /*-{
      var caps = new Object();
      caps.installed = false;
      return caps;
   }-*/;
   
   public native final boolean getInstalled() /*-{
      return this.installed;
   }-*/;
   
   public final boolean hasAllCapabilities() 
   {
      return getInstalled();
   }
}
