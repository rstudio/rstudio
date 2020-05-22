/*
 * SafeHtmlUtil.java
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
package org.rstudio.core.client;

import java.util.Set;
import java.util.TreeSet;

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
   
   /**
    * Appends text to a SafeHtmlBuilder with search matches highlighted.
    * 
    * @param sb The SafeHtmlBuilder to append the search match to
    * @param haystack The text to append. 
    * @param needle The text to search for and highlight.
    * @param matchClass The CSS class to assign to matches.
    */
   public static void highlightSearchMatch(SafeHtmlBuilder sb, String haystack, 
                                           String needle, String matchClass)
   {
      // do nothing if we weren't given a string
      if (StringUtil.isNullOrEmpty(haystack))
         return;
      
      // if we have a needle to search for, and it exists, highlight it
      boolean hasMatch = false;
      if (!StringUtil.isNullOrEmpty(needle))
      {
         int idx = haystack.toLowerCase().indexOf(needle);
         if (idx >= 0)
         {
            hasMatch = true;
            sb.appendEscaped(haystack.substring(0, idx));
            sb.appendHtmlConstant(
                  "<span class=\"" + matchClass + "\">");
            sb.appendEscaped(haystack.substring(idx, 
                  idx + needle.length()));
            sb.appendHtmlConstant("</span>");
            sb.appendEscaped(haystack.substring(idx + needle.length(), 
                  haystack.length()));
         }
      }
      
      // needle not found; append text directly
      if (!hasMatch)
         sb.appendEscaped(haystack);
   }

   /**
    * Appends text to a SafeHtmlBuilder with multiple search matches highlighted.
    * 
    * @param sb The SafeHtmlBuilder to append the search match to
    * @param haystack The text to append. 
    * @param needles The strings to search for and highlight.
    * @param matchClass The CSS class to assign to matches.
    */
   public static void highlightSearchMatch(SafeHtmlBuilder sb, String haystack, 
                                           String[] needles, String matchClass)
   {
      // Do nothing if we weren't given a string
      if (StringUtil.isNullOrEmpty(haystack))
         return;
      
      // Inner class representing a search match found in the haystack
      class SearchMatch
      {
         public SearchMatch(int indexIn, int lengthIn)
         {
            index = indexIn;
            length = lengthIn;
         }
         public Integer index;
         public Integer length;
      };
      
      // Store matches in a tree set ordered by the index at which the match was
      // found.
      Set<SearchMatch> matches = new TreeSet<SearchMatch>(
            (SearchMatch o1, SearchMatch o2) -> {
                  return o1.index.compareTo(o2.index);
            });

      // Find all the matches and add them to the result set.
      for (int i = 0; i < needles.length; i++)
      {
         int idx = haystack.toLowerCase().indexOf(needles[i]);
         if (idx >= 0)
         {
            int endIdx = idx + needles[i].length();

            // Check the existing set of matches; if this overlaps with an
            // existing match we don't want to create overlapping match results.
            boolean overlaps = false;
            for (SearchMatch match: matches)
            {
               if (match.index >= endIdx)
               {
                  // Performance optimization: neither this match nor any
                  // following can overlap since it starts after this match ends
                  // (and matches are sorted by start index.)
                  break;
               }

               // If this match overlaps an existing match, merge it into that
               // match instead of creating a new match.
               int overlap = Math.min(endIdx, match.index + match.length) -
                             Math.max(idx, match.index);
               if (overlap > 0)
               {
                  // The match starts at the earlier of the indices
                  match.index = Math.min(match.index, idx);
                  
                  // The match's new length is the distance to its new endpoint
                  // (the greater of the two matches we're merging)
                  match.length = Math.max(endIdx,  match.index + match.length) - 
                        match.index;
                        
                  overlaps = true;
                  break;
               }
            }

            // If this match does not overlap any existing matches, add it as a
            // new match.
            if (!overlaps)
            {
               matches.add(new SearchMatch(idx, needles[i].length()));
            }
         }
      }
      
      // Build the HTML from the input string and the found matches.
      if (matches.size() > 0)
      {
         int idx = 0;
         for (SearchMatch match: matches)
         {
            // Emit all the text from the last index to the beginning of this
            // match. 
            sb.appendEscaped(haystack.substring(idx, match.index));
            
            // Emit the match itself.
            idx = match.index;
            sb.appendHtmlConstant(
                  "<span class=\"" + matchClass + "\">");
            sb.appendEscaped(haystack.substring(idx, 
                  idx + match.length));
            sb.appendHtmlConstant("</span>");
            
            // Move the index to the end of this match
            idx += match.length;
         }
         
         // Emit the text from end of the last match to the end of the string
         sb.appendEscaped(haystack.substring(idx, haystack.length()));
      }
      else
      {
         // We found no matches at all. Just emit the string into the builder.
         sb.appendEscaped(haystack);
      }
   }
}

