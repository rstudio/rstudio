/*
 * VirtualConsole.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client;

import java.util.ArrayList;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Simulates a console that behaves like the R console, specifically with
 * regard to \r (carriage return) and \b (backspace) characters.
 */
public class VirtualConsole
{
   public VirtualConsole()
   {
   }
   
   public void submit(String data)
   {
      submit(data, null);
   }

   public void submit(String data, String className)
   {
      if (StringUtil.isNullOrEmpty(data))
         return;

      if (CONTROL_SPECIAL.match(data, 0) == null)
      {
         text(data, className);
         return;
      }

      int tail = 0;
      Match match = CONTROL.match(data, 0);
      while (match != null)
      {
         int pos = match.getIndex();

         // If we passed over any plain text on the way to this control
         // character, add it.
         text(data.substring(tail, pos), className);

         tail = pos + 1;

         switch (data.charAt(pos))
         {
            case '\r':
               carriageReturn();
               break;
            case '\b':
               backspace();
               break;
            case '\n':
               newline();
               break;
            case '\f':
               formfeed();
               break;
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(pos) + "", className);
               break;
         }

         match = match.nextMatch();
      }

      // If there was any plain text after the last control character, add it
      text(data.substring(tail), className);
   }

   private void backspace()
   {
      if (pos == 0)
         return;
      o.deleteCharAt(--pos);
   }

   private void carriageReturn()
   {
      if (pos == 0)
         return;
      while (pos > 0 && o.charAt(pos - 1) != '\n')
         pos--;
      // Now we're either at the beginning of the buffer, or just past a '\n'
   }

   private void newline()
   {
      while (pos < o.length() && o.charAt(pos) != '\n')
         pos++;
      // Now we're either at the end of the buffer, or on top of a '\n'
      text("\n", null);
   }

   private void formfeed()
   {
      o.setLength(0);
      charClass.clear();
   }

   private void text(String text, String className)
   {
      assert text.indexOf('\r') < 0 && text.indexOf('\b') < 0;

      int endPos = pos + text.length();
      
      o.replace(pos, endPos, text);
      
      // record the class of each character emitted
      if (className != null) 
      {
         padCharClass(endPos);
         for (int i = pos; i < endPos; i++)
         {
            charClass.set(i, className);
         }
      }

      pos = endPos;
   }
   
   // ensures that the character class mapping buffer is at least 'len' 
   // characters long (note that ensureCapacity just reallocs the underlying
   // JavaScript array if necessary)
   private void padCharClass(int len)
   {
      int curSize = charClass.size();
      if (curSize >= len)
         return;
      charClass.ensureCapacity(len);
      for (int i = 0; i < (len - curSize); i++)
         charClass.add(null);
   }
   
   @Override
   public String toString()
   {
      return o.toString();
   }
   
   public SafeHtml toSafeHtml()
   {
      // convert to a plain-text string
      String plainText = toString();
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      String lastClass = null;
      int len = plainText.length();
      padCharClass(len);
      
      // iterate in lockstep over the plain-text string and character class
      // assignment list; emit the appropriate tags when switching classes
      for (int i = 0; i < len; i++)
      {
         if (!charClass.get(i).equals(lastClass))
         {
            if (lastClass != null) 
               sb.appendHtmlConstant("</span>");
            lastClass = charClass.get(i);
            if (lastClass != null)
               sb.appendHtmlConstant("<span class=\"" + lastClass + "\">");
         }
         sb.appendEscaped(plainText.substring(i, i+1));
      }
      if (lastClass != null)
         sb.appendHtmlConstant("</span>");
      
      return sb.toSafeHtml();
   }
   
   public void clear()
   {
      formfeed();
   }
   
   public static String consolify(String text)
   {
      VirtualConsole console = new VirtualConsole();
      console.submit(text);
      return console.toString();
   }

   private final StringBuilder o = new StringBuilder();
   private final ArrayList<String> charClass = new ArrayList<String>();
   private int pos = 0;
   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");
   private static final Pattern CONTROL_SPECIAL = Pattern.create("[\r\b\f]");
}
