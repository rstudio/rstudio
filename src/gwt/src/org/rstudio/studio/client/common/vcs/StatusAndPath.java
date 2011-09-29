/*
 * StatusAndPath.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class StatusAndPath extends JavaScriptObject
{
   protected StatusAndPath()
   {}

   public native final String getStatus() /*-{
      return this.status;
   }-*/;

   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getRawPath() /*-{
      return this.raw_path;
   }-*/;

   public native final boolean isDiscardable() /*-{
      return this.discardable;
   }-*/;

   public final boolean isFineGrainedActionable()
   {
      return !"??".equals(getStatus());
   }
}
