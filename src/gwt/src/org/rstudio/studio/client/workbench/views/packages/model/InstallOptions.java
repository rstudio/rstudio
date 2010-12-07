/*
 * InstallOptions.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

public class InstallOptions extends JavaScriptObject
{
   protected InstallOptions()
   {   
   }
   
   public static final native InstallOptions create(String repository, 
                                                    String packageName) /*-{
      var options = new Object();
      options.repository = repository ;
      options.packageName = packageName ;
      return options ;
   }-*/;
   

   public final native String getRepository() /*-{
      return this.repository;
   }-*/;
   public final native String getPackageName() /*-{
      return this.packageName;
   }-*/;
   
   

}
