/*
 * FontSizer.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.UIObject;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;


public class FontSizer
{
   public static void ensureStylesInjected()
   {
      Element style = Document.get().getElementById(RSTUDIO_NORMAL_SIZE_ID);
      if (style == null)
         injectStylesIntoDocument(Document.get());
   }

   public static void injectStylesIntoDocument(Document doc)
   {
      StyleElement style = doc.createStyleElement();
      style.setId(RSTUDIO_NORMAL_SIZE_ID);
      style.setType("text/css");
      style.setInnerText(styles.getText());
      doc.getBody().appendChild(style);
   }

   public static void applyNormalFontSize(UIObject object)
   {
      object.addStyleName(styles.normalSize());
   }

   public static void applyNormalFontSize(Element element)
   {
      element.addClassName(styles.normalSize());
   }

   public static String getNormalFontSizeClass()
   {
      return styles.normalSize();
   }
   
   // NOTE: Returns requested line height as a percentage value.
   private static final double normalizeHeight(Double height)
   {
      if (height == null || height == 0.0)
      {
         return getNormalLineHeight() * 100.0;
      }
      else
      {
         return height;
      }
   }
   
   public static void setNormalFontSize(Document document,
                                        ChangeFontSizeEvent event)
   {
      setNormalFontSize(
            document,
            event.getFontSize(),
            event.getLineHeight());
   }
   
   public static void setNormalFontSize(Document document,
                                        FontSizeManager fsm)
   {
      setNormalFontSize(
            document,
            fsm.getFontSize(),
            fsm.getLineHeight());
   }

   public static void setNormalFontSize(Document document,
                                        Double size,
                                        Double height)
   {
      // ensure we have a document to attach styles to and that GWT created
      // our resource class
      if (document == null || styles == null)
         return;

      BodyElement body = document.getBody();
      if (body == null)
         return;
      
      Element oldStyle = document.getElementById(RSTUDIO_NORMAL_SIZE_ID);
      if (oldStyle != null)
         oldStyle.removeFromParent();

      size = size + BrowseCap.getFontSkew();
      height = normalizeHeight(height);
      
      StyleElement style = document.createStyleElement();
      style.setId(RSTUDIO_NORMAL_SIZE_ID);
      style.setAttribute("type", "text/css");
      style.setInnerText("." + styles.normalSize() + ", " +
                         "." + styles.normalSize() + " td, " +
                         "." + styles.normalSize() + " pre " +
                         "{\n" +
                         "  font-size: " + size + "pt !important;\n" +
                         "  line-height: " + height + "%;\n" +
                         "}");
      body.appendChild(style);
   }
   
   public static void updateLineHeight(Document document, double lineHeight)
   {
      if (document == null)
         return;

      Element style = document.getElementById(RSTUDIO_NORMAL_SIZE_ID);
      if (style == null)
         return;
      
      String text = style.getInnerText();
      Pattern pattern = Pattern.create("line-height\\s+:\\s+([^;]+)");
      text = pattern.replaceAll(text, "line-height: " + lineHeight + "%");
      style.removeFromParent();
      
      StyleElement newStyle = document.createStyleElement();
      newStyle.setId(RSTUDIO_NORMAL_SIZE_ID);
      newStyle.setType("text/css");
      newStyle.setInnerText(text);
      document.getBody().appendChild(newStyle);
   }
   

   /**
    * Needs to match the size computed in FontSizer.css; used by xterm.js which is not
    * styled with css but via API calls
    *
    * @return css line_height scaling (no units)
    */
   public static double getNormalLineHeight()
   {
      if (BrowseCap.isMacintosh())
      {
         return 1.45;
      }
      else if (BrowseCap.isWindows())
      {
         return 1.2;
      }
      else
      {
         return 1.25;
      }
   }
   
   private static final String RSTUDIO_NORMAL_SIZE_ID = "__rstudio_normal_size";
   
   // Boilerplate ----
   
   static interface Resources extends ClientBundle
   {
      @Source("FontSizer.css")
      Styles styles();
   }

   static interface Styles extends CssResource
   {
      String normalSize();
   }

   private static Styles styles = GWT.<Resources>create(Resources.class).styles();
   
}
