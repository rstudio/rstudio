/*
 * ChunkHtmlTheme.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.dom.client.Element;

/** Foreground defaults for notebook HTML that has not opted out of theming. */
class ChunkHtmlTheme
{
   public static native void sync(Element body, String foreground, String background) /*-{
      var doc = body.ownerDocument;
      var win = doc.defaultView;
      var state = doc.__rstudioChunkTheme;
      if (!state)
      {
         var style = doc.createElement("style");
         style.id = "rstudio-chunk-table-theme";
         doc.head.insertBefore(style, doc.head.firstChild);

         var canvas = doc.createElement("canvas");
         canvas.width = canvas.height = 1;
         var context = canvas.getContext("2d");
         var colors = {};
         var attribute = "data-rstudio-chunk-surface";
         var nextId = 0;
         var pending = null;
         var observer;

         // Canvas also normalizes modern CSS colors, not just rgb()/rgba().
         function rgba(color)
         {
            if (!colors[color])
            {
               context.clearRect(0, 0, 1, 1);
               context.fillStyle = color;
               context.fillRect(0, 0, 1, 1);
               colors[color] = context.getImageData(0, 0, 1, 1).data;
            }
            return colors[color];
         }

         function surfaceColor(element)
         {
            var ancestors = [];
            for (var node = element; node; node = node.parentElement)
               ancestors.push(node);

            var base = rgba(state.background);
            var rgb = [base[0], base[1], base[2]];
            for (var i = ancestors.length - 1; i >= 0; --i)
            {
               var paint = rgba(win.getComputedStyle(ancestors[i]).backgroundColor);
               var alpha = paint[3] / 255;
               for (var channel = 0; channel < 3; ++channel)
                  rgb[channel] = paint[channel] * alpha + rgb[channel] * (1 - alpha);
            }

            // Choose the neutral foreground with the better contrast against
            // the composited surface. Authored foregrounds never enter here.
            for (var j = 0; j < 3; ++j)
            {
               var value = rgb[j] / 255;
               rgb[j] = value <= 0.04045 ? value / 12.92 :
                  Math.pow((value + 0.055) / 1.055, 2.4);
            }
            var luminance = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
            return (luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05) ?
               "black" : "white";
         }

         function rules(color)
         {
            // Quirks-mode tables can retain a stale body color in Chromium.
            // Normal inheritance fixes theme changes and later-added tables.
            // Zero specificity and source order preserve authored declarations.
            return ":where(table) { color: inherit; }\n" +
               ":where(html) { color: " + color + "; }\n";
         }

         function sheet(color, surfaces)
         {
            var css = rules(color) + surfaces;
            // Unlayered defaults would override even specific authored rules
            // inside cascade layers. Our first layer has the lowest priority.
            return win.CSSLayerBlockRule ? "@layer rstudio-chunk-theme {\n" + css + "}" : css;
         }

         function update()
         {
            if (pending !== null)
            {
               win.cancelAnimationFrame(pending);
               pending = null;
            }

            // Our style/marker changes must not schedule another update.
            observer.disconnect();
            try
            {
               if (style.parentNode !== doc.head)
                  doc.head.insertBefore(style, doc.head.firstChild);
               var body = doc.body;
               if (!body || body.className || body.style.backgroundColor)
               {
                  style.textContent = "";
                  return;
               }

               // Compare two inherited defaults to distinguish an authored
               // foreground (including one on an ancestor) from our default.
               // Both probes and the final rules run synchronously, so the
               // intermediate colors are never painted.
               style.textContent = sheet("black", "");
               var elements = doc.querySelectorAll("*");
               var surfaces = [];
               for (var i = 0; i < elements.length; ++i)
               {
                  var element = elements[i];
                  var computed = win.getComputedStyle(element);
                  if (rgba(computed.backgroundColor)[3] > 0)
                  {
                     surfaces.push({
                        element: element,
                        color: computed.color,
                        background: computed.backgroundColor
                     });
                  }
               }

               style.textContent = sheet("white", "");
               var extra = "";
               for (var j = 0; j < surfaces.length; ++j)
               {
                  var surface = surfaces[j];
                  var computed = win.getComputedStyle(surface.element);
                  if (computed.color === surface.color ||
                      computed.backgroundColor !== surface.background)
                     continue;

                  var id = surface.element.getAttribute(attribute);
                  if (!id)
                  {
                     id = String(++nextId);
                     surface.element.setAttribute(attribute, id);
                  }
                  extra += ":where([" + attribute + "=\"" + id + "\"]) { color: " +
                     surfaceColor(surface.element) + "; }\n";
               }
               style.textContent = sheet(state.foreground, extra);
            }
            finally
            {
               observer.observe(doc.documentElement, {
                  subtree: true,
                  childList: true,
                  characterData: true,
                  attributes: true,
                  attributeFilter: ["class", "style", "bgcolor", "href", "rel", "media"]
               });
            }
         }

         function schedule()
         {
            if (pending === null)
               pending = win.requestAnimationFrame(update);
         }

         observer = new win.MutationObserver(schedule);
         doc.addEventListener("load", function(event) {
            if (event.target.tagName === "LINK")
               schedule();
         }, true);
         state = doc.__rstudioChunkTheme = { update: update };
      }

      state.foreground = foreground;
      state.background = background;
      state.update();
   }-*/;
}
