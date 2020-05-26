/*
 * HelpInfo.java
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
package org.rstudio.studio.client.workbench.views.help.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.*;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;

import java.util.ArrayList;
import java.util.HashMap;

public class HelpInfo extends JavaScriptObject
{
   protected HelpInfo()
   {

   }

   public final ParsedInfo parse(String defaultSignature)
   {
      HashMap<String, String> values = new HashMap<String, String>();
      HashMap<String, String> args = new HashMap<String, String>();
      HashMap<String, String> slots = new HashMap<String, String>();

      String html = getHTML();
      if (html != null)
      {
         DivElement div = Document.get().createDivElement();
         div.setInnerHTML(html);
         
         // disable all links
         NodeList<Element> anchors = div.getElementsByTagName("a");
         for (int i = 0; i < anchors.getLength(); i++)
         {
            Element anchor = anchors.getItem(i);
            Element parent = anchor.getParentElement();
            Node child = anchor.getFirstChild();
            while (child != null)
            {
               parent.insertBefore(child, anchor);
               child = child.getNextSibling();
            }
         }
         
         // parse all description lists
         NodeList<Element> descriptionLists = div.getElementsByTagName("dl");
         for (int i = 0; i < descriptionLists.getLength(); i++)
            parseDescriptionList(args, descriptionLists.getItem(i));
         
         // get all h2 and h3 headings
         NodeList<Element> h2headings = div.getElementsByTagName("h2");
         NodeList<Element> h3headings = div.getElementsByTagName("h3");
         ArrayList<Element> headings = new ArrayList<Element>();
         for (int i = 0; i < h2headings.getLength(); i++)
            headings.add(h2headings.getItem(i));
         for (int i = 0; i < h3headings.getLength(); i++)
            headings.add(h3headings.getItem(i));
         
         // the first h2 heading is the title -- handle that specially
         if (headings.size() > 0)
         {
            Element titleElement = headings.get(0);
            String title = titleElement.getInnerText();
            values.put("Title", title);
         }
         
         // iterate through them
         for (int i = 1; i < headings.size(); i++)
         {
            Element heading = headings.get(i);
            String name = heading.getInnerText();
            if (name == "Arguments")
            {
               parseArguments(args, heading);
            }
            if (name == "Slots")
            {
               parseDescriptionList(slots, heading);
            }
            StringBuffer value = new StringBuffer();
            Node sibling = heading.getNextSibling();
            while (sibling != null
                  && !sibling.getNodeName().toLowerCase().equals("h2")
                  && !sibling.getNodeName().toLowerCase().equals("h3"))
            {
               value.append(DomUtils.getHtml(sibling));
               sibling = sibling.getNextSibling();
            }
            values.put(name, value.toString());
         }
      }

      String signature = getSignature();
      if (signature == null)
         signature = defaultSignature;
      return new ParsedInfo(getPackageName(), signature, values, args, slots);
   }
   
   private void parseDescriptionList(HashMap<String, String> args,
                                     Element heading)
   {
      Element table = (Element) DomUtils.findNode(heading, true, true, new NodePredicate() {
         
         public boolean test(Node n)
         {
            if (n.getNodeType() != Node.ELEMENT_NODE)
               return false;
            
            Element el = (Element) n;
            
            return el.getTagName().toUpperCase().equals("DL");
         }
      });
      
      if (table == null)
      {
         assert false : "Unexpected slots format: no <dl> entry found";
         return;
      }
      
      NodeList<Node> children = table.getChildNodes();
      int nChildren = children.getLength();
      for (int i = 0; i < nChildren; i++)
      {
         Element child = (Element) children.getItem(i);
         if (child.getNodeName().toUpperCase().equals("DT"))
         {
            String argName = child.getInnerText().replaceAll(":", "");
            Element nextChild = (Element) children.getItem(i + 1);
            if (nextChild.getNodeName().toUpperCase().equals("DD"))
            {
               String value = nextChild.getInnerHTML();
               args.put(argName, value);
            }
         }
      }
   }

   private void parseArguments(HashMap<String, String> args,
                               Element heading)
   {
      Element table = (Element) DomUtils.findNode(heading, true, true, 
                                                  new NodePredicate() {
         public boolean test(Node n)
         {
            if (n.getNodeType() != Node.ELEMENT_NODE)
               return false;
            
            Element el = (Element) n;
            
            return el.getTagName().toUpperCase().equals("TABLE")
               && "R argblock".equals(el.getAttribute("summary"));
         }
      });
      
      if (table == null)
      {
         assert false : "Unexpected help format, no argblock table found"; 
         return;
      }

      TableElement t = (TableElement) table;
      NodeList<TableRowElement> rows = t.getRows();
      for (int i = 0; i < rows.getLength(); i++)
      {
         TableRowElement row = rows.getItem(i);
         NodeList<TableCellElement> cells = row.getCells();
         
         TableCellElement argNameCell = cells.getItem(0);
         TableCellElement argValueCell = cells.getItem(1);
         
         String argNameText = argNameCell.getInnerText();
         String argValueHtml = argValueCell.getInnerHTML();
         
         // argNameCell may be multiple comma-delimited arguments;
         // split them up if necessary (duplicate the help across args)
         String[] argNameTextSplat = argNameText.split("\\s*,\\s*");
         for (int j = 0; j < argNameTextSplat.length; j++)
            args.put(argNameTextSplat[j], argValueHtml);
         
      }
   }

   private final native String getHTML() /*-{
      return this.html ? this.html[0] : null;
   }-*/;

   private final native String getSignature() /*-{
      return this.signature ? this.signature[0] : null;
   }-*/;
   
   private final native String getPackageName() /*-{
      return this.pkgname ? this.pkgname[0] : null;
   }-*/;
   
   public static class Custom extends JavaScriptObject
   {
      protected Custom()
      {  
      }
      
      public final native String getPackageName() /*-{
         return this.package_name ? this.package_name[0] : "";
      }-*/;

   
      public final native String getTitle() /*-{
         return this.title ? this.title[0] : "";
      }-*/;

      public final native String getSignature() /*-{
         return this.signature ? this.signature[0]: "";
      }-*/;
   
      public final native String getDescription() /*-{
         return this.description ? this.description[0] : "";
      }-*/;
      
      public final native JsArrayString getArgs() /*-{
         return this.args;
      }-*/;
      
      public final native JsArrayString getArgDescriptions() /*-{
         return this.arg_descriptions;
      }-*/;
      
      public final ParsedInfo toParsedInfo() 
      {
         // values
         HashMap<String,String> values = new HashMap<String,String>();
         values.put("Title", getTitle());
         values.put("Description", getDescription());
         
         // args
         HashMap<String, String> args = null;
         JsArrayString jsArgs = getArgs();
         JsArrayString jsArgDescriptions = getArgDescriptions();
         if (jsArgs != null && jsArgDescriptions != null)
         {
            args = new HashMap<String,String>();
            for (int i =0; i<jsArgs.length(); i++)
               args.put(jsArgs.get(i), jsArgDescriptions.get(i));
         }
          
         // return parsed info
         return new ParsedInfo(getPackageName(),
                               getSignature(),
                               values,
                               args,
                               null);
      }
      
   }
   

   public static class ParsedInfo
   {
      private String pkgName;
      private String signature;
      private HashMap<String, String> values;
      private HashMap<String, String> args;
      private HashMap<String, String> slots;

      public ParsedInfo(String pkgName, String signature, HashMap<String, String> values,
            HashMap<String, String> args, HashMap<String, String> slots)
      {
         super();
         this.pkgName = pkgName;
         this.signature = signature;
         this.values = values != null ? values : new HashMap<String, String>();
         this.args = args;
         this.slots = slots;
      }
      
      public String getPackageName()
      {
         return pkgName;
      }
      
      public String getTitle()
      {
         return values.get("Title");
      }
      
      public String getFunctionSignature()
      {
         return signature;
      }

      public String getDescription()
      {
         return values.get("Description");
      }

      public String getUsage()
      {
         return values.get("Usage");
      }

      public String getDetails()
      {
         return values.get("Details");
      }

      /**
       * Returns null if no args section was present in the docs.
       */
      public HashMap<String, String> getArgs()
      {
         return args;
      }
      
      public HashMap<String, String> getSlots()
      {
         return slots;
      }

      public boolean hasInfo()
      {
         return signature != null
                || args != null
                || getDescription() != null;
      }
   }
}
