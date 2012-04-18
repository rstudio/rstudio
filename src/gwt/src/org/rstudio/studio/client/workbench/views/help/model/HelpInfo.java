/*
 * HelpInfo.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help.model ;

import com.google.gwt.core.client.JavaScriptObject;
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
      HashMap<String, String> values = new HashMap<String, String>() ;
      HashMap<String, String> args = null ;

      String html = getHTML() ;
      if (html != null)
      {
         DivElement div = Document.get().createDivElement() ;
         div.setInnerHTML(html) ;
         
         // disable all links
         NodeList<Element> anchors = div.getElementsByTagName("a") ;
         for (int i = 0; i < anchors.getLength(); i++)
         {
            Element anchor = anchors.getItem(i) ;
            Element parent = anchor.getParentElement() ;
            Node child = anchor.getFirstChild() ;
            while (child != null)
            {
               parent.insertBefore(child, anchor) ;
               child = child.getNextSibling() ;
            }
         }
   
         // get all h2 and h3 headings
         NodeList<Element> h2headings = div.getElementsByTagName("h2") ;
         NodeList<Element> h3headings = div.getElementsByTagName("h3") ;
         ArrayList<Element> headings = new ArrayList<Element>();
         for (int i = 0; i<h2headings.getLength(); i++)
            headings.add(h2headings.getItem(i));
         for (int i = 0; i<h3headings.getLength(); i++)
            headings.add(h3headings.getItem(i));
         
         // iterate through them
         for (int i = 0; i < headings.size(); i++)
         {
            Element heading = headings.get(i) ;
            String name = heading.getInnerText() ;
            if (name.equals("Arguments"))
            {
               args = parseArguments(heading) ;
            }
            StringBuffer value = new StringBuffer() ;
            Node sibling = heading.getNextSibling() ;
            while (sibling != null
                  && !sibling.getNodeName().toLowerCase().equals("h2")
                  && !sibling.getNodeName().toLowerCase().equals("h3"))
            {
               value.append(DomUtils.getHtml(sibling)) ;
               sibling = sibling.getNextSibling() ;
            }
            values.put(name, value.toString()) ;
         }
      }

      String signature = getSignature();
      if (signature == null)
         signature = defaultSignature;
      return new ParsedInfo(getPackageName(), signature, values, args) ;
   }

   private HashMap<String, String> parseArguments(Element heading)
   {
      Element table = (Element) DomUtils.findNode(heading, true, true, 
                                                  new NodePredicate() {
         public boolean test(Node n)
         {
            if (n.getNodeType() != Node.ELEMENT_NODE)
               return false ;
            
            Element el = (Element) n ;
            
            return el.getTagName().toUpperCase().equals("TABLE")
               && "R argblock".equals(el.getAttribute("summary")) ;
         }
      });
      
      if (table == null)
      {
         assert false : "Unexpected help format, no argblock table found" ; 
         return null ;
      }

      HashMap<String, String> result = new HashMap<String, String>() ;
      
      TableElement t = (TableElement) table ;
      NodeList<TableRowElement> rows = t.getRows() ;
      for (int i = 0; i < rows.getLength(); i++)
      {
         TableRowElement row = rows.getItem(i) ;
         NodeList<TableCellElement> cells = row.getCells() ;
         TableCellElement argNameCell = cells.getItem(0) ;
         TableCellElement argValueCell = cells.getItem(1) ;
         
         String argNameText = argNameCell.getInnerText() ;
         String argValueHtml = argValueCell.getInnerHTML() ;
         
         result.put(argNameText, argValueHtml) ;
      }
      
      return result ;
   }

   private final native String getHTML() /*-{
      return this.html ? this.html[0] : null ;
   }-*/;

   private final native String getSignature() /*-{
      return this.signature ? this.signature[0] : null ;
   }-*/;
   
   private final native String getPackageName() /*-{
      return this.pkgname ? this.pkgname[0] : null ;
   }-*/;

   public static class ParsedInfo
   {
      private String pkgName ;
      private String signature ;
      private HashMap<String, String> values ;
      private HashMap<String, String> args ;

      public ParsedInfo(String pkgName, String signature, HashMap<String, String> values,
            HashMap<String, String> args)
      {
         super() ;
         this.pkgName = pkgName ;
         this.signature = signature ;
         this.values = values != null ? values : new HashMap<String, String>();
         this.args = args ;
      }
      
      public String getPackageName()
      {
         return pkgName ;
      }
      
      public String getFunctionSignature()
      {
         return signature ;
      }

      public String getDescription()
      {
         return values.get("Description") ;
      }

      public String getUsage()
      {
         return values.get("Usage") ;
      }

      public String getDetails()
      {
         return values.get("Details") ;
      }

      /**
       * Returns null if no args section was present in the docs.
       */
      public HashMap<String, String> getArgs()
      {
         return args ;
      }

      public boolean hasInfo()
      {
         return signature != null
                || args != null
                || getDescription() != null;
      }
   }
}
