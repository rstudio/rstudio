/*
 * CppCompletion.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import org.rstudio.studio.client.common.icons.code.CodeIcons;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.ImageResource;

public class CppCompletion extends JavaScriptObject
{
   protected CppCompletion()
   {
   }
   
   public static native final CppCompletion create(String typedText) /*-{
      return {
         type: 0,
         typed_text: typedText,
         text: null
      };
   }-*/;
   
   public static native final CppCompletion createSnippetCompletion(
         String snippetName, String snippetContent) /*-{
      return {
         type: 99,
         typed_text: snippetName,
         text: [{
            text: snippetContent,
            comment: null
         }] 
      };
   }-*/;
         
   // completion types
   public static final int UNKNOWN = 0;
   public static final int VARIABLE = 1;
   public static final int FUNCTION = 2;
   public static final int CONSTRUCTOR = 3;
   public static final int DESTRUCTOR = 4;
   public static final int CLASS = 5;
   public static final int STRUCT = 6;
   public static final int NAMESPACE = 7;
   public static final int ENUM = 8;
   public static final int ENUM_VALUE = 9;
   public static final int KEYWORD = 10;
   public static final int MACRO = 11;
   
   public static final int SNIPPET = 99;
   
   public native final int getType() /*-{
       return this.type;
   }-*/;    
   
   public final ImageResource getIcon()
   {
      CodeIcons icons = CodeIcons.INSTANCE;
      switch(getType())
      {
      case UNKNOWN:
         return icons.keyword();
      case VARIABLE:
         return icons.variable();
      case FUNCTION:
      case CONSTRUCTOR:
      case DESTRUCTOR:
         return icons.function();
      case CLASS:
      case STRUCT:
         return icons.clazz();
      case NAMESPACE:
         return icons.namespace();
      case ENUM:
         return icons.enumType();
      case ENUM_VALUE:
         return icons.enumValue();
      case KEYWORD:
         return icons.keyword();
      case MACRO:
         return icons.macro();
      case SNIPPET:
         return icons.snippet();
      default:
         return icons.keyword();
      }
   }
    
   public final boolean hasParameters()
   {
      if (getType() == FUNCTION)
      {
         JsArray<CppCompletionText> textEntries = getText();
         for (int i = 0; i < textEntries.length(); i++)
         {
            String text = textEntries.get(i).getText();
            if (!text.endsWith("()") && !text.endsWith("() const"))
               return true;
         }
      }
      
      return false;
   }
   
   public native final String getTypedText() /*-{
      return this.typed_text;
   }-*/;
   
   public native final JsArray<CppCompletionText> getText() /*-{
      return this.text;
   }-*/;
}
