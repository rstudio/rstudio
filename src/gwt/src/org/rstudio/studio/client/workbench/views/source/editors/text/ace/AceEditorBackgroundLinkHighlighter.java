/*
 * AceEditorBackgroundLinkHighlighter.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.MapUtil.ForEachCommand;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.MouseTracker;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Inject;

public class AceEditorBackgroundLinkHighlighter
      implements
            AceClickEvent.Handler,
            AttachEvent.Handler,
            CommandClickEvent.Handler,
            DocumentChangedEvent.Handler,
            EditorModeChangedEvent.Handler,
            MouseMoveHandler,
            MouseUpHandler
{
   interface Highlighter
   {
      void highlight(AceEditor editor, String line, int row);
   }
   
   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           FileTypeRegistry fileTypeRegistry,
                           EventBus events,
                           FilesServerOperations server,
                           MouseTracker mouseTracker)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      events_ = events;
      server_ = server;
      mouseTracker_ = mouseTracker;
   }
   
   public AceEditorBackgroundLinkHighlighter(AceEditor editor)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      editor_ = editor;
      activeMarkers_ = new SafeMap<Integer, List<MarkerRegistration>>();
      
      nextHighlightStart_ = 0;
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            int n = editor_.getCurrentLineCount();
            int startRow = nextHighlightStart_;
            int endRow   = Math.min(nextHighlightStart_ + N_HIGHLIGHT_ROWS, n);
            
            for (int row = startRow; row < endRow; row++)
               highlightRow(row);
            
            nextHighlightStart_ = endRow;
            if (endRow != n)
               timer_.schedule(5);
         }
      };
      
      
      highlighters_ = new ArrayList<Highlighter>();
      
      handlers_ = new ArrayList<HandlerRegistration>();
      handlers_.add(editor_.addAceClickHandler(this));
      handlers_.add(editor_.addAttachHandler(this));
      handlers_.add(editor_.addDocumentChangedHandler(this));
      handlers_.add(editor_.addEditorModeChangedHandler(this));
      handlers_.add(editor_.addMouseMoveHandler(this));
      handlers_.add(editor_.addMouseUpHandler(this));
      
      refreshHighlighters(editor_.getModeId());
   }
   
   private void refreshHighlighters(String mode)
   {
      clearAllMarkers();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            TextFileType fileType = editor_.getFileType();
            highlighters_.clear();
            highlighters_.add(webLinkHighlighter());
            if (fileType != null && (fileType.isMarkdown() || fileType.isRmd()))
               highlighters_.add(markdownLinkHighlighter());
            nextHighlightStart_ = 0;
            timer_.schedule(700);
         }
      });
   }
   
   private void highlightRow(int row)
   {
      for (Highlighter highlighter : highlighters_)
         highlighter.highlight(editor_, editor_.getLine(row), row);
   }
   
   private void registerActiveMarker(int row,
                                     String id,
                                     int markerId,
                                     final AnchoredRange range)
   {
      if (!activeMarkers_.containsKey(row))
         activeMarkers_.put(row, new ArrayList<MarkerRegistration>());
      List<MarkerRegistration> markers = activeMarkers_.get(row);
      
      // if we're adding a marker that subsumes an old one, clear the old marker
      List<MarkerRegistration> filtered = ListUtil.filter(markers, new FilterPredicate<MarkerRegistration>()
      {
         @Override
         public boolean test(MarkerRegistration marker)
         {
            if (range.intersects(marker.getRange()))
            {
               marker.detach();
               return false;
            }
            return true;
         }
      });
      
      // add our new marker
      filtered.add(new MarkerRegistration(id, markerId, range));
      activeMarkers_.put(row, filtered);
   }
   
   private boolean isRequiredClickModifier(int modifier)
   {
      return BrowseCap.isMacintosh()
            ? modifier == KeyboardShortcut.META
            : modifier == KeyboardShortcut.SHIFT;
   }
   
   private boolean isRequiredClickModifier(NativeEvent event)
   {
      return isRequiredClickModifier(KeyboardShortcut.getModifierValue(event));
   }
   
   private MarkerRegistration getTargetedMarker(NativeEvent event)
   {
      int pageX = event.getClientX();
      int pageY = event.getClientY();
      return getTargetedMarker(pageX, pageY);
   }
   
   private MarkerRegistration getTargetedMarker(int pageX, int pageY)
   {
      Position position = editor_.screenCoordinatesToDocumentPosition(pageX, pageY);
      int row = position.getRow();
      if (!activeMarkers_.containsKey(row))
         return null;
      
      List<MarkerRegistration> markers = activeMarkers_.get(row);
      for (MarkerRegistration marker : markers)
         if (marker.getRange().contains(position))
            return marker;
      
      return null;
   }
   
   private void beginDetectClickTarget(int pageX, int pageY, int modifier)
   {
      if (!isRequiredClickModifier(modifier))
         return;
      
      MarkerRegistration activeMarker = getTargetedMarker(pageX, pageY);
      if (activeMarker == null)
         return;
      
      Element el = DomUtils.elementFromPoint(pageX, pageY);
      if (el == null)
         return;
      
      // the element might itself be the marker we want to update, or
      // it may be the editor instance. handle each case
      String id = activeMarker.getId();
      Element markerEl = el.hasClassName(id)
            ? el
            : DomUtils.getFirstElementWithClassName(el, id);
      if (markerEl == null)
         return;
      
      if (activeHighlightMarkerEl_ != null && activeHighlightMarkerEl_ != markerEl)
      {
         activeHighlightMarkerEl_.addClassName(RES.styles().highlight());
         activeHighlightMarkerEl_.removeClassName(RES.styles().hover());
      }
      
      markerEl.removeClassName(RES.styles().highlight());
      markerEl.addClassName(RES.styles().hover());
      activeHighlightMarkerEl_ = markerEl;
   }
   
   private void endDetectClickTarget()
   {
      if (activeHighlightMarkerEl_ == null)
         return;
      
      // restore highlight styles
      activeHighlightMarkerEl_.addClassName(RES.styles().highlight());
      activeHighlightMarkerEl_.removeClassName(RES.styles().hover());
      
      // unset active el
      activeHighlightMarkerEl_ = null;
   }
   
   private void clearAllMarkers()
   {
      MapUtil.forEach(activeMarkers_, new ForEachCommand<Integer, List<MarkerRegistration>>()
      {
         @Override
         public void execute(Integer row, List<MarkerRegistration> markers)
         {
            for (MarkerRegistration marker : markers)
               marker.detach();
         }
      });
      activeMarkers_.clear();
   }
   
   private void clearMarkers(final Range range)
   {
      for (int row = range.getStart().getRow();
           row <= range.getEnd().getRow();
           row++)
      {
         if (!activeMarkers_.containsKey(row))
            continue;
         
         // clear markers that are included within this range
         List<MarkerRegistration> markers = activeMarkers_.get(row);
         List<MarkerRegistration> filtered = ListUtil.filter(markers, new FilterPredicate<MarkerRegistration>()
         {
            @Override
            public boolean test(MarkerRegistration marker)
            {
               if (range.contains(marker.getRange()))
               {
                  marker.detach();
                  return false;
               }
               
               return true;
            }
         });
         
         // update active markers for this row
         activeMarkers_.put(row, filtered);
      }
   }
   
   private void navigateToUrl(String url)
   {
      // allow web links starting with 'www'
      if (url.startsWith("www."))
         url = "http://" + url;
      
      // attempt to open web links in a new window
      Pattern reWebLink = Pattern.create("^https?://");
      if (reWebLink.test(url))
      {
         globalDisplay_.openWindow(url);
         return;
      }
      
      // handle testthat links
      Pattern reSrcRef = Pattern.create("@[^#]+#\\d+");
      if (reSrcRef.test(url))
         return;
      
      // treat other URLs as paths to files on the server
      final String finalUrl = url;
      server_.stat(finalUrl, new ServerRequestCallback<FileSystemItem>()
      {
         @Override
         public void onResponseReceived(FileSystemItem file)
         {
            // inform user when no file found
            if (file == null || !file.exists())
            {
               String message = "No file at path '" + finalUrl + "'.";
               String caption = "Error navigating to file";
               globalDisplay_.showErrorMessage(caption, message);
               return;
            }
            
            // if we have a registered filetype for this file, try
            // to open it in the IDE; otherwise open in browser
            FileType fileType = fileTypeRegistry_.getTypeForFile(file);
            if (fileType != null && fileType instanceof EditableFileType)
            {
               fileType.openFile(file, null, NavigationMethods.DEFAULT, events_);
            }
            else
            {
               events_.fireEvent(new OpenFileInBrowserEvent(file));
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   
   // Highlighter Implementations ----
   
   private void highlight(final AceEditor editor,
                          int row,
                          int startColumn,
                          int endColumn)
   {
      // check to see if we already have a marker for this range
      Position start = Position.create(row, startColumn);
      Position end   = Position.create(row, endColumn);
      Range range = Range.fromPoints(start, end);
      if (activeMarkers_.containsKey(row))
      {
         List<MarkerRegistration> markers = activeMarkers_.get(row);
         for (MarkerRegistration marker : markers)
         {
            if (marker.getRange().isEqualTo(range))
               return;
         }
      }

      // create an anchored range and add a marker for it
      final String id = "ace_marker-" + StringUtil.makeRandomId(16);
      final String styles = RES.styles().highlight() + " ace_marker " + id;
      AnchoredRange anchoredRange = editor.getSession().createAnchoredRange(start, end, true);
      
      final String title = BrowseCap.isMacintosh()
            ? "Open Link (Command+Click)"
            : "Open Link (Shift+Click)";
      MarkerRenderer renderer =
            MarkerRenderer.create(editor.getWidget().getEditor(), styles, title);
      
      int markerId = editor.getSession().addMarker(anchoredRange, styles, renderer, true);
      registerActiveMarker(row, id, markerId, anchoredRange);
   }
   
   private Highlighter webLinkHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(AceEditor editor, String line, int row)
         {
            onWebLinkHighlight(editor, line, row);
         }
      };
   }
   
   private void onWebLinkHighlight(AceEditor editor, String line, int row)
   {
      // use a regex that captures all non-space characters within
      // a web link, and then fix up the captured link by removing
      // trailing punctuation, etc. as required
      Pattern reWebLink = createWebLinkPattern();
      for (Match match = reWebLink.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         // compute start, end index for discovered URL
         int startIdx = match.getIndex();
         int endIdx   = match.getIndex() + match.getValue().length();
         
         // ensure that the discovered url is not within a string
         Token token = editor_.getTokenAt(Position.create(row, startIdx));
         if (token.hasType("string"))
            continue;
         
         String url = match.getValue();
         
         // trim off enclosing brackets
         if (!url.matches(reWebLink()))
         {
            startIdx++;
            endIdx--;
            url = url.substring(1, url.length() - 1);
         }
         
         // trim off trailing punctuation (characters unlikely
         // to be found at the end of a url)
         String trimmed = url.replaceAll("[,.?!@#$%^&*;:-]+$", "");
         endIdx -= (url.length() - trimmed.length());
         url = trimmed;
         
         // perform highlighting
         highlight(editor, row, startIdx, endIdx);
      }
   }
   
   private static String reWebLink()
   {
      return "(?:\\w+://|www\\.)\\S+";
   }
   
   private static Pattern createWebLinkPattern()
   {
      String rePattern = StringUtil.join(new String[] {
            "\\{" + reWebLink() + "?\\}",
            "\\(" + reWebLink() + "?\\)",
            "\\[" + reWebLink() + "?\\]",
            "\\<" + reWebLink() + "?\\>",
            "'"   + reWebLink() + "'",
            "\""  + reWebLink() + "\"",
            reWebLink()
      }, "|");
      return Pattern.create(rePattern);
   }
   
   private Highlighter markdownLinkHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(AceEditor editor, String line, int row)
         {
            onMarkdownLinkHighlight(editor, line, row);
         }
      };
   }
   
   private void onMarkdownLinkHighlight(AceEditor editor,
                                        String line,
                                        int row)
   {
      Pattern reMarkdownLink = Pattern.create("(\\[[^\\]]+\\])(\\([^\\)]+\\))");
      for (Match match = reMarkdownLink.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex() + match.getGroup(1).length() + 1;
         int endIdx   = match.getIndex() + match.getValue().length() - 1;
         highlight(editor, row, startIdx, endIdx);
      }
   }
   
   @SuppressWarnings("unused")
   private Highlighter testthatErrorHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(AceEditor editor, String line, int row)
         {
            onTestthatErrorHighlight(editor, line, row);
         }
      };
   }
   
   private void onTestthatErrorHighlight(AceEditor editor, String line, int row)
   {
      Pattern reTestthatError = Pattern.create("\\(@[^#]+#\\d+\\)");
      for (Match match = reTestthatError.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex() + 1;
         int endIdx   = match.getIndex() + match.getValue().length() - 1;
         highlight(editor, row, startIdx, endIdx);
      }
   }
   
   // Event Handlers ---
   
   @Override
   public void onAttachOrDetach(AttachEvent event)
   {
      if (event.isAttached())
      {
         previewHandler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
         {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent preview)
            {
               int type = preview.getTypeInt();
               if (type == Event.ONKEYDOWN)
               {
                  int modifier = KeyboardShortcut.getModifierValue(preview.getNativeEvent());
                  beginDetectClickTarget(mouseTracker_.getLastMouseX(), mouseTracker_.getLastMouseY(), modifier);
               }
               else if (type == Event.ONKEYUP)
               {
                  endDetectClickTarget();
               }
            }
         });
      }
      else
      {
         for (HandlerRegistration handler : handlers_)
            handler.removeHandler();
         handlers_.clear();
         
         if (previewHandler_ != null)
         {
            previewHandler_.removeHandler();
            previewHandler_ = null;
         }
      }
   }
   
   @Override
   public void onDocumentChanged(DocumentChangedEvent event)
   {
      // clear markers within the delete range
      clearMarkers(event.getEvent().getRange());
      
      // prepare highlighter
      int row = event.getEvent().getRange().getStart().getRow();
      nextHighlightStart_ = Math.min(nextHighlightStart_, row);
      timer_.schedule(700);
      
      // update marker positions (deferred so that anchors update)
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            final SafeMap<Integer, List<MarkerRegistration>> newMarkers =
                  new SafeMap<Integer, List<MarkerRegistration>>();
            MapUtil.forEach(activeMarkers_, new ForEachCommand<Integer, List<MarkerRegistration>>()
            {
               @Override
               public void execute(Integer oldRow, List<MarkerRegistration> markers)
               {
                  if (markers == null || markers.isEmpty())
                     return;

                  // all markers here should have same row
                  int newRow = markers.get(0).getRange().getStart().getRow();
                  newMarkers.put(newRow, markers);
               }
            });
            activeMarkers_.clear();
            activeMarkers_ = newMarkers;
         }
      });
   }
   
   @Override
   public void onEditorModeChanged(EditorModeChangedEvent event)
   {
      refreshHighlighters(event.getMode());
   }
   
   @Override
   public void onCommandClick(CommandClickEvent event)
   {
      Position position = event.getEvent().getDocumentPosition();
      int row = position.getRow();
      if (!activeMarkers_.containsKey(row))
         return;
      
      List<MarkerRegistration> markers = activeMarkers_.get(row);
      for (MarkerRegistration registration : markers)
      {
         if (registration.getRange().contains(position))
         {
            endDetectClickTarget();
            String url = editor_.getTextForRange(registration.getRange());
            navigateToUrl(url);
            return;
         }
      }
   }
   
   @Override
   public void onAceClick(AceClickEvent clickEvent)
   {
      NativeEvent event = clickEvent.getNativeEvent();
      if (!isRequiredClickModifier(event))
         return;
      
      MarkerRegistration marker = getTargetedMarker(event);
      if (marker == null)
         return;
      
      clickEvent.stopPropagation();
      clickEvent.preventDefault();
      
      // on OS X, we immediately open the popup as otherwise the link
      // will be opened in the background
      if (BrowseCap.isMacintosh() && !BrowseCap.isMacintoshDesktop())
      {
         endDetectClickTarget();
         String url = editor_.getTextForRange(marker.getRange());
         navigateToUrl(url);
      }
   }
   
   @Override
   public void onMouseUp(MouseUpEvent mouseUpEvent)
   {
      // clicks handled in 'onAceClick' for OS X web mode
      if (BrowseCap.isMacintosh() && !BrowseCap.isMacintoshDesktop())
         return;
      
      NativeEvent event = mouseUpEvent.getNativeEvent();
      if (!isRequiredClickModifier(event))
         return;
      
      MarkerRegistration marker = getTargetedMarker(event);
      if (marker == null)
         return;
      
      boolean hasMouseMoved =
            Math.abs(event.getClientX() - mouseTracker_.getLastMouseX()) >= 2 ||
            Math.abs(event.getClientY() - mouseTracker_.getLastMouseY()) >= 2;
      
      if (hasMouseMoved)
         return;
      
      event.stopPropagation();
      event.preventDefault();
      
      endDetectClickTarget();
      String url = editor_.getTextForRange(marker.getRange());
      navigateToUrl(url);
   }
   
   @Override
   public void onMouseMove(MouseMoveEvent event)
   {
      beginDetectClickTarget(
            event.getClientX(),
            event.getClientY(),
            KeyboardShortcut.getModifierValue(event.getNativeEvent()));
   }
   
   // Resources ----
   
   interface Resources extends ClientBundle
   {
      @Source("AceEditorBackgroundLinkHighlighter.css")
      Styles styles();
   }
   
   interface Styles extends CssResource
   {
      String highlight();
      String hover();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   // Private Members ----
   
   private static class MarkerRenderer extends JavaScriptObject
   {
      protected MarkerRenderer() {}
      
      public static final native MarkerRenderer create(final AceEditorNative editor,
                                                       final String clazz,
                                                       final String title)
      /*-{
         var markerBack = editor.renderer.$markerBack;
         return $entry(function(html, range, left, top, config) {
            // HACK: we take advantage of an implementation detail of
            // Ace's 'drawTextMarker' implementation. Ace constructs
            // HTML for the generated markers with code of the form:
            //
            //    html = "<div style='..." + extraStyle + "'>"
            //
            // We take advantage of this, and inject our 'extraStyle'
            // to close the style attribute we were intended to be
            // locked in, and instead inject a 'title' attribute instead.
            var extra = "' title='" + title;
            if (range.isMultiLine())
               return markerBack.drawTextMarker(html, range, clazz, config, extra);
            else
               return markerBack.drawSingleLineMarker(html, range, clazz, config, 0, extra);
         });
      }-*/;
   }
   
   private class MarkerRegistration
   {
      public MarkerRegistration(String id, int markerId, AnchoredRange range)
      {
         id_ = id;
         markerId_ = markerId;
         range_ = range;
      }
      
      public void detach()
      {
         editor_.getSession().removeMarker(getMarkerId());
         range_.detach();
      }
      
      public String getId()
      {
         return id_;
      }
      
      public int getMarkerId()
      {
         return markerId_;
      }
      
      public AnchoredRange getRange()
      {
         return range_;
      }
      
      private final String id_;
      private final int markerId_;
      private final AnchoredRange range_;
   }
   
   private final AceEditor editor_;
   private final List<Highlighter> highlighters_;
   private final Timer timer_;
   private final List<HandlerRegistration> handlers_;
   
   private SafeMap<Integer, List<MarkerRegistration>> activeMarkers_;
   private int nextHighlightStart_;
   private static final int N_HIGHLIGHT_ROWS = 200;
   
   private HandlerRegistration previewHandler_;
   private Element activeHighlightMarkerEl_;
   
   // Injected ----
   private GlobalDisplay globalDisplay_;
   private FileTypeRegistry fileTypeRegistry_;
   private EventBus events_;
   private FilesServerOperations server_;
   private MouseTracker mouseTracker_;
}
