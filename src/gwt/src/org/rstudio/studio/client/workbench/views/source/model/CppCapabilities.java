/*
 * CppCapabilities.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class CppCapabilities extends JavaScriptObject
{
   protected CppCapabilities()
   {
   } 
   
   public static final native CppCapabilities createDefault()  /*-{
      var caps = new Object();
      caps.can_build = false;
      caps.can_source_cpp = false;
      return caps;
   }-*/;
   
   public native final boolean getCanBuild() /*-{
      return this.can_build;
   }-*/;
   
   public native final boolean getCanSourceCpp() /*-{
      return this.can_source_cpp;
   }-*/;
   
   public final boolean hasAllCapabiliites()
   {
      return getCanBuild() && getCanSourceCpp();
   }
}
