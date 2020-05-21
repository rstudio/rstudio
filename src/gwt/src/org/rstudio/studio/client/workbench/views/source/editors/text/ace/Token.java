/*
 * Token.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;

public class Token extends JavaScriptObject
{
   protected Token() {}
   
   public static native final Token create() /*-{
      return {
         "value": "",
         "type": "",
         "column": 0
      };
   }-*/;
   
   public static native final Token create(String value,
                                           String type,
                                           int column) /*-{
      return {
         "value": value,
         "type": type,
         "column": column
      };
   }-*/;

   public native final String getValue() /*-{
      return this.value;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;
   
   public native final int getColumn() /*-{
      return this.column;
   }-*/;
   
   public final boolean valueEquals(String value)
   {
      return value == getValue();
   }
   
   public final boolean valueMatches(String pattern)
   {
      return getValue().matches(pattern);
   }
   
   public final boolean hasAllTypes(String... types)
   {
      String tokenType = getType();
      if (StringUtil.isNullOrEmpty(tokenType))
         return false;
      
      for (String type : types)
      {
         boolean hasType =
               tokenType == type ||
               tokenType.contains(type + ".") ||
               tokenType.contains("." + type);
         
         if (!hasType)
            return false;
      }
      
      return true;
   }
   
   public final boolean hasType(String... types)
   {
      String tokenType = getType();
      if (StringUtil.isNullOrEmpty(tokenType))
         return false;
      
      for (String type : types)
      {
         if (tokenType.equals(type) ||
             tokenType.contains(type + ".") ||
             tokenType.contains("." + type))
         {
            return true;
         }
      }
      return false;
   }
   
   public final boolean typeEquals(String type)
   {
      return type.equals(getType());
   }
   
   public native final boolean isLeftBracket() /*-{
      return this.value && (
         this.value === "{" ||
         this.value === "(" ||
         this.value === "["
      );
   }-*/;
   
   public native final boolean isRightBracket() /*-{
      return this.value && (
         this.value === "}" ||
         this.value === ")" ||
         this.value === "]"
      );
   }-*/;
   
   public native final boolean isLeftAssign() /*-{
      return this.value && (
         this.value === "=" ||
         this.value === "<-"
      );
   }-*/;
   
   public native final boolean isValidForFunctionCall() /*-{
      return this.type && (
         this.type.indexOf("identifier") !== -1 ||
         this.type === "string" ||
         this.type === "keyword"
      );
   }-*/;
   
   public native final boolean isExtractionOperator() /*-{
      return this.value && (
         this.value === "$" ||
         this.value === "@" ||
         this.value === "?" ||
         this.value === "~"
      );
   }-*/;
   
   public final String asString()
   {
      return "'" + getType() + "' -> '" + getValue() + "'";
   }
   
   // NOTE: Tokens attached to a document should be considered immutable;
   // use setters only when applying to a tokenized line separate from an
   // active editor!
   public native final void setValue(String value) /*-{
      this.value = value;
   }-*/;
}
