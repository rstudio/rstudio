/*
 * SuperDevMode.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.common;

import com.google.gwt.core.client.GWT;

public class SuperDevMode
{
   public final static native void reload() /*-{
      $wnd.__gwt_bookmarklet_params = {
         server_url:'http://localhost:9876/',
         module_name:'rstudio'
      }; 
      
      var s = $doc.createElement('script'); 
      s.src = 'http://localhost:9876/dev_mode_on.js'; 
      void($doc.getElementsByTagName('head')[0].appendChild(s));
   }-*/ ;
   
   
   public static boolean isActive()
   {
      return 
        GWT.getModuleBaseURL().equals("http://localhost:8787/rstudio/") &&
        GWT.getModuleBaseForStaticFiles().equals("http://localhost:9876/rstudio/");
   }
}
