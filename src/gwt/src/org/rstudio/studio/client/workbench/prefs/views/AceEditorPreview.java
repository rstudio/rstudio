/*
 * AceEditorPreview.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public class AceEditorPreview extends DynamicIFrame
{
   public AceEditorPreview(String code)
   {
      super(constants_.editorThemePreview());
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

                        doc.getHead().getParentElement().setLang("en"); // accessibility requirement

                        body.getStyle().setMargin(0, Unit.PX);
                        body.getStyle().setBackgroundColor("white");

                        StyleElement style = doc.createStyleElement();
                        style.setType("text/css");
                        style.setInnerText(
                              ".ace_editor {\n" +
                                    "border: none !important;\n" +
                                    "line-height: " + lineHeight_ + "%;\n" +
                              "}");
                        if (Desktop.isDesktop())
                           setFont(ThemeFonts.getFixedWidthFont(), false);
                        else if (webFont_ != null)
                           setFont(webFont_, true);
                        body.appendChild(style);

                        DivElement div = doc.createDivElement();
                        div.setId("editor");
                        div.getStyle().setWidth(100, Unit.PCT);
                        div.getStyle().setHeight(100, Unit.PCT);
                        div.setInnerText(code_);
                        body.appendChild(div);

                        FontSizer.injectStylesIntoDocument(doc);
                        FontSizer.applyNormalFontSize(div);
                        
                        body.appendChild(doc.createScriptElement(RES.loader().getText()));
                        
                        // Give Ace some time to load, then set line height.
                        if (lineHeight_ != null)
                        {
                           Scheduler.get().scheduleFixedDelay(() ->
                           {
                              if (div.hasClassName("ace_editor"))
                              {
                                 setLineHeight(lineHeight_);
                                 return false;
                              }
                              else
                              {
                                 return true;
                              }
                              
                           }, 100);
                        }
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

      if (zoomLevel_ == null)
         FontSizer.setNormalFontSize(getDocument(), fontSize_, lineHeight_);
      else
         FontSizer.setNormalFontSize(getDocument(), fontSize_ * zoomLevel_, lineHeight_);
   }
   
   public void setLineHeight(double lineHeight)
   {
      lineHeight_ = lineHeight;
      if (!isFrameLoaded_)
         return;
      
      Element editorEl = getDocument().getElementById("editor");
      if (editorEl == null)
         return;
      
      editorEl.getStyle().setLineHeight(lineHeight, Unit.PCT);
   }

   public void setFont(String font, boolean webFont)
   {
      final String STYLE_EL_ID = "__rstudio_font_family";
      final String LINK_EL_ID = "__rstudio_font_link";
      Document document = getDocument();

      Element oldStyle = document.getElementById(STYLE_EL_ID);
      Element oldLink = document.getElementById(LINK_EL_ID);
      
      if (webFont)
      {
         LinkElement link = document.createLinkElement();
         link.setRel("stylesheet");
         link.setHref("fonts/css/" + font + ".css");
         link.setId(LINK_EL_ID);
         document.getHead().appendChild(link);
         webFont_ = font;
      }

      String quotedFont = font.startsWith("\"") && font.endsWith("\"") ? font : "\"" + font + "\"";
      StyleElement style = document.createStyleElement();
      style.setAttribute("type", "text/css");
      style.setInnerText(".ace_editor, .ace_text-layer {\n" +
                         "font-family: " + quotedFont + ", monospace !important;\n" +
                         "}");

      document.getBody().appendChild(style);

      if (oldStyle != null)
         oldStyle.removeFromParent();
      if (oldLink != null)
         oldLink.removeFromParent();

      style.setId(STYLE_EL_ID);
   }

   public void setZoomLevel(double zoomLevel)
   {
      zoomLevel_ = zoomLevel;
      if (!isFrameLoaded_)
         return;
      
      if (fontSize_ != null)
         setFontSize(fontSize_);
   }
   
   private LinkElement currentStyleLink_;
   private boolean isFrameLoaded_;
   private String themeUrl_;
   private String webFont_;
   private Double fontSize_;
   private Double lineHeight_;
   private Double zoomLevel_;
   private final String code_;
   
   public interface Resources extends ClientBundle
   {
      @Source("AceEditorPreview.js")
      TextResource loader();
   }

   private static Resources RES = GWT.create(Resources.class);

   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
