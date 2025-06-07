/*
 * PackageStatus.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;



public class PackageStatus extends JavaScriptObject
{
   protected PackageStatus()
   {
   }
   
   public static final native PackageStatus create(String name,
                                                   String library,
                                                   boolean attached)
   /*-{
      return {
         name: name,
         library: library,
         attached: attached
      };
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getLibrary() /*-{
      return this.library;
   }-*/;

   public final native boolean isAttached() /*-{
      return this.attached || false;
   }-*/;
}
