/*
 * RawFormatSelect.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror.dialogs;

import java.util.ArrayList;
import java.util.Arrays;

import org.rstudio.core.client.widget.SelectWidget;

public class RawFormatSelect extends SelectWidget
{
   public RawFormatSelect()
   {
      super("Format:", getOptions(), getValues(), false);
   }
   
   private static String[] getValues()
   {
      return getFormatList("");
   }
   
   private static String[] getOptions()
   {
      return getFormatList("(Choose Format)");
   }
   
   private static String[] getFormatList(String firstItem)
   {
      ArrayList<String> options = new ArrayList<String>();
      options.add(firstItem);
      options.addAll(Arrays.asList(formats_));
      return options.toArray(new String[]{});
   }
   
   
   private static String[] formats_ = {
      "asciidoc",
      "asciidoctor",
      "beamer",
      "commonmark",
      "context",
      "docbook",
      "docbook4",
      "docbook5",
      "docx",
      "dokuwiki",
      "dzslides",
      "epub",
      "epub2",
      "epub3",
      "fb2",
      "gfm",
      "haddock",
      "html",
      "html4",
      "html5",
      "icml",
      "ipynb",
      "jats",
      "jira",
      "json",
      "latex",
      "man",
      "markdown",
      "markdown_github",
      "markdown_mmd",
      "markdown_phpextra",
      "markdown_strict",
      "mediawiki",
      "ms",
      "muse",
      "native",
      "odt",
      "openxml",
      "opendocument",
      "opml",
      "org",
      "plain",
      "pptx",
      "revealjs",
      "rst",
      "rtf",
      "s5",
      "slideous",
      "slidy",
      "tei",
      "texinfo",
      "textile",
      "xwiki",
      "zimwiki",      
   };
}
