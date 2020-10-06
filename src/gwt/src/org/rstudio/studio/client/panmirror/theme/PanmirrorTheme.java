/*
 * PanmirrorTheme.java
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

package org.rstudio.studio.client.panmirror.theme;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorTheme
{
   public boolean darkMode;
   public boolean solarizedMode;
   public String cursorColor;
   public String selectionColor;
   public String nodeSelectionColor;
   public String backgroundColor;
   public String metadataBackgroundColor;
   public String chunkBackgroundColor;
   public String spanBackgroundColor;
   public String divBackgroundColor;
   public String commentColor;
   public String commentFontStyle;
   public String commentBackgroundColor;
   public String gutterBackgroundColor;
   public String gutterTextColor;
   public String textColor;
   public String surfaceWidgetTextColor;
   public String lightTextColor;
   public String linkTextColor;
   public String placeholderTextColor;
   public String invisibleTextColor;
   public String markupTextColor;
   public String findTextBackgroundColor;
   public String findTextBorderColor;
   public String borderBackgroundColor;
   public String blockBorderColor;
   public String focusOutlineColor;
   public String paneBorderColor;
   public String fixedWidthFont;
   public double fixedWidthFontSizePt;
   public String proportionalFont;
   public double proportionalFontSizePt;
   public PanmirrorThemeCode code;
}
