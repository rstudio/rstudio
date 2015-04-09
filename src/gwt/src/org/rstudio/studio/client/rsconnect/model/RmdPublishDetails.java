/*
 * RSConnectPublishDetails.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

package org.rstudio.studio.client.rsconnect.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RmdPublishDetails extends JavaScriptObject
{
   protected RmdPublishDetails()
   {
   }
   
   public final native boolean isMultiRmd() /*-{
      return this.is_multi_rmd;
   }-*/;

   public final native boolean isShinyRmd() /*-{
      return this.is_shiny_rmd;
   }-*/;

   public final native boolean isSelfContained() /*-{
      return this.is_self_contained;
   }-*/;
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;
}
