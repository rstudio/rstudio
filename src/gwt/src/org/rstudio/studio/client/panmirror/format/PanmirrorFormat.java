/*
 * PanmirrorMarkdownFormat.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.format;


import org.rstudio.core.client.StringUtil;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorFormat
{   
   public String pandocMode;
   public String pandocExtensions;
   public PanmirrorRmdExtensions rmdExtensions;
   public PanmirrorHugoExtensions hugoExtensions;
   public String[] docTypes;
   
   public static boolean areEqual(PanmirrorFormat a, PanmirrorFormat b)
   {
      String aDoctypes = a.docTypes != null ? String.join(",", a.docTypes) : "";
      String bDoctypes = b.docTypes != null ? String.join(",", b.docTypes) : "";
      return StringUtil.equals(a.pandocMode, b.pandocMode) &&
             StringUtil.equals(a.pandocExtensions, b.pandocExtensions) &&
             PanmirrorRmdExtensions.areEqual(a.rmdExtensions, b.rmdExtensions) &&
             PanmirrorHugoExtensions.areEqual(a.hugoExtensions, b.hugoExtensions) &&
             aDoctypes == bDoctypes;       
   }
}

