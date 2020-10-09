/*
 * PanmirrorPandocFormatConfig.java
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

package org.rstudio.studio.client.panmirror.uitools;

import java.util.Arrays;

import org.rstudio.core.client.StringUtil;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorPandocFormatConfig
{
   // editor behavior
   public String mode;
   public String extensions;
   public String rmdExtensions;
   public String[] doctypes;
   
   // markdown writing
   public String wrap;
   public String references_location;
   public String references_prefix;
   public boolean canonical;
   
 
   public static boolean isDoctype(PanmirrorPandocFormatConfig config, String doctype)
   {
      return config.doctypes != null && Arrays.asList(config.doctypes).contains(doctype);
   }
   
   public static boolean areEqual(PanmirrorPandocFormatConfig a, PanmirrorPandocFormatConfig b)
   {
      return editorBehaviorConfigEqual(a, b) && markdownWritingConfigEqual(a, b);
   }
   
   public static boolean editorBehaviorConfigEqual(PanmirrorPandocFormatConfig a, PanmirrorPandocFormatConfig b)
   {
      String aDoctypes = a.doctypes != null ? String.join(",", a.doctypes) : "";
      String bDoctypes = b.doctypes != null ? String.join(",", b.doctypes) : "";
      return StringUtil.equals(a.mode, b.mode) &&
             StringUtil.equals(a.extensions, b.extensions) &&
             StringUtil.equals(a.rmdExtensions, b.rmdExtensions) &&
             aDoctypes == bDoctypes;       
   }
   
   public static boolean markdownWritingConfigEqual(PanmirrorPandocFormatConfig a, PanmirrorPandocFormatConfig b)
   {
      return StringUtil.equals(a.wrap, b.wrap) &&
             a.references_location == b.references_location &&
             a.references_prefix == b.references_prefix;         
   }
}

