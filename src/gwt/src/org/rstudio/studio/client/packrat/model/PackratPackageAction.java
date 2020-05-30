/*
 * PackratPackageAction.java
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
package org.rstudio.studio.client.packrat.model;

import org.rstudio.core.client.js.JsObject;

public class PackratPackageAction extends JsObject {
   
   protected PackratPackageAction() {}
   
   public final String getPackage() 
   {
      return getAsString("package");
   }
   
   public final String getAction()
   {
      return getAsString("action");
   }
   
   public final String getPackratVersion()
   {
      return getAsString("packrat.version");
   }

   public final String getLibraryVersion()
   {
      return getAsString("library.version");
   }
   
   public final String getMessage()
   {
      return getAsString("message");
   }
}
