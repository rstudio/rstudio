package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.widget.DynamicIFrame;
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

      final Document doc = getDocument();
      final BodyElement body = doc.getBody();
      body.getStyle().setMargin(0, Unit.PX);

      StyleElement style = doc.createStyleElement();
      style.setType("text/css");
      style.setInnerText(".ace_editor {border: none !important;}");
      body.appendChild(style);

      DivElement div = doc.createDivElement();
      div.setId("editor");
      div.getStyle().setWidth(100, Unit.PCT);
      div.getStyle().setHeight(100, Unit.PCT);
      div.setInnerText("hello");
      body.appendChild(div);

      ExternalJavaScriptLoader loader = new ExternalJavaScriptLoader(
            doc, AceResources.INSTANCE.acejs().getUrl());
      loader.addCallback(new Callback()
      {
         public void onLoaded()
         {
            body.appendChild(doc.createScriptElement(
                  "var editor = ace.edit('editor');\n" +
                  "editor.renderer.setHScrollBarAlwaysVisible(false);\n" +
                  "editor.setHighlightActiveLine(false);"));
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

   private LinkElement currentStyleLink_;
   private boolean isFrameLoaded_;
   private String initialThemeUrl_;
}
