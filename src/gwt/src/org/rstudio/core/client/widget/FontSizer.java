/*
 * FontSizer.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.UIObject;

import java.util.ArrayList;

public class FontSizer
{
   public enum Size
   {
      Pt10,
      Pt12,
      Pt14,
      Pt16,
      Pt18
   }

   static interface Resources extends ClientBundle
   {
      @Source("FontSizer.css")
      Styles styles();
   }

   static interface Styles extends CssResource
   {
      String normalSize();
      String pt10();
      String pt12();
      String pt14();
      String pt16();
      String pt18();
   }

   private static Styles styles = GWT.<Resources>create(Resources.class).styles();

   public static void ensureStylesInjected()
   {
      styles.ensureInjected();
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

   public static void setNormalFontSize(Document document, Size size)
   {
      ArrayList<String> stylesToRemove = new ArrayList<String>();
      stylesToRemove.add(styles.pt10());
      stylesToRemove.add(styles.pt12());
      stylesToRemove.add(styles.pt14());
      stylesToRemove.add(styles.pt16());
      stylesToRemove.add(styles.pt18());

      String styleToAdd;
      switch (size)
      {
         case Pt10:
            styleToAdd = styles.pt10();
            break;
         case Pt12:
            styleToAdd = styles.pt12();
            break;
         case Pt14:
            styleToAdd = styles.pt14();
            break;
         case Pt16:
            styleToAdd = styles.pt16();
            break;
         case Pt18:
            styleToAdd = styles.pt18();
            break;
         default:
            return;
      }

      BodyElement body = document.getBody();
      body.addClassName(styleToAdd);
      stylesToRemove.remove(styleToAdd);
      for (String styleToRemove : stylesToRemove)
         body.removeClassName(styleToRemove);
   }
}
