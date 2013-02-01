/*
 * PresentationState.java
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

public class PresentationState extends JavaScriptObject
{
   protected PresentationState()
   {
   }
   
   public final native boolean isActive() /*-{
      return this.active;
   }-*/;
   
   public final native boolean isAuthorMode() /*-{
      return this.author_mode;
   }-*/;
   
   public final native boolean isUsingRmd() /*-{
      return this.using_rmd;
   }-*/;
   
   public final native String getPaneCaption() /*-{
      return this.pane_caption;
   }-*/;
 
   
   public final native String getDirectory() /*-{
      return this.directory;
   }-*/;
   
   public final native int getSlideIndex() /*-{
      return this.slide_index;
   }-*/;
}
