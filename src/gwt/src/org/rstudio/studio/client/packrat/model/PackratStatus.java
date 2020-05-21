/*
 * PackratStatus.java
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
import com.google.gwt.view.client.ProvidesKey;

// This class represents the JSON value the server returns from
// get_packrat_status.
public class PackratStatus extends JsObject
{
   
   public static final ProvidesKey<PackratStatus> KEY_PROVIDER =
   new ProvidesKey<PackratStatus>() {
      public Object getKey(PackratStatus item) {
         return item.getPackageName();
      }
   };
   
   protected PackratStatus() {}

   public final native int length() /*-{
      return Object.keys(this).length;
   }-*/;
   
   public final native String getPackageName() /*-{
      return this["package"];
   }-*/;
   
   public final native String getPackageVersion() /*-{
      return this["packrat.version"];
   }-*/;
   
   public final native String getPackageSource() /*-{
      return this["packrat.source"];
   }-*/;
   
   public final native String getLibraryVersion() /*-{
      return this["library.version"];
   }-*/;
   
   public final native String getCurrentlyUsed() /*-{
      return this["currently.used"] ? "TRUE" : "FALSE";
   }-*/;
   
}
