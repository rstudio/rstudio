/*
 * HtmlMessageListener.java
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
package org.rstudio.core.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

@Singleton
public class HtmlMessageListener
{
   @Inject
   public HtmlMessageListener(FileTypeRegistry fileTypeRegistry,
                                 EventBus eventBus,
                                 Provider<UserPrefs> pUIPrefs)
   {
      htmlMessageListener_ = this;
      fileTypeRegistry_ = fileTypeRegistry;
      pUserPrefs_ = pUIPrefs;
      themeSources_ = new ArrayList<JavaScriptObject>();
      
      initializeMessageListeners();

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, new UserPrefsChangedHandler() 
      {
         @Override
         public void onUserPrefsChanged(UserPrefsChangedEvent e)
         {
            if (e.getName() == PrefLayer.LAYER_USER)
            {
               for (JavaScriptObject themeSource : themeSources_)
               {
                  postThemeMessage(themeSource, themeOrigin_);
               }
            }
         }
      });
   }

   private static String getDomainFromUrl(String url)
   {
      RegExp reg = RegExp.compile("https?://[^/]+");
      MatchResult result = reg.exec(url);
      if (result != null)
      {
         return result.getGroup(0);
      }

      return "";
   }

   private native static String getOrigin() /*-{
     return $wnd.location.origin;
   }-*/;
   
   public void setUrl(String url)
   {
      url_ = url;
   }
   
   public static String getCurrentDomain()
   {
      if (htmlMessageListener_ == null) return "";

      return getDomainFromUrl(htmlMessageListener_.url_);
   }
   
   private void openFileFromMessage(final String file,
                                    final int line,
                                    final int column,
                                    final boolean highlight)
   {
      if (highlight && !highlightAllowed_) return;

      FilePosition filePosition = FilePosition.create(line, column);
      CodeNavigationTarget navigationTarget = new CodeNavigationTarget(file, filePosition);

      fileTypeRegistry_.editFile(
         FileSystemItem.createFile(navigationTarget.getFile()),
         filePosition,
         highlight);

      highlightAllowed_ = false;
   }

   public static void onOpenFileFromMessage(final String file, int line, int column, boolean highlight)
   {
      if (htmlMessageListener_ != null)
      {
         htmlMessageListener_.openFileFromMessage(file, line, column, highlight);
      }
   }

   private void registerThemeOriginImpl(JavaScriptObject source, String origin)
   {
      themeOrigin_ = origin;
      themeSources_.add(source);

      AceTheme editorTheme = pUserState_.get().theme().getGlobalValue().cast();
      if (editorTheme != null) {
         postThemeMessage(source, themeOrigin_);
      }
   }

   private static void registerThemeOrigin(JavaScriptObject source, String origin)
   {
      htmlMessageListener_.registerThemeOriginImpl(source, origin);
   }
   
   private native static void initializeMessageListeners() /*-{
      var handler = $entry(function(e) {
         var domain = @org.rstudio.core.client.HtmlMessageListener::getCurrentDomain()();
         if (typeof e.data != 'object')
            return;
         if (e.origin != $wnd.location.origin && e.origin != domain)
            return;
         if (e.data.source != "r2d3")
            return;
            
         if (e.data.message === "openfile") {
            @org.rstudio.core.client.HtmlMessageListener::onOpenFileFromMessage(Ljava/lang/String;IIZ)(
               e.data.file,
               parseInt(e.data.line),
               parseInt(e.data.column),
               e.data.highlight === true
            );
         }
         else if (e.data.message === "ontheme") {
            @org.rstudio.core.client.HtmlMessageListener::registerThemeOrigin(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(e.source, e.origin);
         }
      });
      $wnd.addEventListener("message", handler, true);
   }-*/;

   private native static void postThemeMessage(JavaScriptObject source, String origin) /*-{
      source.postMessage({
         message: "ontheme"
      }, origin);
   }-*/;
   
   public String getOriginDomain()
   {
      return getDomainFromUrl(getOrigin());
   }
   
   public void allowOpenOnLoad()
   {
      highlightAllowed_ = true;
   }
   
   private final FileTypeRegistry fileTypeRegistry_;
   private static HtmlMessageListener htmlMessageListener_;
   private Provider<UserPrefs> pUserPrefs_;
   private Provider<UserState> pUserState_;

   private String url_;
   private boolean highlightAllowed_;

   private String themeOrigin_;
   private ArrayList<JavaScriptObject> themeSources_;
}
