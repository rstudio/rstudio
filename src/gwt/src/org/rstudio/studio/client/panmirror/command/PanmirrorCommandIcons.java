
/*
 * PanmirrorCommandIcons.java
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



package org.rstudio.studio.client.panmirror.command;

import java.util.HashMap;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import com.google.gwt.resources.client.ImageResource;

public class PanmirrorCommandIcons
{
   public final String BLOCKQUOTE = "blockquote";
   public final String BOLD = "bold";
   public final String BULLET_LIST = "bullet_list";
   public final String CITATION = "citation";
   public final String CODE = "code";
   public final String IMAGE = "image";
   public final String ITALIC = "italic";
   public final String OMNI = "omni";
   public final String LINK = "link";
   public final String NUMBERED_LIST = "numbered_list";
   public final String TABLE = "table";
   public final String CLEAR_FORMATTING = "clear_formatting";
   public final String COMMENT = "comment";
   
   private PanmirrorCommandIcons()
   {
      PanmirrorToolbarResources res = PanmirrorToolbarResources.INSTANCE;
      icons_.put(BLOCKQUOTE, res.blockquote());
      icons_.put(BOLD, res.bold());
      icons_.put(dm(BOLD), res.bold_dm());
      icons_.put(BULLET_LIST, res.bullet_list());
      icons_.put(dm(BULLET_LIST), res.bullet_list_dm());
      icons_.put(CITATION, res.citation());
      icons_.put(dm(CITATION), res.citation_dm());
      icons_.put(CODE, res.code());
      icons_.put(dm(CODE), res.code_dm());
      icons_.put(IMAGE, res.image());
      icons_.put(ITALIC, res.italic());
      icons_.put(dm(ITALIC), res.italic_dm());
      icons_.put(OMNI, res.omni());
      icons_.put(LINK, res.link());
      icons_.put(NUMBERED_LIST, res.numbered_list());
      icons_.put(dm(NUMBERED_LIST), res.numbered_list_dm());
      icons_.put(TABLE, res.table());
      icons_.put(CLEAR_FORMATTING, res.clear_formatting());
      icons_.put(dm(CLEAR_FORMATTING), res.clear_formatting_dm());
      icons_.put(COMMENT, res.comment());
   }
   
   public ImageResource get(String name)
   {
      ImageResource icon = null;
      if (RStudioThemes.isEditorDark())
         icon = icons_.get(dm(name));
      if (icon == null)
         icon = icons_.get(name);
      if (icon != null)
         icon = new ImageResource2x(icon);
      return icon;
   }
   
   private String dm(String icon)
   {
      return icon + "_dm";
   }
   
   public static PanmirrorCommandIcons INSTANCE = new PanmirrorCommandIcons();
   
   private HashMap<String,ImageResource> icons_ = new HashMap<String,ImageResource>();
   
}
