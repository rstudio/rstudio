/*
 * Spinner.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
import com.google.gwt.dom.client.Document;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Widget;

public class Spinner extends Widget
{
   public interface Styles extends CssResource
   {
      String spinner();
   }

   public interface Resources extends ClientBundle
   {
      @Source("Spinner.css")
      Styles styles();
   }

   public Spinner()
   {
      setElement(Document.get().createDivElement());
      getElement().setAttribute("aria-hidden", "true");
      addStyleName(RES.styles().spinner());
   }

   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      ensureStylesInjected();
   }
}
