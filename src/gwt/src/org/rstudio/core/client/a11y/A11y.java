/*
 * A11y.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.core.client.a11y;

import com.google.gwt.dom.client.Element;

/**
 * Accessibility helpers including newer ARIA stuff not included with GWT
 */
public class A11y
{
   public static void setARIADialogModal(Element element)
   {
      element.setAttribute("aria-modal", "true");
   }

   /**
    * Flag an image that does not convey content, is decorative, or is
    * redundant (purpose already conveyed in text).
    * @param element Image element
    */
   public static void setDecorativeImage(Element element)
   {
      element.setAttribute("alt", "");
   }

   public static void setARIAMenuItemExpanded(Element element, boolean expanded)
   {
      element.setAttribute("aria-expanded", expanded ? "true" : "false");
   }

   public static void setARIATablistOrientation(Element element, boolean vertical)
   {
      element.setAttribute("aria-orientation", vertical ? "vertical" : "horizontal");
   }
}
