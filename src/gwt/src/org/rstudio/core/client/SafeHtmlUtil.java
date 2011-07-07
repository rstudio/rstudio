/*
 * SafeHtmlUtil.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

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
}

