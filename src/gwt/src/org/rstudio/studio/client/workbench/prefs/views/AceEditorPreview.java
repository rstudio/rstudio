/*
 * AceEditorPreview.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
      final Document doc = getDocument();
      
      // NOTE: There is an interesting 'feature' in Firefox whereby an
      // initialized IFrame will report that it has successfully initialized the
      // window / underlying document (readyState is 'complete') but, in fact,
      // there is still some initialization left to occur and any changes made
      // before complete initialization will cause it to be swept out from under
      // our feet. To work around this, we double-check that the document we are
      // working with is the _same document_ after each JavaScript load
      // iteration.
      new ExternalJavaScriptLoader(getDocument(), AceResources.INSTANCE.acejs().getSafeUri().asString())
            .addCallback(new Callback()
      {
         public void onLoaded()
         {
            if (getDocument() != doc) 
            {
               onFrameLoaded();
               return;
            }
            
            new ExternalJavaScriptLoader(getDocument(), AceResources.INSTANCE.acesupportjs().getSafeUri().asString())
                  .addCallback(new Callback()
                  {
                     public void onLoaded()
                     {
                        
                        if (getDocument() != doc)
                        {
                           onFrameLoaded();
                           return;
                        }
                        
                        final Document doc = getDocument();
                        final BodyElement body = doc.getBody();
                        
                        if (themeUrl_ != null)
                           setTheme(themeUrl_);
                        if (fontSize_ != null)
                           setFontSize(fontSize_);
                        if (zoomLevel_ != null)
                           setZoomLevel(zoomLevel_);

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
                        
                        body.appendChild(doc.createScriptElement(
                              "var event = require('ace/lib/event');\n" +
                              "var Editor = require('ace/editor').Editor;\n" +
                              "var Renderer = require('ace/virtual_renderer').VirtualRenderer;\n" +
                              "var dom = require('ace/lib/dom');\n" +
                              "var container = document.getElementById('editor');\n" +
                              "var value = dom.getInnerText(container);\n" +
                              "container.innerHTML = '';\n" +
                              "var session = ace.createEditSession(value);\n" +
                              "var editor = new Editor(new Renderer(container, {}));\n" +
                              "editor.setSession(session);\n" +
                              "var env = {document: session, editor: editor, onResize: editor.resize.bind(editor, null)};\n" +
                              "event.addListener(window, 'resize', env.onResize);\n" +
                              "editor.on('destory', function() { event.removeListener(window, 'resize', env.onResize); });\n" +
                              "editor.container.env = editor.env = env;\n" +
                              "editor.renderer.setHScrollBarAlwaysVisible(false);\n" +
                              "editor.setHighlightActiveLine(false);\n" +
                              "editor.setReadOnly(true);\n" +
                              "editor.renderer.setShowGutter(false);\n" +
                              "editor.renderer.setDisplayIndentGuides(false);\n" +
                              "var RMode = require('mode/r').Mode;\n" +
                              "editor.getSession().setMode(new RMode(false, editor.getSession()));"));
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
