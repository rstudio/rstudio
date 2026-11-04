/*
 * KeySymbolLegend.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.CoreClientConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

/**
 * Names the macOS key symbols that KeyCombination uses when it formats
 * shortcuts for display on macOS.
 */
public class KeySymbolLegend extends Composite
{
   public KeySymbolLegend()
   {
      // keep in sync with the symbols in KeyCombination.toString and getKeyName
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.appendHtmlConstant("<dl>");
      appendSymbol(sb, "&#8984;", constants_.keySymbolCommand());
      appendSymbol(sb, "&#8997;", constants_.keySymbolOption());
      appendSymbol(sb, "&#8679;", constants_.keySymbolShift());
      appendSymbol(sb, "&#8963;", constants_.keySymbolControl());
      appendSymbol(sb, "&#8617;", constants_.keySymbolReturn());
      appendSymbol(sb, "&#8677;", constants_.keySymbolTab());
      appendSymbol(sb, "&#9003;", constants_.keySymbolDelete());
      appendSymbol(sb, "&#8998;", constants_.keySymbolForwardDelete());
      sb.appendHtmlConstant("</dl>");

      HTML html = new HTML(sb.toSafeHtml());
      html.addStyleName(ShortcutReferenceDialog.RES.styles().legend());
      initWidget(html);
   }

   private static void appendSymbol(SafeHtmlBuilder sb, String symbol, String name)
   {
      sb.appendHtmlConstant("<div><dt>" + symbol + "</dt><dd>");
      sb.appendEscaped(name);
      sb.appendHtmlConstant("</dd></div>");
   }

   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
