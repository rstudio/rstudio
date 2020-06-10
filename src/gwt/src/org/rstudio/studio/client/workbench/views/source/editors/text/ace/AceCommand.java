/*
 * AceCommand.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;

public class AceCommand extends JavaScriptObject
{
   protected AceCommand() {}
   
   public final native boolean isPlaceHolder()
   /*-{
      return this.bindKeys == null;
   }-*/;
   
   public final native String getInternalName()
   /*-{
         return this.name;
   }-*/;

   public final native JavaScriptObject getInternalBindings()
   /*-{
         return this.bindKeys;
   }-*/;

   public final String getDisplayName()
   {
      String internal = getInternalName();
      if (DISPLAY_NAME_MAP.hasKey(internal))
         return DISPLAY_NAME_MAP.getString(internal);
      else
         return StringUtil.prettyCamel(internal);
   }

   private final native String getDisplayName(String name, JavaScriptObject map)
   /*-{
      return map[name] || name;
   }-*/;

   public final JsArrayString getWindowsKeyBinding()
   {
      return getKeyBindings(true);
   }

   public final JsArrayString getMacKeyBinding()
   {
      return getKeyBindings(false);
   }

   public final JsArrayString getBindingsForCurrentPlatform()
   {
      return getKeyBindings(BrowseCap.isWindows());
   }

   private final native JsArrayString getKeyBindings(boolean isWindows)
   /*-{
      var binding = this.bindKey;

      if (binding == null) {
         return [];
      }

      if (typeof binding === "string") {
         return binding.split("|");
      }

      var shortcut = isWindows ? binding.win : binding.mac;
      if (typeof shortcut === "string") {
         return shortcut.split("|");
      } else {
         return [];
      }
      
   }-*/;

   public final native boolean isReadOnly()
   /*-{
      return this.readOnly;
   }-*/;

   private static final native JsObject makeDisplayNameMap()
   /*-{
      var $ = {};
      
      $["selectall"] = "Select All";
      $["centerselection"] = "Center Selection";
      $["gotoline"] = "Go To Line";
      $["foldall"] = "Fold All";
      $["unfoldall"] = "Unfold All";
      $["findnext"] = "Find Next";
      $["findprevious"] = "Find Previous";
   
      $["selecttostart"] = "Select to Beginning of Document";
      $["gotostart"] = "Go to Start of Document";
      $["selectup"] = "Select Line Upwards";
      $["golineup"] = "Move Upwards One Line";
   
      $["selecttoend"] = "Select to End of Document";
      $["gotoend"] = "Go to End of Document";
      $["selectdown"] = "Select Line Downwards";
      $["golinedown"] = "Move Downwards one Line";
   
      $["selectwordleft"] = "Select Previous Word";
      $["gotowordleft"] = "Go to Previous Word";
   
      $["selecttolinestart"] = "Select to Line Start";
      $["gotolinestart"] = "Go to Start of Line";
   
      $["selectleft"] = "Select Previous Character";
      $["gotoleft"] = "Move to Previous Character";
   
      $["selectwordright"] = "Select Next Word";
      $["gotowordright"] = "Move to Next Word";
   
      $["selecttolineend"] = "Select to End of Line";
      $["gotolineend"] = "Go to End of Line";
   
      $["selectright"] = "Select Next Character";
      $["gotoright"] = "Go to Next Character";
   
      $["selectpagedown"] = "Expand Selection down a Page";
      $["pagedown"] = "Scroll View Down a Page";
      $["gotopagedown"] = "Move Cursor Down a Page";
   
      $["selectpageup"] = "Expand Selection Up a Page";
      $["pageup"] = "Scroll View Up a Page";
      $["gotopageup"] = "Move Cursor Up a Page";
   
      $["scrollup"] = "Scroll Up a Line";
      $["scrolldown"] = "Scroll Down a Line";
   
      $["selectlinestart"] = "Select to Start of Line";
      $["selectlineend"] = "Select to End of Line";
   
      $["jumptomatching"] = "Jump to Matching Bracket";
      $["selecttomatching"] = "Select to Matching Bracket";
   
      $["removeline"] = "Remove Line";
      $["sortlines"] = "Sort Lines";
      $["togglecomment"] = "Toggle Comment";
      $["modifyNumberUp"] = "Increment Number at Cursor";
      $["modifyNumberDown"] = "Decrement Number at Cursor";
   
      $["copylinesup"] = "Copy Lines Up";
      $["movelinesup"] = "Move Lines Up";
      $["copylinesdown"] = "Copy Lines Down";
      $["movelinesdown"] = "Move Lines Down";
   
      $["del"] = "Delete";
      $["cut_or_delete"] = "Cut of Delete";
   
      $["removetolinestart"] = "Remove to Start of Line";
      $["removetolineend"] = "Remove to End of Line";
      $["removewordleft"] = "Remove Word Left";
      $["removewordright"] = "Remove Word Right";
   
      $["blockoutdent"] = "Block Outdent";
      $["blockindent"] = "Block Indent";
   
      $["splitline"] = "Split Line";
      $["transposeletters"] = "Transpose Letters";
      $["touppercase"] = "To Upper Case";
      $["tolowercase"] = "To Lower Case";
      $["expandtoline"] = "Expand to Line";
      $["joinlines"] = "Join Lines";
   
      return $;
   }-*/;
   
   public final native boolean isCustomBinding() /*-{ return !!this.isCustom; }-*/;

   private static final JsObject DISPLAY_NAME_MAP = makeDisplayNameMap();
}
