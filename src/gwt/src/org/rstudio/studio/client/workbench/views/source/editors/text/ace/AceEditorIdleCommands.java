/*
 * AceEditorIdleCommands.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.mathjax.MathJaxUtil;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AceEditorIdleCommands
{
   public AceEditorIdleCommands()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      PREVIEW_LINK = previewLink();
      PREVIEW_LATEX = previewLatex();
   }
   
   // Private Methods ----
   
   @Inject
   private void initialize(SourceWindowManager swm)
   {
      swm_ = swm;
   }
   
// Latex Preview ----
   
   private IdleCommand previewLatex()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(AceEditor editor, IdleState state)
         {
            onPreviewLatex(editor, state);
         }
      };
   }
   
   private void onPreviewLatex(AceEditor editor, IdleState state)
   {
      Position position = resolvePosition(editor, state);
      Range range = MathJaxUtil.getLatexRange(editor, position);
      if (range == null)
         return;
      
      editor.renderLatex(range);
   }
   
   // Link Preview ----
   
   private IdleCommand previewLink()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(AceEditor editor, IdleState idleState)
         {
            onPreviewLink(editor, idleState);
         }
      };
   }
   
   private void onPreviewLink(AceEditor editor, IdleState idleState)
   {
      Position position = resolvePosition(editor, idleState);
      Token token = editor.getTokenAt(position);
      if (token == null)
         return;

      if (!token.hasType("href"))
         return;

      Range tokenRange = Range.fromPoints(
            Position.create(position.getRow(), token.getColumn()),
            Position.create(position.getRow(), token.getColumn() + token.getValue().length()));
            
      String href = token.getValue();
      if (href.endsWith(".png")  ||
          href.endsWith(".jpg")  ||
          href.endsWith(".jpeg") ||
          href.endsWith(".gif")  ||
          href.endsWith(".svg"))
      {
         onPreviewImage(editor, href, position, tokenRange);
      }
   }
   
   private void onPreviewImage(AceEditor editor,
                               String href,
                               Position position,
                               Range tokenRange)
   {
      // for local images, construct a path that the server URI redirects
      // will handle properly
      String srcPath = imgSrcPathFromHref(href);
      
      // check to see if we already have a popup showing for this image;
      // if we do then bail early
      String encoded = StringUtil.encodeURIComponent(href);
      Element el = Document.get().getElementById(encoded);
      if (el != null)
         return;
      
      // construct image el, place in popup, and show
      AnchoredPopupPanel panel = new AnchoredPopupPanel(editor, tokenRange, srcPath);
      panel.getElement().setId(encoded);
      
      ScreenCoordinates coordinates =
            editor.documentPositionToScreenCoordinates(position);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
   }
   
   private String imgSrcPathFromHref(String href)
   {
      // return paths that have a custom / external protocol as-is
      Pattern reProtocol = Pattern.create("^\\w+://");
      if (reProtocol.test(href))
         return href;
      
      // make relative paths absolute
      String absPath = href;
      if (FilePathUtils.pathIsRelative(href))
      {
         // TODO: infer correct parent dir based on knitr params?
         String docPath = swm_.getCurrentDocPath();
         absPath = FilePathUtils.dirFromFile(docPath) + "/" + absPath;
      }
      
      return "file_show?path=" + StringUtil.encodeURIComponent(absPath) + "&id=" + ID++;
   }
   
   private static Position resolvePosition(AceEditor editor, IdleState state)
   {
      int type = state.getType();
      if (type == IdleState.STATE_CURSOR_IDLE)
         return editor.getCursorPosition();
      else if (type == IdleState.STATE_MOUSE_IDLE)
         return editor.screenCoordinatesToDocumentPosition(state.getMouseX(), state.getMouseY());
      
      assert false : "Unhandled idle state type '" + type + "'";
      return Position.create(0, 0);
   }
   
   private static class AnchoredPopupPanel extends MiniPopupPanel
   {
      public AnchoredPopupPanel(AceEditor editor, Range range, String href)
      {
         super(true, false);
         
         // defer visibility until image has finished loading
         setVisible(false);
         image_ = new Image(href);
         image_.addLoadHandler(new LoadHandler()
         {
            @Override
            public void onLoad(LoadEvent event)
            {
               showSmall();
            }
         });
         add(image_);
         
         // allow zoom with double-click
         setTitle("Double-Click to Zoom");
         addDomHandler(new DoubleClickHandler()
         {
            @Override
            public void onDoubleClick(DoubleClickEvent event)
            {
               toggleSize();
            }
         }, DoubleClickEvent.getType());
         
         // use anchor + cursor changed handler for smart auto-dismiss
         anchor_ = editor.createAnchoredSelection(
               range.getStart(),
               range.getEnd());
         
         handler_ = editor.addCursorChangedHandler(new CursorChangedHandler()
         {
            @Override
            public void onCursorChanged(CursorChangedEvent event)
            {
               Position position = event.getPosition();
               if (!anchor_.getRange().contains(position))
                  hide();
            }
         });
         
         addAttachHandler(new AttachEvent.Handler()
         {
            @Override
            public void onAttachOrDetach(AttachEvent event)
            {
               if (!event.isAttached())
                  detachHandlers();
            }
         });
      }
      
      private boolean isSmall()
      {
         ImageElementEx el = image_.getElement().cast();
         Style style = el.getStyle();
         return
               SMALL_MAX_WIDTH.equals(style.getProperty("maxWidth")) ||
               SMALL_MAX_HEIGHT.equals(style.getProperty("maxHeight"));
      }
      
      private void showSmall()
      {
         showWithDimensions(SMALL_MAX_WIDTH, SMALL_MAX_HEIGHT);
      }
      
      private void showLarge()
      {
         showWithDimensions(LARGE_MAX_WIDTH, LARGE_MAX_HEIGHT);
      }
      
      private void showWithDimensions(String width, String height)
      {
         setVisible(true);
         
         ImageElementEx el = image_.getElement().cast();
         Style style = el.getStyle();
         
         boolean isWide = el.naturalWidth() > el.naturalHeight();
         if (isWide)
            style.setProperty("maxWidth", width);
         else
            style.setProperty("maxHeight", height);
      }
      
      private void toggleSize()
      {
         if (isSmall())
            showLarge();
         else
            showSmall();
      }
      
      private void detachHandlers()
      {
         anchor_.detach();
         handler_.removeHandler();
      }
      
      private final AnchoredSelection anchor_;
      private final HandlerRegistration handler_;
      private final Image image_;
      
      private static final String SMALL_MAX_WIDTH  = "100px";
      private static final String SMALL_MAX_HEIGHT = "100px";
      
      private static final String LARGE_MAX_WIDTH  = "400px";
      private static final String LARGE_MAX_HEIGHT = "600px";
   }
   
   public final IdleCommand PREVIEW_LINK;
   public final IdleCommand PREVIEW_LATEX;
   
   private static int ID;
   
   // Injected ----
   private SourceWindowManager swm_;
}