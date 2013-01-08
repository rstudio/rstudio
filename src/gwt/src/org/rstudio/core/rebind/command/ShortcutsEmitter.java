/*
 * ShortcutsEmitter.java
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
package org.rstudio.core.rebind.command;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.user.rebind.SourceWriter;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.StringWriter;

public class ShortcutsEmitter
{
   public ShortcutsEmitter(TreeLogger logger,
                           Element shortcutsEl) throws UnableToCompleteException
   {
      logger_ = logger;
      shortcutsEl_ = shortcutsEl;
   }

   public void generate(SourceWriter writer) throws UnableToCompleteException
   {
      NodeList children = shortcutsEl_.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node childNode = children.item(i);
         if (childNode.getNodeType() != Node.ELEMENT_NODE)
            continue;
         Element childEl = (Element)childNode;
         if (!childEl.getTagName().equals("shortcut"))
         {
            logger_.log(Type.ERROR, "Unexpected element: " + elementToString(childEl));
            throw new UnableToCompleteException();
         }

         String condition = childEl.getAttribute("if");
         String commandId = childEl.getAttribute("refid");
         String shortcutValue = childEl.getAttribute("value");

         if (commandId.length() == 0)
         {
            logger_.log(Type.ERROR, "Required attribute refid was missing\n" + elementToString(childEl));
            throw new UnableToCompleteException();
         }
         if (shortcutValue.length() == 0)
         {
            logger_.log(Type.ERROR, "Required attribute shortcut was missing\n" + elementToString(childEl));
            throw new UnableToCompleteException();
         }

         printShortcut(writer, condition, shortcutValue, commandId);
      }
   }

   private void printShortcut(SourceWriter writer,
                              String condition,
                              String shortcutValue,
                              String commandId) throws UnableToCompleteException
   {
      String[] chunks = shortcutValue.split("\\+");
      int modifiers = KeyboardShortcut.NONE;
      boolean cmd = false;
      for (int i = 0; i < chunks.length - 1; i++)
      {
         String m = chunks[i];
         if (m.equals("Ctrl"))
            modifiers += KeyboardShortcut.CTRL;
         else if (m.equals("Meta"))
            modifiers += KeyboardShortcut.META;
         else if (m.equals("Alt"))
            modifiers += KeyboardShortcut.ALT;
         else if (m.equals("Shift"))
            modifiers += KeyboardShortcut.SHIFT;
         else if (m.equals("Cmd"))
            cmd = true;
         else
         {
            logger_.log(Type.ERROR, "Invalid shortcut " + shortcutValue);
            throw new UnableToCompleteException();
         }
      }

      String key = toKey(chunks[chunks.length - 1]);
      if (key == null)
      {
         logger_.log(Type.ERROR, "Invalid shortcut " + shortcutValue + ", only " +
                                 "modified alphanumeric characters, enter, " +
                                 "left, right, up, down, pageup, pagedown, " +
                                 "and tab are valid");
         throw new UnableToCompleteException();
      }

      if (!condition.isEmpty())
      {
         writer.println("if (" + condition + ") {");
         writer.indent();
      }

      if (cmd)
      {
         writer.println("ShortcutManager.INSTANCE.register(" +
                        (modifiers| KeyboardShortcut.CTRL) + ", " +
                        key + ", " +
                        commandId + "());");
         writer.println("ShortcutManager.INSTANCE.register(" +
                        (modifiers| KeyboardShortcut.META) + ", " +
                        key + ", " +
                        commandId + "());");
      }
      else
      {
         writer.println("ShortcutManager.INSTANCE.register(" +
                        modifiers + ", " +
                        key + ", " +
                        commandId + "());");
      }

      if (!condition.isEmpty())
      {
         writer.outdent();
         writer.println("}");
      }
   }

   private String toKey(String val)
   {
      if (val.matches("^[a-zA-Z0-9]$"))
         return "'" + val.toUpperCase() + "'";
      if (val.equals("/"))
         return "191";
      if (val.equalsIgnoreCase("enter"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_ENTER";
      if (val.equalsIgnoreCase("right"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_RIGHT";
      if (val.equalsIgnoreCase("left"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_LEFT";
      if (val.equalsIgnoreCase("up"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_UP";
      if (val.equalsIgnoreCase("down"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_DOWN";
      if (val.equalsIgnoreCase("tab"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_TAB";
      if (val.equalsIgnoreCase("pageup"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_PAGEUP";
      if (val.equalsIgnoreCase("pagedown"))
         return "com.google.gwt.event.dom.client.KeyCodes.KEY_PAGEDOWN";
      if (val.equalsIgnoreCase("F1"))
         return "112";
      if (val.equalsIgnoreCase("F2"))
         return "113";
      if (val.equalsIgnoreCase("F3"))
         return "114";
      if (val.equalsIgnoreCase("F4"))
         return "115";
      if (val.equalsIgnoreCase("F5"))
         return "116";
      if (val.equalsIgnoreCase("F6"))
         return "117";
      if (val.equalsIgnoreCase("F7"))
         return "118";
      if (val.equalsIgnoreCase("F8"))
         return "119";
      if (val.equalsIgnoreCase("F9"))
         return "120";
      if (val.equalsIgnoreCase("F10"))
         return "121";
      if (val.equalsIgnoreCase("F11"))
         return "122";
      if (val.equalsIgnoreCase("F12"))
         return "123";
      if (val.equals("`"))
         return "192";
      if (val.equals("."))
         return "190";
      if (val.equals("="))
         return "187";
      if (val.equals("<"))
         return "188";
      if (val.equals("Backspace"))
         return "8";
      return null;
   }

   private String elementToString(Element el) throws UnableToCompleteException
   {
      try
      {
         javax.xml.transform.TransformerFactory tfactory = TransformerFactory.newInstance();
         javax.xml.transform.Transformer xform = tfactory.newTransformer();
         javax.xml.transform.Source src = new DOMSource(el);
         java.io.StringWriter writer = new StringWriter();
         Result result = new javax.xml.transform.stream.StreamResult(writer);
         xform.transform(src, result);
         return writer.toString();
      }
      catch (Exception e)
      {
         logger_.log(Type.ERROR, "Error attempting to stringify some XML", e);
         throw new UnableToCompleteException();
      }
   }

   private final TreeLogger logger_;
   private final Element shortcutsEl_;
}