/*
 * CodeMirrorBraceMatcher.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.codemirror;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import org.rstudio.studio.client.workbench.views.console.shell.BraceMatcher;

import java.util.HashMap;

public class CodeMirrorBraceMatcher extends BraceMatcher<Node>
{
   protected CodeMirrorBraceMatcher(boolean forward,
                                    String openStr,
                                    String closeStr)
   {
      super(forward);
      openStr_ = openStr;
      closeStr_ = closeStr;
   }

   @Override
   protected int value(Node token)
   {
      if (token.getNodeType() != Node.ELEMENT_NODE)
         return 0;

      Element el = (Element) token;

      if (!"bracket".equals(el.getClassName()))
         return 0;

      if (!"span".equals(el.getTagName().toLowerCase()))
         return 0;

      String text = el.getInnerText().trim();
      return text.equals(openStr_) ? 1 : text.equals(closeStr_) ? -1 : 0;
   }

   public static CodeMirrorBraceMatcher createForToken(SpanElement span)
   {
      if (!"bracket".equals(span.getClassName()))
         return null;

      String text = span.getInnerText();
      return table.get(text.trim());
   }

   public static SpanElement findMatch(SpanElement span)
   {
      CodeMirrorBraceMatcher matcher = createForToken(span);
      if (matcher == null)
         return null;
      return (SpanElement) matcher.match(new NodeTokenSource(span));
   }

   private final String openStr_;
   private final String closeStr_;

   private static final HashMap<String, CodeMirrorBraceMatcher> table
         = new HashMap<String, CodeMirrorBraceMatcher>();
   static
   {
      table.put("{", new CodeMirrorBraceMatcher(true, "{", "}"));
      table.put("[", new CodeMirrorBraceMatcher(true, "[", "]"));
      table.put("(", new CodeMirrorBraceMatcher(true, "(", ")"));
      table.put("[[", new CodeMirrorBraceMatcher(true, "[[", "]]"));
      table.put("}", new CodeMirrorBraceMatcher(false, "{", "}"));
      table.put("]", new CodeMirrorBraceMatcher(false, "[", "]"));
      table.put(")", new CodeMirrorBraceMatcher(false, "(", ")"));
      table.put("]]", new CodeMirrorBraceMatcher(false, "[[", "]]"));
   }
}
