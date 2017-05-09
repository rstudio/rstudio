/*
 * RStudioFrame.java
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;

public class RStudioFrame extends Frame
{
   public RStudioFrame()
   {
      this(null);
   }
   
   public RStudioFrame(String url)
   {
      this(url, false, null);
   }
   
   public RStudioFrame(String url, boolean sandbox, String sandboxAllow)
   {
      super();
      if (sandbox)
         getElement().setAttribute("sandbox", StringUtil.notNull(sandboxAllow));
      if (url != null)
         setUrl(url);
   }
   
   private void addThemesStyle(String customStyle, String urlStyle)
   {
      if (getWindow() != null && getWindow().getDocument() != null)
      {
         Document document = getWindow().getDocument();
         
         if (customStyle != null) {
            StyleElement style = document.createStyleElement();
            style.setInnerHTML(customStyle);
            document.getHead().appendChild(style);
         }
         
         if (urlStyle != null) {
            LinkElement styleLink = document.createLinkElement();
            styleLink.setHref(urlStyle);
            styleLink.setRel("stylesheet");
            document.getHead().appendChild(styleLink);
         }
         
         RStudioGinjector.INSTANCE.getAceThemes().applyTheme(document);
         
         BodyElement body = document.getBody();
         if (body != null)
         {
            String themeName = RStudioGinjector.INSTANCE.getUIPrefs().getFlatTheme().getValue();
            RStudioThemes.initializeThemes(themeName, document, document.getBody());
            
            body.addClassName("ace_editor_theme");
         }
      }
   }
   
   public void setAceTheme()
   {
      setAceThemeAndCustomStyle(null);
   }
   
   public void setAceThemeAndCustomStyle(final String customStyle)
   {
      setAceThemeAndCustomStyle(customStyle, null);  
   }

   public void setAceThemeAndCustomStyle(
      final String customStyle,
      final String urlStyle)
   {
      addThemesStyle(customStyle, urlStyle);
      
      this.addLoadHandler(new LoadHandler()
      {      
         @Override
         public void onLoad(LoadEvent arg0)
         {
            addThemesStyle(customStyle, urlStyle);
         }
      });
   }
   
   public WindowEx getWindow()
   {
      return getIFrame().getContentWindow();
   }
   
   public IFrameElementEx getIFrame()
   {
      return getElement().cast();
   }

}
