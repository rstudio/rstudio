/*
 * YamlFrontMatter.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.model;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class YamlFrontMatter
{
   public static int[] getFrontMatterRange(String code)
   {
      RegExp frontMatterReg = RegExp.compile("^---\\s*$", "gm");
      MatchResult beginMatch = frontMatterReg.exec(code);
      if (beginMatch == null)
         return null;

      // ensure that only whitespace exists before the beginning --- 
      // (this matches front matter extraction behavior in the R Markdown
      // package)
      if (!code.substring(0, beginMatch.getIndex()).matches("\\s*")) 
         return null;
      
      MatchResult endMatch = frontMatterReg.exec(code);
      if (endMatch == null)
         return null;

      // the YAML range extends one character past the end of the first match
      // (i.e. after the newline) up to the second match
      return new int[] { beginMatch.getIndex() + beginMatch.getGroup(0).length() + 1, 
            endMatch.getIndex() };
   }
   
   public static String getFrontMatter(String code)
   {
      int[] range = getFrontMatterRange(code);
      if (range == null)
      {
         return "output: html_document\n";
      } 
      else
      {
         return code.substring(range[0], range[1]);
      }
   }
   
   public static String applyFrontMatter(String code, String yaml) 
   {
      if (yaml == null || yaml.isEmpty())
         return code;
      
      int[] range = YamlFrontMatter.getFrontMatterRange(code);
      if (range == null)
      {
         // add the YAML if no front matter exists
         code = RmdFrontMatter.FRONTMATTER_SEPARATOR +
                yaml +
                RmdFrontMatter.FRONTMATTER_SEPARATOR +
                code;
      }
      else
      {
         // replace the front matter if already present
         code = code.substring(0, range[0]) + yaml + 
                code.substring(range[1], code.length());
      }
      return code;
   }
}
