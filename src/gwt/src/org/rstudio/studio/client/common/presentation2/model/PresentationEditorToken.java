/*
 * PresentationToken.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.common.presentation2.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PresentationEditorToken extends JavaScriptObject
{
   public static final String TITLE = "title";
   public static final String HEADING = "heading";
   public static final String HR = "hr";
   public static final String CURSOR = "cursor";
   
   
   protected PresentationEditorToken()
   {
   }
   
   public static final native PresentationEditorToken title() /*-{
      return {
         type: 'title',
         level: 0
      };
   }-*/;
   
   public static final native PresentationEditorToken heading(int level) /*-{
      return {
         type: 'heading',
         level: level
      };
   }-*/;
   
   public static final native PresentationEditorToken hr() /*-{
      return {
         type: 'hr',
         level: 0
      };
   }-*/;
   
   
   public static final native PresentationEditorToken cursor() /*-{
      return {
         type: 'cursor',
         level: 0
      };
   }-*/;
   
   public final boolean isTitle()
   {
      return getType().equals(TITLE);
   }
   
   public final boolean isHeading()
   {
      return getType().equals(HEADING);
   }
   
   public final boolean isHR()
   {
      return getType().equals(HR);
   }
   
   public final boolean isCursor()
   {
      return getType().equals(CURSOR);
   }
      
   public final native String getType() /*-{
      return this.type;
   }-*/;
 
   public final native int getLevel() /*-{
      return this.level;
   }-*/;
}
