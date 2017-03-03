/*
 * BootInfo.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class BootInfo extends JavaScriptObject
{
   protected BootInfo()
   {
   }

   public static BootInfo empty()
   {
      return (BootInfo)JavaScriptObject.createObject().cast();
   }

   public final native boolean getUse2xResolution() /*-{
      var isEmpty = (this === null || typeof(this) === "undefined");

      return !isEmpty && typeof(this.use_2x_resolution) !== "undefined" ? this.use_2x_resolution : true;
   }-*/;
}
