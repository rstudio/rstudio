/*
 * CodeMirrorStringMatcher.java
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

public class CodeMirrorStringMatcher extends BraceMatcher<Node>
{
   protected CodeMirrorStringMatcher(boolean forward,
                                     boolean singleQuote)
   {
      super(forward, 1);
      startClass_ = "str-" +
                     (forward ? "start" : "end") +
                     "-" +
                     (singleQuote ? "single" : "double");
      targetClass_ = "str-" +
                     (forward ? "end" : "start") +
                     "-" +
                     (singleQuote ? "single" : "double");
   }

   @Override
   protected int value(Node token)
   {
      if (token.getNodeType() == Node.ELEMENT_NODE
            && token.getNodeName().toLowerCase().equals("span"))
      {
         Element el = ((Element)token);
         String className = el.getClassName();
         if (startClass_.equals(className))
            return 1;
         if (targetClass_.equals(className))
            return -1;
         else if (className.equals("str") || className.equals("whitespace"))
            return 0;

         // break out--we left the string
         return 100;
      }

      return 0;
   }

   private final String startClass_;
   private final String targetClass_;



   public static SpanElement findMatch(SpanElement span)
   {
      CodeMirrorStringMatcher matcher = createForToken(span);
      if (matcher == null)
         return null;
      return (SpanElement) matcher.match(new NodeTokenSource(span));
   }

   private static CodeMirrorStringMatcher createForToken(SpanElement span)
   {
      String className = span.getClassName();
      if (!className.startsWith("str-"))
         return null;

      boolean forward;
      if (className.startsWith("str-start"))
         forward = true;
      else if (className.startsWith("str-end"))
         forward = false;
      else
         return null;

      boolean singleQuote;
      if (className.endsWith("-single"))
         singleQuote = true;
      else if (className.endsWith("-double"))
         singleQuote = false;
      else
         return null;

      return new CodeMirrorStringMatcher(forward, singleQuote);
   }

}
