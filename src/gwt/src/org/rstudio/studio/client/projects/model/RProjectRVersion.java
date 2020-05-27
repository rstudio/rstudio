/*
 * RProjectRVersion.java
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

public class RProjectRVersion extends JavaScriptObject
{
   public static final String DEFAULT = "Default";
   
   public static final String ARCH_32 = "32";
   public static final String ARCH_64 = "64";
   
   protected RProjectRVersion()
   {
   }
   
   public static final RProjectRVersion useDefault()
   {
      return create(DEFAULT);
   }
   
   public static final RProjectRVersion useDefault(String arch)
   {
      return create(DEFAULT, arch);
   }
   
   public static final RProjectRVersion create(String number)
   {
      return create(number, "");
   }
   
   public native static final RProjectRVersion create(String number, 
                                                      String arch) /*-{ 
      var ver = new Object();
      ver.number = number;
      ver.arch = arch;
      return ver;   
   }-*/;
   
   public native final String getNumber() /*-{
      return this.number;
   }-*/;
  
   public native final String getArch() /*-{
      return this.arch;
   }-*/;
}
