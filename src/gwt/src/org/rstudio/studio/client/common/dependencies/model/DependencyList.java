/*
 * DependencyList.java
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
package org.rstudio.studio.client.common.dependencies.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class DependencyList extends JavaScriptObject
{
   protected DependencyList()
   {
   }

   public final native Dependency getPackage(String packageName) /*-{
      var pkg = this.packages[packageName];
      if (typeof pkg !== "undefined")
         return pkg;
      return null;
   }-*/;
   
   public final native JsArrayString getFeatureDependencies(String feature) /*-{
      var packages = this.features[feature];
      if (typeof packages !== "undefined")
         return packages["packages"];
      return [];
   }-*/;
   
   public final native String getFeatureDescription(String feature) /*-{
      var feature = this.features[feature];
      if (typeof feature !== "undefined")
         return feature["description"];
      return "";
   }-*/;
}
