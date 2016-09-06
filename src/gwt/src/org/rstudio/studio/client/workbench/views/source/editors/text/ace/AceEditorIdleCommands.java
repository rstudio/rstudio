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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.mathjax.MathJaxUtil;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
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
      if (isImageHref(href))
         onPreviewImage(editor, href, position, tokenRange);
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
      
      // display stand-alone links as line widgets
      String line = editor.getLine(position.getRow());
      if (isStandaloneMarkdownLink(line))
      {
         onPreviewImageLineWidget(editor, href, position, tokenRange);
         return;
      }
      
      // construct image el, place in popup, and show
      AnchoredPopupPanel panel = new AnchoredPopupPanel(editor, tokenRange, href);
      panel.getElement().setId(encoded);
      
      ScreenCoordinates coordinates =
            editor.documentPositionToScreenCoordinates(position);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
   }
   
   private void onPreviewImageLineWidget(final AceEditor editor,
                                         final String href,
                                         final Position position,
                                         final Range tokenRange)
   {
      // if we already have a line widget for this row, bail
      LineWidget lineWidget = editor.getLineWidgetForRow(position.getRow());
      if (lineWidget != null)
         return;
      
      // shared mutable state that we hide in this closure
      final Mutable<PinnedLineWidget>  plw = new Mutable<PinnedLineWidget>();
      final Mutable<ChunkOutputWidget> cow = new Mutable<ChunkOutputWidget>();
      final Mutable<HandlerRegistration> docChangedHandler = new Mutable<HandlerRegistration>(); 
      final Mutable<HandlerRegistration> renderHandler = new Mutable<HandlerRegistration>();
      
      // command that ensures state is cleaned up when widget hidden
      final Command onDetach = new Command()
      {
         private void detach()
         {
            // detach chunk output widget
            cow.set(null);

            // detach pinned line widget
            if (plw.get() != null)
               plw.get().detach();
            plw.set(null);

            // detach render handler
            if (renderHandler.get() != null)
               renderHandler.get().removeHandler();
            renderHandler.set(null);

            // detach doc changed handler
            if (docChangedHandler.get() != null)
               docChangedHandler.get().removeHandler();
            docChangedHandler.set(null);
         }
         
         @Override
         public void execute()
         {
            // if the associated chunk output widget has been cleaned up,
            // make a last-ditch detach effort anyhow
            ChunkOutputWidget widget = cow.get();
            if (widget == null)
            {
               detach();
               return;
            }
            
            // fade out and then detach
            FadeOutAnimation anim = new FadeOutAnimation(widget, new Command()
            {
               @Override
               public void execute()
               {
                  detach();
               }
            });
            
            anim.run(400);
         }
      };
      
      // construct placeholder for image
      final SimplePanel container = new SimplePanel();
      final Label noImageLabel = new Label("(No image at path " + href + ")");
      
      // resize command (used by various routines that need to respond
      // to width / height change events)
      final CommandWithArg<Integer> onResize = new CommandWithArg<Integer>()
      {
         @Override
         public void execute(Integer height)
         {
            // defend against missing chunk output widget (can happen if a widget
            // is closed / dismissed before image finishes loading)
            ChunkOutputWidget widget = cow.get();
            if (widget == null)
               return;
            
            widget.getFrame().setHeight(height + "px");
            LineWidget lw = plw.get().getLineWidget();
            lw.setPixelHeight(height);
            editor.onLineWidgetChanged(lw);
         }
      };
      
      // construct our image
      final Image image = new Image(imgSrcPathFromHref(href));
      
      // add load handlers to image
      final Element imgEl = image.getElement();
      DOM.sinkEvents(imgEl, Event.ONLOAD | Event.ONERROR);
      DOM.setEventListener(imgEl, new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONLOAD)
            {
               // set styles
               ImageElementEx imgEl = image.getElement().cast();
               
               int minWidth = Math.min(imgEl.naturalWidth(), 100);
               int maxWidth = Math.min(imgEl.naturalWidth(), 650);
               
               Style style = imgEl.getStyle();
               style.setProperty("width", "100%");
               style.setProperty("minWidth", minWidth + "px");
               style.setProperty("maxWidth", maxWidth + "px"); 
               
               // attach to container
               container.setWidget(image);
               
               // update widget
               int height = image.getOffsetHeight() + 10;
               onResize.execute(height);
            }
            else if (DOM.eventGetType(event) == Event.ONERROR)
            {
               container.setWidget(noImageLabel);
            }
         }
      });
      
      // handle editor resize events
      final Timer renderTimer = new Timer()
      {
         @Override
         public void run()
         {
            int height = image.getOffsetHeight() + 30;
            onResize.execute(height);
         }
      };
      
      // initialize render handler
      renderHandler.set(editor.addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      {
         private int width_;
         
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            int width = editor.getWidget().getOffsetWidth();
            if (width == width_)
               return;
            
            width_ = width;
            renderTimer.schedule(50);
         }
      }));
      
      // initialize doc changed handler
      docChangedHandler.set(editor.addDocumentChangedHandler(new DocumentChangedEvent.Handler()
      {
         String href_;
         final Timer refreshImageTimer = new Timer()
         {
            @Override
            public void run()
            {
               // if the discovered href isn't an image link, just bail
               if (!isImageHref(href_))
                  return;
               
               // set new src location (load handler will replace label as needed)
               image.getElement().setAttribute("src", imgSrcPathFromHref(href_));
            }
         };
         
         @Override
         public void onDocumentChanged(DocumentChangedEvent event)
         {
            int row = plw.get().getRow();
            Range range = event.getEvent().getRange();
            if (range.getStart().getRow() <= row && row <= range.getEnd().getRow())
            {
               String line = editor.getLine(row);
               if (isStandaloneMarkdownLink(line))
               {
                  // check to see if the URL text has been updated
                  Token hrefToken = null;
                  JsArray<Token> tokens = editor.getSession().getTokens(row);
                  for (Token token : JsUtil.asIterable(tokens))
                  {
                     if (token.hasType("href"))
                     {
                        hrefToken = token;
                        break;
                     }
                  }
                  
                  if (hrefToken == null)
                     return;
                  
                  href_ = hrefToken.getValue();
                  refreshImageTimer.schedule(700);
               }
               else
               {
                  onDetach.execute();
               }
            }
         }
      }));
      
      ChunkOutputHost host = new ChunkOutputHost()
      {
         @Override
         public void onOutputRemoved(final ChunkOutputWidget widget)
         {
            onDetach.execute();
         }
         
         @Override
         public void onOutputHeightChanged(ChunkOutputWidget widget,
                                           int height,
                                           boolean ensureVisible)
         {
            onResize.execute(height);
         }
      };
      
      cow.set(new ChunkOutputWidget(
            StringUtil.makeRandomId(8),
            StringUtil.makeRandomId(8),
            RmdChunkOptions.create(),
            ChunkOutputWidget.EXPANDED,
            host));
      
      ChunkOutputWidget outputWidget = cow.get();
      outputWidget.setRootWidget(container);
      outputWidget.hideSatellitePopup();

      plw.set(new PinnedLineWidget("image", editor, outputWidget, position.getRow(), null, null));
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
   
   private static boolean isStandaloneMarkdownLink(String line)
   {
      return line.matches("^\\s*!\\s*\\[[^\\]]*\\]\\s*\\([^)]*\\)\\s*$");
   }
   
   private static boolean isImageHref(String href)
   {
      return
          href.endsWith(".png")  ||
          href.endsWith(".jpg")  ||
          href.endsWith(".jpeg") ||
          href.endsWith(".gif")  ||
          href.endsWith(".svg");
   }
   
   private class AnchoredPopupPanel extends MiniPopupPanel
   {
      public AnchoredPopupPanel(AceEditor editor, Range range, String href)
      {
         super(true, false);
         
         // defer visibility until image has finished loading
         setVisible(false);
         image_ = new Image(imgSrcPathFromHref(href));
         error_ = new Label("No image at path " + href);
         
         final Element imgEl = image_.getElement();
         DOM.sinkEvents(imgEl, Event.ONLOAD | Event.ONERROR);
         DOM.setEventListener(imgEl, new EventListener()
         {
            @Override
            public void onBrowserEvent(Event event)
            {
               if (DOM.eventGetType(event) == Event.ONLOAD)
               {
                  setWidget(image_);
                  showSmall();
               }
               else
               {
                  setWidget(error_);
                  setVisible(true);
               }
            }
         });
         
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
      private final Label error_;
      
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