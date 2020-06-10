/*
 * NewPackageOptions.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class NewPackageOptions extends JavaScriptObject
{
   protected NewPackageOptions()
   {
   }
   
   public native final static NewPackageOptions create(String packageName,
                                                       boolean usingRcpp,
                                                       JsArrayString codeFiles) 
   /*-{
      var options = new Object();
      options.package_name = packageName;
      options.using_rcpp = usingRcpp;
      options.code_files = codeFiles;
      return options;
   }-*/;
   
   public native final boolean getUsingRcpp() /*-{
      return this.using_rcpp;
   }-*/;
 
   public native final JsArrayString getCodeFiles() /*-{
      return this.code_files;
   }-*/;
   
   public native final String getPackageName() /*-{
      return this.package_name;
   }-*/;
}
