/*
 * PackratContext.java
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


import com.google.gwt.core.client.JavaScriptObject;

public class PackratContext extends JavaScriptObject
{
   protected PackratContext()
   {
   }
   
   
   public final native static PackratContext empty() /*-{
      return {
        available: false,
        applicable: false, 
        packified: false,
        mode_on: false 
      };
   }-*/;
   
   
   public final native boolean isAvailable() /*-{
      return this.available;
   }-*/;
    
   public final native boolean isApplicable() /*-{
      return this.applicable;
   }-*/;
   
   public final native boolean isPackified() /*-{
      return this.packified;
   }-*/;
   
   public final native boolean isModeOn() /*-{
      return this.mode_on;
}-*/;


   
   
   
   
   
      
 
}
