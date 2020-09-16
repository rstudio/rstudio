/*
 * FileIconRenderer.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * Given an {@link FileIcon}, renders an element to show it.
 */
public class FileIconRenderer extends AbstractSafeHtmlRenderer<FileIcon>
{
   interface Template extends SafeHtmlTemplates
   {
      @SafeHtmlTemplates.Template("<img src='{0}' draggable='false' border='0' width='{1}' height='{2}' alt='{3}'>")
      SafeHtml image(SafeUri imageUri, int width, int height, String altText);
   }

   private static final Template TEMPLATE = GWT.create(Template.class);

   @Override
   public SafeHtml render(FileIcon image)
   {
      if (image.getImageResource() instanceof ImageResourcePrototype.Bundle)
      {
         return AbstractImagePrototype.create(image.getImageResource()).getSafeHtml();
      } 
      else
      {
         ImageResource imageRez = image.getImageResource();
         return TEMPLATE.image(
               imageRez.getSafeUri(), 
               imageRez.getWidth(), 
               imageRez.getHeight(),
               image.getDescription());
      }
   }
}
