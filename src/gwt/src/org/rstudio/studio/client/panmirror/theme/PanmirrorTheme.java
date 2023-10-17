/*
 * PanmirrorTheme.java
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

package org.rstudio.studio.client.panmirror.theme;

import jsinterop.annotations.JsType;

// https://github.com/quarto-dev/quarto/blob/main/packages/editor/src/editor/editor-theme.ts

@JsType
public class PanmirrorTheme
{
   public Boolean  darkMode;
   public Boolean  highContrast;
   public Boolean  solarizedMode;
   public String   cursorColor;
   public String   selectionColor;
   public String   selectionForegroundColor;
   public String   nodeSelectionColor;
   public String   backgroundColor;
   public String   metadataBackgroundColor;
   public String   chunkBackgroundColor;
   public String   spanBackgroundColor;
   public String   divBackgroundColor;
   public String   commentColor;
   public String   commentBackgroundColor;
   public String   hrBackgroundColor;
   public String   gutterBackgroundColor;
   public String   gutterTextColor;
   public String   toolbarBackgroundColor;
   public String   toolbarTextColor;
   public String   textColor;
   public String   lightTextColor;
   public String   disabledTextColor;
   public String   placeholderTextColor;
   public String   invisibleTextColor;
   public String   linkTextColor;
   public String   surfaceWidgetTextColor;
   public String   markupTextColor;
   public String   findTextBackgroundColor;
   public String   findTextBorderColor;
   public String   borderBackgroundColor;
   public String   blockBorderColor;
   public String   focusOutlineColor;
   public String   paneBorderColor;
   public String   fixedWidthFont;
   public Number   fixedWidthFontSizePt;
   public String   proportionalFont;
   public Number   proportionalFontSizePt;
   public String   suggestWidgetBackgroundColor;
   public String   suggestWidgetBorderColor;
   public String   suggestWidgetForegroundColor;
   public String   suggestWidgetFocusHighlightForegroundColor;
   public String   suggestWidgetHighlightForegroundColor;
   public String   suggestWidgetSelectedBackgroundColor;
   public String   suggestWidgetSelectedForegroundColor;
   public String   suggestWidgetSelectedIconForegroundColor ;
   public String   symbolIconClassForegroundColor;
   public String   symbolIconConstantForegroundColor;
   public String   symbolIconEnumForegroundColor;
   public String   symbolIconFunctionForegroundColor;
   public String   symbolIconInterfaceForegroundColor;
   public String   symbolIconKeywordForegroundColor;
   public String   symbolIconMethodForegroundColor;
   public String   symbolIconNamespaceForegroundColor;
   public String   symbolIconPropertyForegroundColor;
   public String   symbolIconTextForegroundColor;
   public String   symbolIconTypeParameterForegroundColor;
   public String   symbolIconVariableForegroundColor;
   public String   debugStartForegroundColor;
   public String   debugStepForgroundColor;
   
   public PanmirrorThemeCode code;
}
