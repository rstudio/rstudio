/*
 * SafeHtmlUtil.java
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
package org.rstudio.core.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class SafeHtmlUtil
{
   public static void appendDiv(SafeHtmlBuilder sb, 
                                String style, 
                                String textContent)
   {
      sb.append(createOpenTag("div",
                              "class", style));
      sb.appendEscaped(textContent);
      sb.appendHtmlConstant("</div>");
   }
   
   public static void appendDiv(SafeHtmlBuilder sb, 
                                String style, 
                                SafeHtml htmlContent)
   {
      sb.append(createOpenTag("div",
                              "class", style));
      sb.append(htmlContent);
      sb.appendHtmlConstant("</div>");
   }
   
   public static void appendSpan(SafeHtmlBuilder sb, 
                                 String style,
                                 String textContent)
   {
      sb.append(SafeHtmlUtil.createOpenTag("span", 
                                           "class", style));
      sb.appendEscaped(textContent);
      sb.appendHtmlConstant("</span>");   
   }
   
   public static void appendSpan(SafeHtmlBuilder sb, 
                                 String style,
                                 SafeHtml htmlContent)
   {
      sb.append(SafeHtmlUtil.createOpenTag("span", 
                                           "class", style));
      sb.append(htmlContent);
      sb.appendHtmlConstant("</span>");   
   }

   public static void appendImage(SafeHtmlBuilder sb,
                                  String style,
                                  ImageResource image)
   {
      sb.append(SafeHtmlUtil.createOpenTag("img",
                                           "class", style,
                                           "width", Integer.toString(image.getWidth()),
                                           "height", Integer.toString(image.getHeight()),
                                           "src", image.getSafeUri().asString()));
      sb.appendHtmlConstant("</img>");   
   }

   public static SafeHtml createOpenTag(String tagName,
                                        String... attribs)
   {
      StringBuilder builder = new StringBuilder();
      builder.append("<").append(tagName);
      for (int i = 0; i < attribs.length; i += 2)
      {
         builder.append(' ')
               .append(SafeHtmlUtils.htmlEscape(attribs[i]))
               .append("=\"")
               .append(SafeHtmlUtils.htmlEscape(attribs[i+1]))
               .append("\"");
      }
      builder.append(">");
      return SafeHtmlUtils.fromTrustedString(builder.toString());
   }
   
   public static SafeHtml createDiv(String... attribs)
   {
      return createOpenTag("div", attribs);
   }

   public static SafeHtml createEmpty()
   {
      return SafeHtmlUtils.fromSafeConstant("");
   }

   public static SafeHtml concat(SafeHtml... pieces)
   {
      StringBuilder builder = new StringBuilder();
      for (SafeHtml piece : pieces)
      {
         if (piece != null)
            builder.append(piece.asString());
      }
      return SafeHtmlUtils.fromTrustedString(builder.toString());
   }
   
   public static SafeHtml createStyle(String... strings)
   {
      StringBuilder builder = new StringBuilder();
      for (int i = 0, n = strings.length; i < n; i += 2)
      {
         String key = strings[i];
         String value = strings[i + 1];
         
         builder.append(SafeHtmlUtils.htmlEscape(key))
                .append(": ")
                .append(SafeHtmlUtils.htmlEscape(value))
                .append("; ");
      }
      return SafeHtmlUtils.fromTrustedString(builder.toString());
   }
}

