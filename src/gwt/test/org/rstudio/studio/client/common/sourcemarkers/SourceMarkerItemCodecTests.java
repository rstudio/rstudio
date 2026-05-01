/*
 * SourceMarkerItemCodecTests.java
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
package org.rstudio.studio.client.common.sourcemarkers;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.junit.client.GWTTestCase;

public class SourceMarkerItemCodecTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static class FakePrefs implements VirtualConsole.Preferences
   {
      @Override public int truncateLongLinesInConsoleHistory() { return 1000; }
      @Override public String consoleAnsiMode() { return UserPrefs.ANSI_CONSOLE_MODE_ON; }
      @Override public boolean screenReaderEnabled() { return false; }
   }

   private VirtualConsole vc(PreElement ele)
   {
      return new VirtualConsole(ele, new FakePrefs());
   }

   public void testRenderMessageHtmlBranchPreservesMarkup()
   {
      // When the server has marked a message as HTML (e.g. clang Find Usages
      // wrapping the matched symbol in <strong>), the codec must render the
      // markup as real DOM, not as escaped text.
      PreElement ele = Document.get().createPreElement();
      SourceMarkerItemCodec.renderMessage(vc(ele), true, "<strong>X</strong>");

      String html = ele.getInnerHTML();
      assertTrue("expected <strong> element in: " + html,
                 html.toLowerCase().contains("<strong>"));
      assertFalse("HTML branch must not escape markup, got: " + html,
                  html.contains("&lt;strong&gt;"));
   }

   public void testRenderMessagePlainBranchEscapesMarkup()
   {
      // Compiler/linter output is the untrusted case. The server escapes
      // angle brackets to &lt;/&gt; before sending; the client must render
      // those as literal text, never let them reach innerHTML as live tags.
      PreElement ele = Document.get().createPreElement();
      String serverEscaped = "&lt;script&gt;alert(1)&lt;&#x2F;script&gt;";
      SourceMarkerItemCodec.renderMessage(vc(ele), false, serverEscaped);

      String html = ele.getInnerHTML();
      assertFalse("plain branch must not produce a <script> element: " + html,
                  html.toLowerCase().contains("<script"));
      assertTrue("plain branch must render the decoded text as escaped HTML: " + html,
                 html.contains("&lt;script&gt;"));
   }

   public void testRenderMessagePlainBranchHandlesRawAngleBrackets()
   {
      // Defensive: even if a plain-text marker somehow reaches the client
      // without server-side escaping (e.g. an upstream bug), the codec must
      // still treat it as text and not parse it as HTML.
      PreElement ele = Document.get().createPreElement();
      SourceMarkerItemCodec.renderMessage(vc(ele), false, "<img src=x onerror=alert(1)>");

      String html = ele.getInnerHTML();
      assertFalse("plain branch must not produce an <img> element: " + html,
                  html.toLowerCase().contains("<img"));
   }
}
