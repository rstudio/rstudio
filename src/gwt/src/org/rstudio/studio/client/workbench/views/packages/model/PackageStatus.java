/*
 * PackageStatus.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;



public class PackageStatus extends JavaScriptObject
{
   protected PackageStatus()
   {
   }
   
   public final native String getName() /*-{
      return this.name[0];
   }-*/;

   public final native boolean isLoaded() /*-{
      return this.loaded[0];
   }-*/;
}
