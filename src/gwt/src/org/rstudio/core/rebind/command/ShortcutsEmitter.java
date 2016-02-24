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

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class ShortcutsEmitter
{
   public ShortcutsEmitter(TreeLogger logger,
                           String groupName, 
                           Element shortcutsEl) throws UnableToCompleteException
   {
      logger_ = logger;
      shortcutsEl_ = shortcutsEl;
      groupName_ = groupName;
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
         String command = childEl.getAttribute("refid");
         String shortcutValue = childEl.getAttribute("value");
         String title = childEl.getAttribute("title");
         String disableModes = childEl.getAttribute("disableModes");

         // Use null when we don't have a command associated with the shortcut,
         // otherwise refer to the function that returns the command 
         command += command.isEmpty() ? "null" : "()";

         if (shortcutValue.length() == 0)
         {
            logger_.log(Type.ERROR, "Required attribute shortcut was missing\n" + elementToString(childEl));
            throw new UnableToCompleteException();
         }

         List<String> shortcuts = preprocessShortcutValue(shortcutValue);
         for (String shortcut : shortcuts)
         {
            printShortcut(writer, condition, shortcut, 
                  command, groupName_, title, disableModes);
         }
      }
   }
   
   private static List<String> preprocessShortcutValue(String shortcutValue)
   {
      List<String> shortcuts = new ArrayList<String>();
      
      for (String keySequence : shortcutValue.split("\\|"))
      {
         if (keySequence.indexOf("Cmd") != -1)
         {
            shortcuts.add(keySequence.replaceAll("Cmd", "Ctrl"));
            shortcuts.add(keySequence.replaceAll("Cmd", "Meta"));
         }
         else
         {
            shortcuts.add(keySequence);
         }
      }
      
      return shortcuts;
   }

   private void printShortcut(SourceWriter writer,
                              String condition,
                              String shortcut,
                              String command,
                              String shortcutGroup,
                              String title,
                              String disableModes) throws UnableToCompleteException
   {
      List<Pair<Integer, String>> keys = new ArrayList<Pair<Integer, String>>();
      for (String keyCombination : shortcut.split("\\s+"))
      {
         String[] chunks = keyCombination.split("\\+");
         
         // Build the shortcut modifiers integer and validate
         // as we build.
         int modifiers = KeyboardShortcut.NONE;
         for (int i = 0; i < chunks.length - 1; i++)
         {
            String m = chunks[i];
            if (m.equals("Ctrl"))
               modifiers += KeyboardShortcut.CTRL;
            else if (m.equals("Alt"))
               modifiers += KeyboardShortcut.ALT;
            else if (m.equals("Shift"))
               modifiers += KeyboardShortcut.SHIFT;
            else if (m.equals("Meta"))
               modifiers += KeyboardShortcut.META;
            else
            {
               logger_.log(
                     Type.ERROR,
                     "Invalid modifier '" + m + "'; expected one of " +
                     "'Ctrl', 'Alt', 'Shift', 'Meta'");
               
               throw new UnableToCompleteException();
            }
         }
            
         // Validate the key name.
         String key = toKey(chunks[chunks.length - 1]);
         if (key == null)
         {
            logger_.log(Type.ERROR,
                  "Invalid shortcut '" + shortcut + "', only " +
                  "modified alphanumeric characters, enter, " +
                  "left, right, up, down, pageup, pagedown, " +
                  "and tab are valid");
            throw new UnableToCompleteException();
         }
         
         // Push the parsed keys to the list.
         keys.add(new Pair<Integer, String>(modifiers, key));
      }
      
      // Emit the relevant code registering these shortcuts.
      if (!condition.isEmpty())
      {
         writer.println("if (" + condition + ") {");
         writer.indent();
      }
      
      if (keys.size() == 1)
      {
         int modifiers = keys.get(0).first;
         String key = keys.get(0).second;
         writer.println("ShortcutManager.INSTANCE.register(" +
               modifiers + ", " +
               key + ", " +
               command + ", " +
               "\"" + shortcutGroup + "\", " +
               "\"" + title + "\", " + 
               "\"" + disableModes + "\");");
      }
      else if (keys.size() == 2)
      {
         int m1    = keys.get(0).first;
         String k1 = keys.get(0).second;
         int m2    = keys.get(1).first;
         String k2 = keys.get(1).second;
         
         writer.println("ShortcutManager.INSTANCE.register(" +
               m1 + ", " +
               k1 + ", " +
               m2 + ", " +
               k2 + ", " +
               command + ", " +
               "\"" + shortcutGroup + "\", " +
               "\"" + title + "\", " + 
               "\"" + disableModes + "\");");
         
      }
      else
      {
         logger_.log(
               Type.ERROR,
               "Invalid key sequence: sequences must be of length 1 or 2");
         throw new UnableToCompleteException();
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
      if (val.equals(","))
         return "188";
      if (val.equals("-"))
         return "189";
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
   private final String groupName_;
}