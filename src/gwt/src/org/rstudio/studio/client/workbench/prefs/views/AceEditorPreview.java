/*
 * AceEditorPreview.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceResources;

public class AceEditorPreview extends DynamicIFrame
{
   public AceEditorPreview(String code)
   {
      code_ = code;
      Style style = getStyleElement().getStyle();
      style.setBorderColor("#CCC");
      style.setBorderWidth(1, Unit.PX);
      style.setBorderStyle(BorderStyle.SOLID);
   }

   @Override
   protected void onFrameLoaded()
   {
      isFrameLoaded_ = true;
      if (themeUrl_ != null)
         setTheme(themeUrl_);
      if (fontSize_ != null)
         setFontSize(fontSize_);
      if (zoomLevel_ != null)
         setZoomLevel(zoomLevel_);

      final Document doc = getDocument();
      final BodyElement body = doc.getBody();
      body.getStyle().setMargin(0, Unit.PX);
      body.getStyle().setBackgroundColor("white");

      StyleElement style = doc.createStyleElement();
      style.setType("text/css");
      style.setInnerText(
            ".ace_editor {\n" +
            "border: none !important;\n" +
            "}");
      setFont(ThemeFonts.getFixedWidthFont());
      body.appendChild(style);

      DivElement div = doc.createDivElement();
      div.setId("editor");
      div.getStyle().setWidth(100, Unit.PCT);
      div.getStyle().setHeight(100, Unit.PCT);
      div.setInnerText(code_);
      body.appendChild(div);

      FontSizer.injectStylesIntoDocument(doc);
      FontSizer.applyNormalFontSize(div);

      new ExternalJavaScriptLoader(doc, AceResources.INSTANCE.acejs().getSafeUri().asString())
            .addCallback(new Callback()
      {
         public void onLoaded()
         {
            new ExternalJavaScriptLoader(doc, AceResources.INSTANCE.acesupportjs().getSafeUri().asString())
                  .addCallback(new Callback()
                  {
                     public void onLoaded()
                     {
                        body.appendChild(doc.createScriptElement(
                              "var editor = ace.edit('editor');\n" +
                              "editor.renderer.setHScrollBarAlwaysVisible(false);\n" +
                              "editor.renderer.setTheme({});\n" +
                              "editor.setHighlightActiveLine(false);\n" +
                              "editor.renderer.setShowGutter(false);\n" +
                              "editor.renderer.setDisplayIndentGuides(false);\n" +
                              "var RMode = require('mode/r').Mode;\n" +
                              "editor.getSession().setMode(new RMode(false, editor.getSession().getDocument()));"));
                     }
                  });
         }
      });
   }

   public void setTheme(String themeUrl)
   {
      themeUrl_ = themeUrl;
      if (!isFrameLoaded_)
         return;

      if (currentStyleLink_ != null)
         currentStyleLink_.removeFromParent();

      Document doc = getDocument();
      currentStyleLink_ = doc.createLinkElement();
      currentStyleLink_.setRel("stylesheet");
      currentStyleLink_.setType("text/css");
      currentStyleLink_.setHref(themeUrl);
      doc.getBody().appendChild(currentStyleLink_);
   }

   public void setFontSize(double fontSize)
   {
      fontSize_ = fontSize;
      if (!isFrameLoaded_)
         return;

      FontSizer.setNormalFontSize(getDocument(), fontSize);
   }

   public void setFont(String font)
   {
      final String STYLE_EL_ID = "__rstudio_font_family";
      Document document = getDocument();

      Element oldStyle = document.getElementById(STYLE_EL_ID);

      StyleElement style = document.createStyleElement();
      style.setAttribute("type", "text/css");
      style.setInnerText(".ace_editor, .ace_text-layer {\n" +
                         "font-family: " + font + " !important;\n" +
                         "}");

      document.getBody().appendChild(style);

      if (oldStyle != null)
         oldStyle.removeFromParent();

      style.setId(STYLE_EL_ID);
   }

   public void setZoomLevel(double zoomLevel)
   {
      zoomLevel_ = zoomLevel;
      if (!isFrameLoaded_)
         return;
   
      final String STYLE_EL_ID = "__rstudio_zoom_level";
      Document document = getDocument();

      Element oldStyle = document.getElementById(STYLE_EL_ID);

      StyleElement style = document.createStyleElement();
      style.setAttribute("type", "text/css");
      String zoom = Double.toString(zoomLevel);
      style.setInnerText(".ace_line {\n" +
                         "  -webkit-transform: scale(" + zoom + ");\n" +
                         "}");

      document.getBody().appendChild(style);

      if (oldStyle != null)
         oldStyle.removeFromParent();

      style.setId(STYLE_EL_ID);
   }
   
   public void reload()
   {
      getWindow().reload();
   }
   
 
 
   private LinkElement currentStyleLink_;
   private boolean isFrameLoaded_;
   private String themeUrl_;
   private Double fontSize_;
   private Double zoomLevel_;
   private final String code_;
}
