package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceResources;

public class AceEditorPreview extends DynamicIFrame
{
   public AceEditorPreview()
   {
      Style style = getStyleElement().getStyle();
      style.setBorderColor("#CCC");
      style.setBorderWidth(1, Unit.PX);
      style.setBorderStyle(BorderStyle.SOLID);
      style.setWidth(85, Unit.PCT);

   }

   @Override
   protected void onFrameLoaded()
   {
      isFrameLoaded_ = true;
      if (initialThemeUrl_ != null)
         setTheme(initialThemeUrl_);
      if (hasInitialFontSize_)
         setFontSize(initialFontSize_);

      final Document doc = getDocument();
      final BodyElement body = doc.getBody();
      body.getStyle().setMargin(0, Unit.PX);
      body.getStyle().setBackgroundColor("white");

      StyleElement style = doc.createStyleElement();
      style.setType("text/css");
      style.setInnerText(
            ".ace_editor {\n" +
            "border: none !important;\n" +
            "}\n" +
            ".ace_editor, .ace_text-layer {\n" +
            "font-family: " + ThemeFonts.getFixedWidthFont() + " !important;\n" +
            "}");
      body.appendChild(style);

      DivElement div = doc.createDivElement();
      div.setId("editor");
      div.getStyle().setWidth(100, Unit.PCT);
      div.getStyle().setHeight(100, Unit.PCT);
      div.setInnerText("hello");
      body.appendChild(div);

      FontSizer.injectStylesIntoDocument(doc);
      FontSizer.applyNormalFontSize(div);

      new ExternalJavaScriptLoader(doc, AceResources.INSTANCE.acejs().getUrl())
            .addCallback(new Callback()
      {
         public void onLoaded()
         {
            new ExternalJavaScriptLoader(doc, AceResources.INSTANCE.acesupportjs().getUrl())
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
                              "var RMode = require('mode/r').Mode;\n" +
                              "editor.getSession().setMode(new RMode(false));"));
                     }
                  });
         }
      });
   }

   public void setTheme(String themeUrl)
   {
      if (!isFrameLoaded_)
      {
         initialThemeUrl_ = themeUrl;
         return;
      }

      if (currentStyleLink_ != null)
         currentStyleLink_.removeFromParent();

      Document doc = getDocument();
      currentStyleLink_ = doc.createLinkElement();
      currentStyleLink_.setRel("stylesheet");
      currentStyleLink_.setType("text/css");
      currentStyleLink_.setHref(themeUrl);
      doc.getBody().appendChild(currentStyleLink_);
   }

   public void setFontSize(Size fontSize)
   {
      if (!isFrameLoaded_)
      {
         hasInitialFontSize_ = true;
         initialFontSize_ = fontSize;
         return;
      }

      FontSizer.setNormalFontSize(getDocument(), fontSize);
   }

   private LinkElement currentStyleLink_;
   private boolean isFrameLoaded_;
   private String initialThemeUrl_;
   private boolean hasInitialFontSize_;
   private Size initialFontSize_;
}
