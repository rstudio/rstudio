/*
 * FindMatchHighlight.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.ColorUtil.RGBColor;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;

/**
 * Colours the editor's current Find match (the "ace_find_match" marker) from
 * the active theme. The highlight is the theme's own selection colour, moved
 * in lightness just far enough from the editor background to be seen: several
 * bundled dark themes draw selections at under 2:1 against their background,
 * which is too faint to pick a short match out by (rstudio#13204). Themes
 * whose selection already stands out are left exactly as they are.
 */
public class FindMatchHighlight
{
   /**
    * Recompute the highlight colour for the theme currently applied to the
    * document and publish it as a CSS variable on its root element. When the
    * theme cannot be sampled, the variable is removed so that the stylesheet
    * falls back to lightening or darkening the selection beneath the marker.
    */
   public static void update(Document document)
   {
      String value = null;
      try
      {
         value = computeBackground(document);
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }

      Element root = document.getDocumentElement();
      if (value != null)
         setCssVariable(root, CSS_VARIABLE, value);
      else
         removeCssVariable(root, CSS_VARIABLE);
   }

   private static String computeBackground(Document document)
   {
      // sample the theme through a hidden element tree shaped like an editor,
      // so that the selection rule (".ace_marker-layer .ace_selection") applies
      Element editor = document.createDivElement();
      editor.setClassName("ace_editor ace_content");
      Style style = editor.getStyle();
      style.setVisibility(Visibility.HIDDEN);
      style.setPosition(Position.ABSOLUTE);
      style.setTop(-10000, Unit.PX);
      style.setLeft(-10000, Unit.PX);

      Element layer = document.createDivElement();
      layer.setClassName("ace_marker-layer");
      editor.appendChild(layer);

      Element selection = document.createDivElement();
      selection.setClassName("ace_selection");
      layer.appendChild(selection);

      String background;
      String foreground;
      String selected;
      document.getBody().appendChild(editor);
      try
      {
         Style computed = DomUtils.getComputedStyles(editor);
         background = computed.getBackgroundColor();
         foreground = computed.getColor();
         selected = DomUtils.getComputedStyles(selection).getBackgroundColor();
      }
      finally
      {
         editor.removeFromParent();
      }

      // a transparent background means the theme stylesheet is not applied yet
      RGBColor backgroundColor = RGBColor.fromCss(background);
      RGBColor selectionColor = RGBColor.fromCss(selected);
      if (backgroundColor.alpha() == 0 || selectionColor.alpha() == 0)
         return null;

      RGBColor highlight = ColorUtil.separateFromBackground(
            ColorUtil.over(selectionColor, backgroundColor),
            backgroundColor,
            RGBColor.fromCss(foreground),
            MIN_DELTA_E);

      return highlight.asRgb();
   }

   private static native void setCssVariable(Element element, String name, String value) /*-{
      element.style.setProperty(name, value);
   }-*/;

   private static native void removeCssVariable(Element element, String name) /*-{
      element.style.removeProperty(name);
   }-*/;

   private static final String CSS_VARIABLE = "--rstudio-find-match-background";

   // CIE76 distance the highlight keeps from the editor background. Greys get
   // all of it from lightness; a selection with its own hue needs less.
   private static final double MIN_DELTA_E = 35;
}
