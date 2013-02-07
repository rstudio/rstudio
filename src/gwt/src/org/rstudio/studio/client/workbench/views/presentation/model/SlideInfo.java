/*
 * SlideInfo.java
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
package org.rstudio.studio.client.workbench.views.presentation.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SlideInfo extends JavaScriptObject
{
   protected SlideInfo()
   {
   }
      
   public final native String getTitle() /*-{
      return this.title;
   }-*/;
 
   public final native boolean isSection() /*-{
      return this.is_section;
   }-*/;
}
