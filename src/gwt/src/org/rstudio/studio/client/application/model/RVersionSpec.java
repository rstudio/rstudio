/*
 * RVersionSpec.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RVersionSpec extends JavaScriptObject
{
   protected RVersionSpec() {}
 
   public final static RVersionSpec createEmpty()
   {
      return create("","");
   }
   
   public final static native RVersionSpec create(String version, 
                                                  String rHome) /*-{
      return {
         version: version,
         r_home: rHome
      };
   }-*/;                               
   
   public final native String getVersion() /*-{
      return this.version;
   }-*/;

   public final native String getRHome() /*-{
      return this.r_home;
   }-*/;
   
   public static boolean hasDuplicates(JsArray<RVersionSpec> rVersions)
   {
      for (int i = 0; i<rVersions.length(); i++)
      {
         for (int j = 0; j<rVersions.length(); j++)
         {
            if (i != j && rVersions.get(i).getVersion().equals(
                          rVersions.get(j).getVersion())) 
               return true;
         }
      }
      
      return false;
   }
}
