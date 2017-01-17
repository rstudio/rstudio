/*
 * ImagePreviewer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;


import java.util.Map;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.html.HTMLAttributesParser;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class ImagePreviewer
{
   public ImagePreviewer(DocDisplay display, DocUpdateSentinel sentinel, 
         UIPrefs prefs)
   {
      display_ = display;
      prefs_ = prefs;
      sentinel_ = sentinel;
   }
   
   public void previewAllLinks()
   {
      for (int i = 0, n = display_.getRowCount(); i < n; i++)
      {
         String line = display_.getLine(i);
         if (isStandaloneMarkdownLink(line))
         {
            // perform a preview with the cursor on the href (past the opening
            // parenthetical)
            onPreviewLink(display_, sentinel_, prefs_, 
                  Position.create(i, line.indexOf('(') + 2));
         }
      }
   }
   
   public void removeAllPreviews()
   {
      JsArray<LineWidget> widgets = display_.getLineWidgets();
      for (int i = 0; i < widgets.length(); i++)
      {
         LineWidget widget = widgets.get(i);
         if (widget.getType() == LINE_WIDGET_TYPE)
            display_.removeLineWidget(widget);
      }
   }

   public static void onPreviewLink(DocDisplay display, 
         DocUpdateSentinel sentinel, UIPrefs prefs, Position position)
   {
      Token token = display.getTokenAt(position);
      if (token == null)
         return;

      if (!token.hasType("href"))
         return;
      
      Range tokenRange = Range.fromPoints(
            Position.create(position.getRow(), token.getColumn()),
            Position.create(position.getRow(), 
                  token.getColumn() + token.getValue().length()));
            
      String href = token.getValue();
      if (ImagePreviewer.isImageHref(href))
      {
         // extract HTML attributes from line for markdown links, e.g.
         //
         //    ![](plot.png){width=400 height=400}
         //
         String attributes = null;
         String line = display.getLine(position.getRow());
         if (isStandaloneMarkdownLink(line))
         {
            int startBraceIdx = line.indexOf("){");
            int endBraceIdx   = line.lastIndexOf("}");
            if (startBraceIdx != -1 &&
                endBraceIdx != -1 &&
                endBraceIdx > startBraceIdx)
            {
               attributes = line.substring(startBraceIdx + 2, endBraceIdx).trim();
            }
         }
         
         onPreviewImage(display, sentinel, prefs, href, attributes, position, tokenRange);
      }
   }
   
   private static void onPreviewImageLineWidget(final DocDisplay display,
                                                final DocUpdateSentinel sentinel,
                                                final String href, 
                                                final String attributes,
                                                final Position position,
                                                final Range tokenRange)
   {
      // if we already have a line widget for this row, bail
      LineWidget lineWidget = display.getLineWidgetForRow(position.getRow());
      if (lineWidget != null)
         return;
      
      // shared mutable state that we hide in this closure
      final Mutable<PinnedLineWidget>  plw = new Mutable<PinnedLineWidget>();
      final Mutable<ChunkOutputWidget> cow = new Mutable<ChunkOutputWidget>();
      final Mutable<HandlerRegistration> docChangedHandler = 
            new Mutable<HandlerRegistration>(); 
      final Mutable<HandlerRegistration> renderHandler = 
            new Mutable<HandlerRegistration>();
      
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
      container.addStyleName(RES.styles().container());
      final Label noImageLabel = new Label("(No image at path " + href + ")");
      
      // resize command (used by various routines that need to respond
      // to width / height change events)
      final CommandWithArg<Integer> onResize = new CommandWithArg<Integer>()
      {
         private int state_ = -1;
         
         @Override
         public void execute(Integer height)
         {
            // defend against missing chunk output widget (can happen if a widget
            // is closed / dismissed before image finishes loading)
            ChunkOutputWidget widget = cow.get();
            if (widget == null)
               return;
            
            // don't resize if the chunk widget if we were already collapsed
            int state = widget.getExpansionState();
            if (state == state_ && state == ChunkOutputWidget.COLLAPSED)
               return;
            
            state_ = state;
            widget.getFrame().setHeight(height + "px");
            LineWidget lw = plw.get().getLineWidget();
            lw.setPixelHeight(height);
            display.onLineWidgetChanged(lw);
         }
      };
      
      // construct our image
      String srcPath = imgSrcPathFromHref(sentinel, href);
      final Image image = new Image(srcPath);
      image.addStyleName(RES.styles().image());
      
      // parse and inject attributes
      Map<String, String> parsedAttributes = HTMLAttributesParser.parseAttributes(attributes);
      final Element imgEl = image.getElement();
      for (Map.Entry<String, String> entry : parsedAttributes.entrySet())
      {
         String key = entry.getKey();
         String val = entry.getValue();
         if (StringUtil.isNullOrEmpty(key) || StringUtil.isNullOrEmpty(val))
            continue;
         imgEl.setAttribute(key, val);
      }
      
      // add load handlers to image
      DOM.sinkEvents(imgEl, Event.ONLOAD | Event.ONERROR);
      DOM.setEventListener(imgEl, new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONLOAD)
            {
               final ImageElementEx imgEl = image.getElement().cast();
               
               int minWidth = Math.min(imgEl.naturalWidth(), 100);
               int maxWidth = Math.min(imgEl.naturalWidth(), 650);
               
               Style style = imgEl.getStyle();
               
               boolean hasWidth =
                     imgEl.hasAttribute("width") ||
                     style.getProperty("width") != null;
               
               if (!hasWidth)
               {
                  style.setProperty("width", "100%");
                  style.setProperty("minWidth", minWidth + "px");
                  style.setProperty("maxWidth", maxWidth + "px");
               }
               
               // attach to container
               container.setWidget(image);
               
               // update widget
               int height = image.getOffsetHeight() + 10;
               onResize.execute(height);
            }
            else if (DOM.eventGetType(event) == Event.ONERROR)
            {
               container.setWidget(noImageLabel);
               onResize.execute(50);
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
      renderHandler.set(display.addRenderFinishedHandler(
            new RenderFinishedEvent.Handler()
      {
         private int width_;
         
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            int width = display.getBounds().getWidth();
            if (width == width_)
               return;
            
            width_ = width;
            renderTimer.schedule(100);
         }
      }));
      
      // initialize doc changed handler
      docChangedHandler.set(display.addDocumentChangedHandler(
            new DocumentChangedEvent.Handler()
      {
         private String href_ = href;
         private String attributes_ = StringUtil.notNull(attributes);
         
         private final Timer refreshImageTimer = new Timer()
         {
            @Override
            public void run()
            {
               // if the discovered href isn't an image link, just bail
               if (!ImagePreviewer.isImageHref(href_))
                  return;
               
               // set new src location (load handler will replace label as needed)
               container.setWidget(new SimplePanel());
               noImageLabel.setText("(No image at path " + href_ + ")");
               image.getElement().setAttribute("src", imgSrcPathFromHref(
                     sentinel, href_));
               
               // parse and inject attributes
               Map<String, String> parsedAttributes = HTMLAttributesParser.parseAttributes(attributes_);
               final Element imgEl = image.getElement();
               for (Map.Entry<String, String> entry : parsedAttributes.entrySet())
               {
                  String key = entry.getKey();
                  String val = entry.getValue();
                  if (StringUtil.isNullOrEmpty(key) || StringUtil.isNullOrEmpty(val))
                     continue;
                  imgEl.setAttribute(key, val);
               }
            }
         };
         
         private void onDocumentChangedImpl(DocumentChangedEvent event)
         {
            int row = plw.get().getRow();
            Range range = event.getEvent().getRange();
            if (range.getStart().getRow() <= row && row <= range.getEnd().getRow())
            {
               String line = display.getLine(row);
               if (ImagePreviewer.isStandaloneMarkdownLink(line))
               {
                  // check to see if the URL text has been updated
                  Token hrefToken = null;
                  JsArray<Token> tokens = display.getTokens(row);
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
                  
                  String attributes = "";
                  int startBraceIdx = line.indexOf("){");
                  int endBraceIdx   = line.lastIndexOf("}");
                  if (startBraceIdx != -1 &&
                        endBraceIdx != -1 &&
                        endBraceIdx > startBraceIdx)
                  {
                     attributes = line.substring(startBraceIdx + 2, endBraceIdx).trim();
                  }
                  
                  // if we have the same href as before, don't update
                  // (avoid flickering + re-requests of same URL)
                  if (hrefToken.getValue().equals(href_) && attributes.equals(attributes_))
                     return;
                  
                  // cache href and schedule refresh of image
                  href_ = hrefToken.getValue();
                  attributes_ = attributes;
                  
                  refreshImageTimer.schedule(700);
               }
               else
               {
                  onDetach.execute();
               }
            }
         }
         
         @Override
         public void onDocumentChanged(final DocumentChangedEvent event)
         {
            // ignore 'removeLines' events as they won't mutate the actual
            // line containing the markdown link
            String action = event.getEvent().getAction();
            if (action.equals("removeLines"))
               return;
            
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  onDocumentChangedImpl(event);
               }
            });
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
            sentinel.getId(),
            "md-image-preview-" + StringUtil.makeRandomId(8),
            RmdChunkOptions.create(),
            ChunkOutputWidget.EXPANDED,
            false,  // can close
            host,
            ChunkOutputSize.Bare));
      
      ChunkOutputWidget outputWidget = cow.get();
      outputWidget.setRootWidget(container);
      outputWidget.hideSatellitePopup();
      outputWidget.getElement().getStyle().setMarginTop(4, Unit.PX);

      plw.set(new PinnedLineWidget(LINE_WIDGET_TYPE, display, outputWidget, 
            position.getRow(), null, null));
   }
   
   private static boolean isStandaloneMarkdownLink(String line)
   {
      return line.matches("^\\s*!\\s*\\[[^\\]]*\\]\\s*\\([^)]*\\)\\s*(?:{.*)?$");
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

   private static String imgSrcPathFromHref(DocUpdateSentinel sentinel, 
                                            String href)
   {
      // return paths that have a custom / external protocol as-is
      Pattern reProtocol = Pattern.create("^\\w+://");
      if (reProtocol.test(href))
         return href;
      
      // make relative paths absolute
      String absPath = href;
      if (FilePathUtils.pathIsRelative(href))
      {
         String docPath = sentinel.getPath();
         absPath = FilePathUtils.dirFromFile(docPath) + "/" + absPath;
      }
      
      return "file_show?path=" + StringUtil.encodeURIComponent(absPath) + 
            "&id=" + IMAGE_ID++;
   }
   
   private static void onPreviewImage(DocDisplay display, 
                                      DocUpdateSentinel sentinel,
                                      UIPrefs prefs,
                                      String href,
                                      String attributes,
                                      Position position,
                                      Range tokenRange)
   {
      // check to see if we already have a popup showing for this image;
      // if we do then bail early
      String encoded = StringUtil.encodeURIComponent(href);
      Element el = Document.get().getElementById(encoded);
      if (el != null)
         return;
      
      String pref = prefs.showLatexPreviewOnCursorIdle().getValue();
      
      // skip if disabled entirely
      if (!sentinel.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, 
            pref != UIPrefsAccessor.LATEX_PREVIEW_SHOW_NEVER))
         return;
      
      // display stand-alone links as line widgets (if enabled)
      String line = display.getLine(position.getRow());
      if (isStandaloneMarkdownLink(line) && 
          sentinel.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, 
            prefs.showLatexPreviewOnCursorIdle().getValue() == 
                UIPrefsAccessor.LATEX_PREVIEW_SHOW_ALWAYS))
      {
         onPreviewImageLineWidget(display, sentinel,
               href, attributes, position, tokenRange);
         return;
      }
      
      // construct image el, place in popup, and show
      ImagePreviewPopup panel = new ImagePreviewPopup(display, tokenRange, 
            href, imgSrcPathFromHref(sentinel, href));
      panel.getElement().setId(encoded);
      
      ScreenCoordinates coordinates =
            display.documentPositionToScreenCoordinates(position);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
   }
   
   private final DocDisplay display_;
   private final DocUpdateSentinel sentinel_;
   private final UIPrefs prefs_;

   private static final String LINE_WIDGET_TYPE = "image-preview" ;
   private static int IMAGE_ID = 0;
   
   interface Styles extends CssResource
   {
      String container();
      String image();
   }
   
   interface Resources extends ClientBundle
   {
      @Source("ImagePreviewer.css")
      Styles styles();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   static { RES.styles().ensureInjected(); }
   
}
