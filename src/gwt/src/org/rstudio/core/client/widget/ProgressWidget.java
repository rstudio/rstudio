/*
 * ProgressWidget.java
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

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Widget;

public class ProgressWidget extends Widget
{
   public interface Styles extends CssResource
   {
      String track();
      String bar();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ProgressWidget.css")
      Styles styles();
   }

   public ProgressWidget()
   {
      setElement(Document.get().createDivElement());

      int barWidthPct = getBarWidthPct();
      String animName = ensureKeyframes(barWidthPct);

      getElement().getStyle().setWidth(barWidthPct, Unit.PCT);
      getElement().getStyle().setHeight(getTrackHeightPx(), Unit.PX);
      getElement().addClassName(RES.styles().bar());
      getElement().getStyle().setProperty("borderRadius", "2px");
      getElement().getStyle().setProperty("animation",
         animName + " " + getAnimationDurationSecs() + "s ease-in-out infinite");
   }

   /**
    * Track width in pixels. Subclasses can override to customize.
    */
   public int getTrackWidthPx()
   {
      return 100;
   }

   /**
    * Track height in pixels. Subclasses can override to customize.
    */
   public int getTrackHeightPx()
   {
      return 16;
   }

   /**
    * Bar width as a percentage of the track. Subclasses can override
    * to customize.
    */
   protected int getBarWidthPct()
   {
      return 75;
   }

   /**
    * Animation duration in seconds. Subclasses can override to customize.
    */
   protected double getAnimationDurationSecs()
   {
      return 1.5;
   }

   public static String trackStyle()
   {
      return RES.styles().track();
   }

   /**
    * Injects a @keyframes rule for the given bar width, if one hasn't
    * been injected already. Returns the animation name.
    *
    * translateX percentages are relative to the bar's own width.
    * -100% places it fully off the left edge; (10000/W)% places it
    * fully off the right edge for a bar that is W% of the track.
    */
   private static String ensureKeyframes(int barWidthPct)
   {
      String name = "progress-slide-" + barWidthPct;

      if (!injectedWidths_.contains(barWidthPct))
      {
         int endTranslate = 10000 / barWidthPct;

         String css =
            "@keyframes " + name + " { " +
               "0% { transform: translateX(-100%); } " +
               "100% { transform: translateX(" + endTranslate + "%); } " +
            "}";

         StyleElement style = Document.get().createStyleElement();
         style.setInnerText(css);
         Document.get().getHead().appendChild(style);
         injectedWidths_.add(barWidthPct);
      }

      return name;
   }

   private static final Set<Integer> injectedWidths_ = new HashSet<>();

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
}
