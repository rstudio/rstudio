/*
 * FileIconRenderer.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import org.rstudio.core.client.StringUtil;

/**
 * Given an {@link FileIcon}, renders an element to show it. When the icon
 * carries a badge (see {@link FileIcon#withLinkBadge}), the base icon is
 * wrapped and the badge composited over its lower-left corner (#9924);
 * otherwise the output is a single &lt;img&gt; (or sprite), unchanged.
 */
public class FileIconRenderer extends AbstractSafeHtmlRenderer<FileIcon>
{
   interface Template extends SafeHtmlTemplates
   {
      @SafeHtmlTemplates.Template("<img src='{0}' draggable='false' border='0' width='{1}' height='{2}' alt='{3}'>")
      SafeHtml image(SafeUri imageUri, int width, int height, String altText);

      @SafeHtmlTemplates.Template("<span class='{0}' title='{1}'>{2}<img class='{3}' src='{4}' draggable='false' border='0' width='{5}' height='{6}' alt='{7}'></span>")
      SafeHtml badged(String wrapperClass, String tooltip, SafeHtml base,
                      String badgeClass, SafeUri badgeUri, int badgeWidth,
                      int badgeHeight, String badgeAlt);
   }

   interface Styles extends CssResource
   {
      String overlayWrapper();
      String overlayBadge();
   }

   interface Resources extends ClientBundle
   {
      @Source("FileIconRenderer.css")
      Styles styles();
   }

   private static final Template TEMPLATE = GWT.create(Template.class);
   private static final Resources RESOURCES = GWT.create(Resources.class);
   static { RESOURCES.styles().ensureInjected(); }

   @Override
   public SafeHtml render(FileIcon icon)
   {
      SafeHtml base = renderImage(icon);

      ImageResource badge = icon.getBadgeResource();
      if (badge == null)
         return base;

      return TEMPLATE.badged(
            RESOURCES.styles().overlayWrapper(),
            StringUtil.notNull(icon.getTooltip()),
            base,
            RESOURCES.styles().overlayBadge(),
            badge.getSafeUri(),
            badge.getWidth(),
            badge.getHeight(),
            StringUtil.notNull(icon.getBadgeDescription()));
   }

   private SafeHtml renderImage(FileIcon icon)
   {
      if (icon.getImageResource() instanceof ImageResourcePrototype.Bundle)
      {
         return AbstractImagePrototype.create(icon.getImageResource()).getSafeHtml();
      }
      else
      {
         ImageResource imageRez = icon.getImageResource();
         return TEMPLATE.image(
               imageRez.getSafeUri(),
               imageRez.getWidth(),
               imageRez.getHeight(),
               icon.getDescription());
      }
   }
}
