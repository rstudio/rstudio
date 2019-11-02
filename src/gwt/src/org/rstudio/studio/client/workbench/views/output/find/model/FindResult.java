/*
 * FindResult.java
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
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;

import java.util.ArrayList;

public class FindResult extends JavaScriptObject
{
   public static native FindResult create(String file,
                                          int line,
                                          String lineValue) /*-{
      return ({
         file: file,
         line: line,
         lineValue: lineValue,
         replace: "",
         replaceIndicator: false
      });
   }-*/;

   protected FindResult() {}

   public native final FindResult clone() /*-{
      return ({
         file: this.file,
         line: this.line,
         lineValue: this.lineValue,
         replaceIndicator: this.replaceIndicator,
         replace: this.replace,
         matchOn: this.matchOn,
         matchOff: this.matchOff,
         replaceMatchOn: this.replaceMatchOn,
         replaceMatchOff: this.replaceMatchOff
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

   public native final boolean getReplaceIndicator()/*-{
      return this.replaceIndicator;
   }-*/;

   public native final void setReplaceIndicator()/*-{
      this.replaceIndicator = true;
   }-*/;

   public final ArrayList<Integer> getMatchOns()
   {
      return getJavaArray("matchOn");
   }

   public final ArrayList<Integer> getMatchOffs()
   {
      return getJavaArray("matchOff");
   }

   public final ArrayList<Integer> getReplaceMatchOns()
   {
      return getJavaArray("replaceMatchOn");
   }

   public final ArrayList<Integer> getReplaceMatchOffs()
   {
      return getJavaArray("replaceMatchOff");
   }

   public final native void setReplace(String value) /*-{
      if (value)
         this.replace = value;
      else
         this.replace = "";
   }-*/;


   public final SafeHtml getLineHTML()
   {
      // !!! entire function is sloppy
      SafeHtmlBuilder out = new SafeHtmlBuilder();

      ArrayList<Integer> on = getMatchOns();
      ArrayList<Integer> off = getMatchOffs();
      ArrayList<Pair<Boolean, Integer>> parts
                                      = new ArrayList<Pair<Boolean, Integer>>();

      ArrayList<Integer> replaceOn = getReplaceMatchOns();
      ArrayList<Integer> replaceOff = getReplaceMatchOffs();
      ArrayList<Pair<Boolean, Integer>> replaceParts
                                      = new ArrayList<Pair<Boolean, Integer>>();

      int difference = 0;
      while (on.size() + off.size() > 0)
      {
         int onVal = on.size() == 0 ? Integer.MAX_VALUE : on.get(0);
         int offVal = off.size() == 0 ? Integer.MAX_VALUE : off.get(0);
         int replaceOnVal = replaceOn.size() == 0 ? Integer.MAX_VALUE : replaceOn.get(0);
         int replaceOffVal = replaceOff.size() == 0 ? Integer.MAX_VALUE : replaceOff.get(0);

         if (onVal <= offVal)
            parts.add(new Pair<Boolean, Integer>(true, on.remove(0)));
         else
            parts.add(new Pair<Boolean, Integer>(false, off.remove(0)));

         if (replaceOn.size() + replaceOff.size() > 0)
         {
            if (replaceOnVal <= replaceOffVal)
            {
               difference = offVal - onVal;
               replaceParts.add(new Pair<Boolean, Integer>(true, (replaceOn.remove(0) + difference)));
            }
            else
               replaceParts.add(new Pair<Boolean, Integer>(false, (replaceOff.remove(0) + difference)));
         }
      }

      String line = getLineValue();

      // Use a counter to ensure tags are balanced.
      int openStrongTags = 0;
      int openEmTags = 0;

      for (int i = 0; i < line.length(); i++)
      {
         while (parts.size() > 0 && parts.get(0).second == i)
         {
            if (parts.remove(0).first)
            {
               out.appendHtmlConstant("<strong>");
               openStrongTags++;
            }
            else if (openStrongTags > 0)
            {
               out.appendHtmlConstant("</strong>");
               openStrongTags--;
               String replace = getReplaceValue();
               if (!StringUtil.isNullOrEmpty(replace))
               {
                  out.appendHtmlConstant("<em>");
                  for (int j = 0; j < replace.length(); j++)
                     out.append(replace.charAt(j));
                  out.appendHtmlConstant("</em>");
               }
            }
         }
         while (replaceParts.size() > 0 && replaceParts.get(0).second == i)
         {
            if (replaceParts.remove(0).first)
            {
               out.appendHtmlConstant("<em>");
               openEmTags++;
            }
            else if (openEmTags > 0)
            {
               out.appendHtmlConstant("</em>");
               openEmTags--;
            }
         }
         out.append(line.charAt(i));
      }

      while (openStrongTags > 0)
      {
         openStrongTags--;
         out.appendHtmlConstant("</strong>");
         String replace = getReplaceValue();
         if (!StringUtil.isNullOrEmpty(replace))
         {
            out.appendHtmlConstant("<em>");
            for (int j = 0; j < replace.length(); j++)
               out.append(replace.charAt(j));
            out.appendHtmlConstant("</em>");
         }
      }

      while (openEmTags > 0)
      {
         openEmTags--;
         out.appendHtmlConstant("</em>");
      }

      return out.toSafeHtml();
   }


   public final SafeHtml getLineReplaceHTML()
   {
      SafeHtmlBuilder out = new SafeHtmlBuilder();

      ArrayList<Integer> on = getMatchOns();
      ArrayList<Integer> off = getMatchOffs();

      ArrayList<Integer> onReplace = getReplaceMatchOns();
      ArrayList<Integer> offReplace = getReplaceMatchOffs();

      ArrayList<Pair<Boolean, Integer>> parts
                                      = new ArrayList<Pair<Boolean, Integer>>();
      while (onReplace.size() + offReplace.size() > 0)
      {
         int onReplaceVal = onReplace.size() == 0 ? Integer.MAX_VALUE : onReplace.get(0);
         int offReplaceVal = offReplace.size() == 0 ? Integer.MAX_VALUE : offReplace.get(0);
         if (onReplaceVal <= offReplaceVal)
            parts.add(new Pair<Boolean, Integer>(true, onReplace.remove(0)));
         else
            parts.add(new Pair<Boolean, Integer>(false, offReplace.remove(0)));
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
               String replace = getReplaceValue();
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
