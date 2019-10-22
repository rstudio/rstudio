/*
 * ReplaceResult.java
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
package org.rstudio.studio.client.workbench.views.output.find.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;

import java.util.ArrayList;

public class ReplaceResult extends JavaScriptObject
{
   public static native ReplaceResult create(String file,
                                          int line,
                                          String lineValue) /*-{
      return ({
         file: file,
         line: line,
         lineValue: lineValue,
         replace: "true"
      });
   }-*/;

   protected ReplaceResult() {}

   public native final ReplaceResult clone() /*-{
      return ({
         file: this.file,
         line: this.line,
         lineValue: this.lineValue,
         replace: this.replace,
         matchOn: this.matchOn,
         matchOff: this.matchOff
      });
   }-*/;

   public native final String getFile() /*-{
      return this.file;
   }-*/;

   public native final int getLine() /*-{
      return this.line;
   }-*/;

   public native final String getLineValue() /*-{
      return this.lineValue;
   }-*/;

   public native final String getReplaceValue()/*-{
      return this.replace;
   }-*/;

   public final ArrayList<Integer> getMatchOns()
   {
      return getJavaArray("matchOn");
   }

   public final ArrayList<Integer> getMatchOffs()
   {
      return getJavaArray("matchOff");
   }

   public final native void setReplace(String value) /*-{
      if (value)
         this.replace = value;
      else
         this.replace = "";
   }-*/;


   public final SafeHtml getLineHTML()
   {
      SafeHtmlBuilder out = new SafeHtmlBuilder();

      ArrayList<Integer> on = getMatchOns();
      ArrayList<Integer> off = getMatchOffs();
      ArrayList<Pair<Boolean, Integer>> parts
                                      = new ArrayList<Pair<Boolean, Integer>>();
      while (on.size() + off.size() > 0)
      {
         int onVal = on.size() == 0 ? Integer.MAX_VALUE : on.get(0);
         int offVal = off.size() == 0 ? Integer.MAX_VALUE : off.get(0);
         if (onVal <= offVal)
            parts.add(new Pair<Boolean, Integer>(true, on.remove(0)));
         else
            parts.add(new Pair<Boolean, Integer>(false, off.remove(0)));
      }

      String line = getLineValue();

      // Use a counter to ensure tags are balanced.
      int openTags = 0;

      for (int i = 0; i < line.length(); i++)
      {
         while (parts.size() > 0 && parts.get(0).second == i)
         {
            if (parts.remove(0).first)
            {
               out.appendHtmlConstant("<em>");
               openTags++;
            }
            else if (openTags > 0)
            {
               out.appendHtmlConstant("</em>");
               openTags--;
            }
         }
         out.append(line.charAt(i));
      }

      while (openTags > 0)
      {
         openTags--;
         out.appendHtmlConstant("</em>");
      }

      return out.toSafeHtml();
   }

   private ArrayList<Integer> getJavaArray(String property)
   {
      JsArrayInteger array = getArray(property);
      ArrayList<Integer> ints = new ArrayList<Integer>();
      for (int i = 0; i < array.length(); i++)
         ints.add(array.get(i));
      return ints;
   }

   private native final JsArrayInteger getArray(String property) /*-{
      if (this == null)
         return [];
      return this[property] || [];
   }-*/;
}
