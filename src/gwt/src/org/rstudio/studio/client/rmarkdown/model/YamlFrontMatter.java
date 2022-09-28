/*
 * YamlFrontMatter.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rmarkdown.model;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class YamlFrontMatter
{
   // front matter can end with ... rather than ---; see spec:
   // http://www.yaml.org/spec/1.2/spec.html#id2760395
   //
   // note that these can't be statically initialized as fully compiled
   // regexes due to state reuse
   private static String frontMatterBeginRegex = "^---\\s*$";
   private static String frontMatterEndRegex = "^(---|\\.\\.\\.)\\s*$";

   public static Range getFrontMatterRange(DocDisplay display)
   {
      RegExp frontMatterBegin = RegExp.compile(frontMatterBeginRegex, "gm");
      RegExp frontMatterEnd = RegExp.compile(frontMatterEndRegex, "gm");

      Position begin = null;
      Position end = null;
      
      for (int i = 0; i < display.getRowCount(); i++)
      {
         String code = display.getLine(i);
         if (begin == null)
         {
            // haven't found front matter begin yet; test this line
            MatchResult beginMatch = frontMatterBegin.exec(code);
            if (beginMatch == null)
            {
               // ensure that only whitespace exists before the beginning --- 
               // (this matches front matter extraction behavior in the R
               // Markdown package)
               if (!code.matches("\\s*"))
                  break;
            }
            else
            {
               begin = Position.create(i + 1, 0);
               continue;
            }
         }
         else if (end == null)
         {
            // haven't found front matter end yet; test this line
            MatchResult endMatch = frontMatterEnd.exec(code);
            if (endMatch != null)
            {
               end = Position.create(i, 0);
               break;
            }
         }
      }
      
      if (begin == null || end == null)
         return null;
      
      return Range.fromPoints(begin, end);
   }
   
   public static String getFrontMatter(String document)
   {
      RegExp frontMatterBegin = RegExp.compile(frontMatterBeginRegex, "gm");
      RegExp frontMatterEnd = RegExp.compile(frontMatterEndRegex, "gm");

      ArrayList<String> frontMatter = new ArrayList<String>();
      for (String line : StringUtil.getLineIterator(document))
      {
         if (frontMatter.size() == 0)
         {
            MatchResult beginMatch = frontMatterBegin.exec(line);
            if (beginMatch != null)
            {
               frontMatter.add(line);
            }
            else
            {
               if (!line.matches("\\s*"))
                  return null;
            }
         }
         else
         {
            MatchResult endMatch = frontMatterEnd.exec(line);
            if (endMatch != null)
            {
               return StringUtil.join(frontMatter.subList(1, frontMatter.size()), "\n");
            }
            else
            {
               frontMatter.add(line);
            }
         }
      }
      return null;
   }
   
   public static String getFrontMatter(DocDisplay display)
   {
      Range range = getFrontMatterRange(display);
      if (range == null)
      {
         return "output: html_document\n";
      } 
      else
      {
         return display.getTextForRange(range);
      }
   }
   
   /**
    * Replaces the document's front matter with the given front matter; 
    * adds a new front matter section if none exists.
    * 
    * @param display The editor to mutate
    * @param yaml    The new front matter
    * @return Whether the editor buffer was mutated
    */
   public static boolean applyFrontMatter(DocDisplay display, String yaml) 
   {
      if (yaml == null || yaml.isEmpty())
         return false;
      
      Range range = YamlFrontMatter.getFrontMatterRange(display);
      if (range == null)
      {
         // add the YAML if no front matter exists
         range = Range.create(0, 0, 0, 0);
         yaml = RmdFrontMatter.FRONTMATTER_SEPARATOR +
                yaml +
                RmdFrontMatter.FRONTMATTER_SEPARATOR;
      }
      else if (display.getTextForRange(range) == yaml)
      {
         return false;
      }

      display.replaceRange(range, yaml);
      return true;
   }
}
