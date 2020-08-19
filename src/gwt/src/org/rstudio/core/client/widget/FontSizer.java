/*
 * FontSizer.java
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
import org.rstudio.studio.client.application.Desktop;


public class FontSizer
{
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

   public static void ensureStylesInjected()
   {
      styles.ensureInjected();
   }

   public static void injectStylesIntoDocument(Document doc)
   {
      StyleElement style = doc.createStyleElement();
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

   public static void setNormalFontSize(Document document, double size)
   {
      // ensure we have a document to attach styles to and that GWT created
      // our resource class
      if (document == null || styles == null)
         return;

      size = size + BrowseCap.getFontSkew();

      final String STYLE_EL_ID = "__rstudio_normal_size";

      Element oldStyle = document.getElementById(STYLE_EL_ID);

      StyleElement style = document.createStyleElement();
      if (style == null)
         return;
      style.setAttribute("type", "text/css");
      style.setInnerText("." + styles.normalSize() + ", " +
                         "." + styles.normalSize() + " td, " +
                         "." + styles.normalSize() + " pre" +
                         " {font-size:" + size + "pt !important;}");
      BodyElement body = document.getBody();
      if (body == null)
         return;
      body.appendChild(style);

      if (oldStyle != null)
         oldStyle.removeFromParent();

      style.setId(STYLE_EL_ID);
   }

   /**
    * Needs to match the size computed in FontSizer.css; used by xterm.js which is not
    * styled with css but via API calls
    *
    * @return css line_height scaling (no units)
    */
   public static double getNormalLineHeight()
   {
      double lineHeight = 1.25;

      if (BrowseCap.isMacintosh())
         lineHeight = 1.45;

      if (Desktop.isDesktop())
      {
         if (BrowseCap.isWindows())
            lineHeight = 1.2;
      }
      else if (BrowseCap.isFirefox())
      {
         if (BrowseCap.isWindows())
            lineHeight = 1.2;
      }
      else if (BrowseCap.isWindows())
         lineHeight = 1.1;

      if (Document.get().getBody().hasClassName("ubuntu_mono_firefox"))
         lineHeight = 1.1;
      else if (Document.get().getBody().hasClassName("ubuntu_mono"))
         lineHeight = 1.2;

      return lineHeight;
   }
}
