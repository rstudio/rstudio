/*
 * ChunkIconsManager.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;


import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.DisplayChunkOptionsEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.ExecuteChunksEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.events.AfterAceRenderEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChunkIconsManager
{
   public ChunkIconsManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      events_.addHandler(
            AfterAceRenderEvent.TYPE,
            new AfterAceRenderEvent.Handler()
            {
               
               @Override
               public void onAfterAceRender(AfterAceRenderEvent event)
               {
                  manageChunkIcons(event.getEditor());
               }
            });
   }
   
   @Inject
   private void initialize(EventBus events,
                           UIPrefs uiPrefs,
                           AceThemes themes)
   {
      events_ = events;
      uiPrefs_ = uiPrefs;
   }
  
   private boolean isPseudoMarker(Element el)
   {
      return el.getOffsetHeight() == 0 || el.getOffsetWidth() == 0;
   }
   
   private boolean shouldDisplayIcons(AceEditorNative editor)
   {
      String id = editor.getSession().getMode().getId();
      return id.equals("mode/RMarkdown");  // also Rpres
   }
   
   private Position toDocumentPosition(Element el, AceEditorNative editor)
   {
      int pageX = el.getAbsoluteLeft();
      int pageY = el.getAbsoluteTop();
      
      return editor.getRenderer().screenToTextCoordinates(pageX, pageY);
   }
   
   private boolean isRunnableChunk(Element el, AceEditorNative editor)
   {
      Position pos = toDocumentPosition(el, editor);
      String text = editor.getSession().getLine(pos.getRow());
      
      // Check for R Markdown chunks, and verify that the engine is 'r' or 'rscript'.
      // First, check for chunk headers of the form:
      //
      //     ```{r ...}
      //
      // as opposed to
      //
      //     ```{sh ...}
      String lower = text.toLowerCase().trim();
      if (lower.startsWith("```{"))
      {
         Pattern reREngine = Pattern.create("```{r(?:script)?[ ,}]", "");
         if (!reREngine.test(lower))
            return false;
      }
      
      // If this is an 'R' chunk, it's possible that an alternate engine
      // has been specified, e.g.
      //
      //     ```{r, engine = 'awk'}
      //
      // which is the 'old-fashioned' way of specifying non-R chunks.
      Pattern pattern = Pattern.create("engine\\s*=\\s*['\"]([^'\"]*)['\"]", "");
      Match match = pattern.match(text, 0);
      
      if (match == null)
         return true;
      
      String engine = match.getGroup(1).toLowerCase();
      
      return engine.equals("r") || engine.equals("rscript");
   }
   
   private boolean chunkLiesWithinFoldedMarkdownSection(Element el, AceEditorNative editor)
   {
      Position pos = toDocumentPosition(el, editor);
      AceFold fold = editor.getSession().getFoldAt(pos.getRow() + 1, 0);
      if (fold == null)
         return false;
      
      Position foldPos = fold.getStart();
      String state = editor.getSession().getState(foldPos.getRow());
      return !"r-start".equals(state);
   }
   
   private void manageChunkIcons(AceEditorNative editor)
   {
      Element container = editor.getContainer();
      if (container == null)
         return;
      
      if (!uiPrefs_.showInlineToolbarForRCodeChunks().getValue())
         return;
      
      if (!shouldDisplayIcons(editor))
         return;
      
      Element[] chunkStarts = DomUtils.getElementsByClassName("rstudio_chunk_start");
      
      for (int i = 0; i < chunkStarts.length; i++)
      {
         Element el = chunkStarts[i];
         
         if (isPseudoMarker(el))
            continue;
         
         if (!isRunnableChunk(el, editor))
            continue;
         
         if (!DomUtils.isVisibleVert(container, el))
            continue;
         
         if (chunkLiesWithinFoldedMarkdownSection(el, editor))
            continue;
         
         ensureToolbar(el, isSetupChunk(el, editor), editor);
      }
   }
   
   private boolean isSetupChunk(Element el, AceEditorNative editor)
   {
      int pageX = el.getAbsoluteLeft() + 5;
      int pageY = el.getAbsoluteTop() + 5;
      
      Position position =
            editor.getRenderer().screenToTextCoordinates(pageX, pageY);
      
      String line = editor.getSession().getLine(position.getRow());
      return line.contains("r setup");
   }
   
   private void ensureToolbar(Element el, boolean isSetupChunk, AceEditorNative editor)
   {

   }
   
   public final void fireExecuteChunkEvent(Object object)
   {
      fireExecuteChunksEvent(ExecuteChunksEvent.Scope.Current, object);
   }
   
   public final void fireExecutePreviousChunksEvent(Object object)
   {
      fireExecuteChunksEvent(ExecuteChunksEvent.Scope.Previous, object);
   }
   
   public final void fireExecuteChunksEvent(ExecuteChunksEvent.Scope scope,
                                             Object object)
   {
      if (!(object instanceof NativeEvent))
         return;
      
      NativeEvent event = (NativeEvent) object;
      events_.fireEvent(new ExecuteChunksEvent(scope, 
                                               event.getClientX(), 
                                               event.getClientY()));
   }
   
   public final void fireDisplayChunkOptionsEvent(Object object)
   {
      if (!(object instanceof NativeEvent))
         return;
      
      NativeEvent event = (NativeEvent) object;
      events_.fireEvent(new DisplayChunkOptionsEvent(event));
   }
   
   public void displayChunkOptions(AceEditor editor, NativeEvent event)
   {
      // Translate the 'pageX' + 'pageY' position to document position
      int pageX = event.getClientX();
      int pageY = event.getClientY();
      
      Renderer renderer = editor.getWidget().getEditor().getRenderer();
      Position position = renderer.screenToTextCoordinates(pageX, pageY);
      
      if (optionsPanel_ != null)
         optionsPanel_ = null;
      
      Element el = event.getEventTarget().cast();
      /*
      if (el.hasClassName(RES.styles().setupChunk()))
         optionsPanel_ = new SetupChunkOptionsPopupPanel();
      else
         optionsPanel_ = new DefaultChunkOptionsPopupPanel();
         */
      
      optionsPanel_.init(editor.getWidget(), position);
      optionsPanel_.show();
      optionsPanel_.focus();
      PopupPositioner.setPopupPosition(
            optionsPanel_,
            pageX,
            pageY,
            10);
      
   }
   
   private ChunkOptionsPopupPanel optionsPanel_;
   
   private EventBus events_;
   private UIPrefs uiPrefs_;
   
   public interface Styles extends CssResource
   {
      String setupChunk();
   }
}
