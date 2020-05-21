/*
 * UpdateCheckResult.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

public class UpdateCheckResult extends JavaScriptObject
{
   protected UpdateCheckResult() {}
   
   public final native String getUpdateMessage() /*-{
      return (this['update-message'] || "").trim();
   }-*/;

   public final native int getUpdateUrgency() /*-{
      return parseInt(this['update-urgent']);
   }-*/;
   
   public final native String getUpdateUrl() /*-{
      return (this['update-url'] || "").trim();
   }-*/;

   public final native String getUpdateVersion() /*-{
      return (this['update-version'] || "").trim();
   }-*/;
}
