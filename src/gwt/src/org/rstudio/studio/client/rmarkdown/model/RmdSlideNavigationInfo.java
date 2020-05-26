/*
 * RmdSlideNavigationInfo.java
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
package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.studio.client.common.presentation.model.SlideNavigation;

import com.google.gwt.core.client.JavaScriptObject;

public class RmdSlideNavigationInfo extends JavaScriptObject
{
   protected RmdSlideNavigationInfo() 
   {
   }

   public final native int getPreviewSlide() /*-{
      return this.preview_slide;
   }-*/;
   
   public final native SlideNavigation getSlideNavigation() /*-{
      return this.slide_navigation;
   }-*/;
   
   public final native void copySlideInfo(RmdSlideNavigationInfo other) /*-{
      this.preview_slide = other.preview_slide;
      this.slide_navigation = other.slide_navigation;
   }-*/;
}
