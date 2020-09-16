/*
 * PanmirrorSetMarkdownResult.java
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

package org.rstudio.studio.client.panmirror;


import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorSetMarkdownResult
{    
   public String canonical;
   public String line_wrapping;
   public String[] unrecognized;
   public JsObject unparsed_meta;
   
   public static final String kLineWrappingNone = "none";
   public static final String kLineWrappingColumn= "column";
   public static final String kLineWrappingSentence = "sentence";
}
