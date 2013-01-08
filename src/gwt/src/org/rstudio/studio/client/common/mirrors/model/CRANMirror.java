/*
 * CRANMirror.java
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
package org.rstudio.studio.client.common.mirrors.model;

import com.google.gwt.core.client.JavaScriptObject;


public class CRANMirror extends JavaScriptObject
{
   protected CRANMirror()
   {
   }
   
   public final static native CRANMirror empty() /*-{
      var cranMirror = new Object();
      cranMirror.name = "";
      cranMirror.host = "";
      cranMirror.url = "";
      cranMirror.country = "";
      return cranMirror;
   }-*/;
   
   public final boolean isEmpty()
   {
      return getName() == null || getName().length() == 0;
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getHost() /*-{
      return this.host;
   }-*/;

   public final native String getURL() /*-{
      return this.url;
   }-*/;
   
   public final native String getCountry() /*-{
      return this.country;
   }-*/;

   public final String getDisplay()
   {
      return getName()  +" - " + getHost();
   }
}
