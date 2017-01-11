/*
 * DialogHtmlSanitizer.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.common.rstudioapi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.safehtml.shared.HtmlSanitizer;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public final class DialogHtmlSanitizer implements HtmlSanitizer {
   private static final Set<String> TAG_WHITELIST = new HashSet<String>(
      Arrays.asList(
         "p", "em", "strong", "b", "i"
      )
   );

   public static SafeHtml sanitizeHtml(String html) {
      if (StringUtil.isNullOrEmpty(html)) {
         html = "";
      }
      
      return SafeHtmlUtils.fromTrustedString(dialogSanitize(html));
   }

   private static String dialogSanitize(String text) {
      StringBuilder sanitized = new StringBuilder();

      boolean firstSegment = true;
      for (String segment : text.split("<", -1)) {
         if (firstSegment) {
            firstSegment = false;
            sanitized.append(SafeHtmlUtils.htmlEscapeAllowEntities(segment));
            continue;
         }

         int tagStart = 0;
         int tagEnd = segment.indexOf('>');
         String tag = null;
         boolean isValidTag = false;
         if (tagEnd > 0) {
            if (segment.charAt(0) == '/') {
               tagStart = 1;
            }
            tag = segment.substring(tagStart, tagEnd).toLowerCase();
            if (TAG_WHITELIST.contains(tag)) {
               isValidTag = true;
            }
         }

         if (isValidTag) {
            if (tagStart == 0) {
               sanitized.append('<');
            } else {
               sanitized.append("</");
            }
            sanitized.append(tag).append('>');

            sanitized.append(SafeHtmlUtils.htmlEscapeAllowEntities(
            segment.substring(tagEnd + 1)));
         } else {
            sanitized.append("&lt;").append(
            SafeHtmlUtils.htmlEscapeAllowEntities(segment));
         }
      }

      return sanitized.toString();
   }

   private DialogHtmlSanitizer() {
   }

   public SafeHtml sanitize(String html) {
      return sanitizeHtml(html);
   }
}