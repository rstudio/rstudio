/*
 * PresentationEditorLocation.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.presentation2.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class PresentationEditorLocation extends JavaScriptObject
{   
   protected PresentationEditorLocation()
   {
   }
   
   public static final native PresentationEditorLocation create(JsArray<PresentationEditorLocationItem> items,
                                                                int autoSlideLevel) /*-{
      return {
         items: items,
         auto_slide_level: autoSlideLevel 
      };
   }-*/;
   
     
   public final native JsArray<PresentationEditorLocationItem> getItems() /*-{
      return this.items;
   }-*/;
   
   public final native int getAutoSlideLevel() /*-{
      return this.auto_slide_level;
   }-*/;
 
}
