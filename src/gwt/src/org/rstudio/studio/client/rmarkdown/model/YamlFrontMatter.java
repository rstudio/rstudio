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

public class YamlFrontMatter
{
   public static int[] getFrontMatterRange(String code)
   {
      String separator = RmdFrontMatter.FRONTMATTER_SEPARATOR;
      int beginPos = code.indexOf(separator) + separator.length();
      if (beginPos < 0)
         return null;
      int endPos = code.indexOf(separator, beginPos);
      if (endPos < 0)
         return null;
      return new int[] { beginPos, endPos };
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
