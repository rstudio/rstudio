/*
 * PackageList.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.studio.client.workbench.projects.ProjectContext;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class PackageState extends JavaScriptObject
{
   protected PackageState()
   {
   }
   
   public final native JsArray<PackageInfo> getPackageList() /*-{
      return this.package_list;
   }-*/;
   
   public final native ProjectContext getProjectContext() /*-{
      return {
         "packrat_context": this.packrat_context,
         "renv_context": this.renv_context
      };
   }-*/;
}
