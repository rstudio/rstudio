/*
 * PanmirrorEditor.java
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

package org.rstudio.studio.client.panmirror;

import jsinterop.annotations.JsType;

import org.rstudio.core.client.jsinterop.JsConsumerFunction;
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;
import org.rstudio.studio.client.panmirror.command.PanmirrorMenus;
import org.rstudio.studio.client.panmirror.findreplace.PanmirrorFindReplace;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocation;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineItem;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorSpellingDoc;
import org.rstudio.studio.client.panmirror.theme.PanmirrorTheme;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;

import com.google.gwt.dom.client.Element;

import elemental2.core.JsObject;
import elemental2.promise.Promise;


@JsType(isNative = true, name="Editor", namespace = "Panmirror")
public class PanmirrorEditor
{
   public native static Promise<PanmirrorEditor> create(Element parent, 
                                                        PanmirrorContext context, 
                                                        PanmirrorFormat format,
                                                        PanmirrorOptions options);
   
   public native void destroy();
   
   public native void setTitle(String title);
   public native String getTitle();
   
   public native Promise<JsObject> setMarkdown(String code, PanmirrorWriterOptions options, boolean emitUpdate);
   
   public native Promise<JsObject> getMarkdown(PanmirrorWriterOptions options);
   
   public native Promise<String> getCanonical(String code, PanmirrorWriterOptions options);
   
   public native boolean isInitialDoc();
   
   public native JsVoidFunction subscribe(String event, JsConsumerFunction handler);
   
   public native PanmirrorCommand[] commands();
   
   public native String getHTML();
   
   public native String getSelectedText();
   public native void replaceSelection(String value);
   
   public native PanmirrorSelection getSelection();
   
   public native PanmirrorMenus getMenus();
   
   public native PanmirrorOutlineItem[] getOutline();
   
   public native PanmirrorFindReplace getFindReplace();
   
   public native PanmirrorSpellingDoc getSpellingDoc();
   public native void spellingInvalidateAllWords();
   public native void spellingInvalidateWord(String word);
   
   public native PanmirrorEditingLocation getEditingLocation();
   
   public native void setEditingLocation(
      PanmirrorEditingOutlineLocation outlineLocation, 
      PanmirrorEditingLocation previousLocation
   );
   
   public native String getYamlFrontMatter();
   public native void applyYamlFrontMatter(String yaml);
   
   public native void focus();
   public native void blur();
   
   public native void resize();
   
   public native void insertChunk(String chunkPlaceholder, int rowOffset, int colOffset);
   
   public native void navigate(String type, String location, boolean recordCurrent);

   public native void applyTheme(PanmirrorTheme theme);
   
   public native void setMaxContentWidth(int maxWidth, int minPadding);
   
   public native void setKeybindings(PanmirrorKeybindings keybindings);
   
   public native PanmirrorFormat getEditorFormat();
   public native PanmirrorPandocFormat getPandocFormat();
   public native PanmirrorPandocFormatConfig getPandocFormatConfig(boolean isRmd);
   
   public native void enableDevTools(JsObject initFn);
 
}
