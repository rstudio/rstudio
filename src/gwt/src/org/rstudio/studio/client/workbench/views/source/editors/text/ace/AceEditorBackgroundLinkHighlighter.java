/*
 * AceEditorBackgroundLinkHighlighter.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
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
            AttachEvent.Handler,
            CommandClickEvent.Handler,
            DocumentChangedEvent.Handler,
            EditorModeChangedEvent.Handler,
            MouseMoveHandler
{
   interface Highlighter
   {
      void highlight(String line, int row);
   }

   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           FileTypeRegistry fileTypeRegistry,
                           EventBus events,
                           FilesServerOperations server,
                           UserPrefs userPrefs)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      events_ = events;
      server_ = server;
      userPrefs_ = userPrefs;
   }

   public AceEditorBackgroundLinkHighlighter(AceEditor editor)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      editor_ = editor;
      activeMarkers_ = new SafeMap<>();

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


      highlighters_ = new ArrayList<>();
      
      handlers_ = new ArrayList<>();
      handlers_.add(editor_.addAttachHandler(this));
      handlers_.add(editor_.addDocumentChangedHandler(this));
      handlers_.add(editor_.addEditorModeChangedHandler(this));
      handlers_.add(editor_.addMouseMoveHandler(this));
      handlers_.add(editor_.addCommandClickHandler(this));
      
      userPrefs_.highlightWebLink().bind((Boolean enabled) ->
      {
         if (editor_ != null)
            refreshHighlighters(editor_.getModeId());
      });

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
            
            if (userPrefs_.highlightWebLink().getGlobalValue())
               highlighters_.add(webLinkHighlighter());
            
            if (fileType != null && fileType.isR())
               highlighters_.add(issueHighlighter());
            if (fileType != null && (fileType.isMarkdown() || fileType.isRmd())) 
            {
               highlighters_.add(issueHighlighter());
               highlighters_.add(markdownLinkHighlighter());
            }
            
            nextHighlightStart_ = 0;
            timer_.schedule(100);
         }
      });
   }

   private void highlightRow(int row)
   {
      String line = editor_.getLine(row);
      for (Highlighter highlighter : highlighters_)
         highlighter.highlight(line, row);
   }

   private void registerActiveMarker(int row,
                                     int markerId,
                                     final AnchoredRange range)
   {
      if (!activeMarkers_.containsKey(row))
         activeMarkers_.put(row, new ArrayList<>());
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
      filtered.add(new MarkerRegistration(markerId, range));
      activeMarkers_.put(row, filtered);
   }

   private boolean isRequiredClickModifier(int modifier)
   {
      return BrowseCap.isMacintosh()
            ? modifier == KeyboardShortcut.META
            : modifier == KeyboardShortcut.SHIFT;
   }

   private void beginDetectClickTarget(int modifier)
   {
      if (isRequiredClickModifier(modifier))
         editor_.getWidget().getElement().addClassName(RES.styles().modified()); 
      else 
         endDetectClickTarget();
   }

   private void endDetectClickTarget()
   {
      editor_.getWidget().getElement().removeClassName(RES.styles().modified()); 
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

      final String finalUrl = url;
      
      // issue:
      // 
      // - #123 
      //
      // - tidyverse/dplyr#123
      // - github::tidyverse/dplyr#123
      // 
      // - gitlab::jimhester/covr#214
      Match match = ISSUE_LINK_PATTERN.match(url, 0);
      if (match != null)
      {
         String remote = match.getGroup(1);
         String orgRepo = match.getGroup(2);
         String issue = match.getGroup(3);

         // special case when simple issue: #123
         // because this needs to query BugReports
         if (remote == null && orgRepo == null)
         {
            server_.getIssueUrl(url, new SimpleRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String response)
               {
                  if (response.length() > 0)
                     globalDisplay_.openWindow(response); 
                  else 
                     RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                        constants_.couldNotResolveIssue(finalUrl));
               }
            });
            return;
         }

         if (remote == null || StringUtil.equals(remote, "github::"))
         {
            globalDisplay_.openWindow("https://www.github.com/" + orgRepo + "/issues/" + issue);
         } 
         else if (StringUtil.equals(remote, "gitlab::"))
         {
            globalDisplay_.openWindow("https://www.gitlab.com/" + orgRepo + "/issues/" + issue);
         }
         
         return;
      }

      // treat other URLs as paths to files on the server
      server_.stat(finalUrl, new ServerRequestCallback<FileSystemItem>()
      {
         @Override
         public void onResponseReceived(FileSystemItem file)
         {
            // inform user when no file found
            if (file == null || !file.exists())
            {
               String message = constants_.noFileAtPath(finalUrl);
               String caption = constants_.errorNavigatingToFile();
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

   private void highlight(int row,
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
      final String styles = RES.styles().highlight() + " ace_marker ";
      AnchoredRange anchoredRange = editor_.getSession().createAnchoredRange(start, end, true);

      final String title = BrowseCap.isMacintosh()
            ? constants_.openLinkMacCommand()
            : constants_.openLinkNotMacCommand();
      MarkerRenderer renderer =
             MarkerRenderer.create(editor_.getWidget().getEditor(), styles, title);
      int markerId = editor_.getSession().addMarker(anchoredRange, styles, renderer, false);

      registerActiveMarker(row, markerId, anchoredRange);
   }

   // Highlighter Implementations ----

   private Highlighter webLinkHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(String line, int row)
         {
            onWebLinkHighlight(line, row);
         }
      };
   }

   private void onWebLinkHighlight(String line, int row)
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
         highlight(row, startIdx, endIdx);
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

   private Highlighter issueHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(String line, int row)
         {
            onIssueHighlight(line, row);
         }
      };
   }

   private void onIssueHighlight(String line, int row)
   {
      for (Match match = ISSUE_LINK_PATTERN.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex();
         int endIdx   = match.getIndex() + match.getValue().length();
         
         // Bail if the issue number is immediately followed by a letter
         if (line.length() >= endIdx) {
            char c = line.charAt(endIdx);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
               continue;
         }
         
         if (startIdx > 0 && endIdx < line.length())
         {
            char before = line.charAt(startIdx - 1);
            char after  = line.charAt(endIdx);
         
            // bail if the match is surrounded by quotes
            // because this may be an hex color
            if ( (before == '\'' || before == '"') && after == before)
               continue;
         }
         highlight(row, startIdx, endIdx);
      }
   }

   private Highlighter markdownLinkHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(String line, int row)
         {
            onMarkdownLinkHighlight(line, row);
         }
      };
   }

   private void onMarkdownLinkHighlight(String line,
                                        int row)
   {
      Pattern reMarkdownLink = Pattern.create("(\\[[^\\]]+\\])(\\([^\\)]+\\))");
      for (Match match = reMarkdownLink.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex() + match.getGroup(1).length() + 1;
         int endIdx   = match.getIndex() + match.getValue().length() - 1;
         highlight(row, startIdx, endIdx);
      }
   }

   @SuppressWarnings("unused")
   private Highlighter testthatErrorHighlighter()
   {
      return new Highlighter()
      {
         @Override
         public void highlight(String line, int row)
         {
            onTestthatErrorHighlight(line, row);
         }
      };
   }

   private void onTestthatErrorHighlight(String line, int row)
   {
      Pattern reTestthatError = Pattern.create("\\(@[^#]+#\\d+\\)");
      for (Match match = reTestthatError.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex() + 1;
         int endIdx   = match.getIndex() + match.getValue().length() - 1;
         highlight(row, startIdx, endIdx);
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
                  beginDetectClickTarget(modifier);
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
            final SafeMap<Integer, List<MarkerRegistration>> newMarkers = new SafeMap<>();
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
   public void onMouseMove(MouseMoveEvent event)
   {
      int modifier = KeyboardShortcut.getModifierValue(event.getNativeEvent());
      beginDetectClickTarget(modifier);
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
      String modified();
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
            markerBack.drawSingleLineMarker(html, range, clazz, config, 0);
            
            // HACK: after the marker is drawn, retrieve it based on markerBack.i
            //       and squeeze in a title attribute
            var x;
            var i = markerBack.i;
            var markers = markerBack.element.childNodes;
            if (i == -1) {
               x = markers[markers.length - 1];
            } else {
               x = markers[i - 1];
            }

            x.setAttribute("title", title);
         });
      }-*/;
   }
   private class MarkerRegistration
   {
      public MarkerRegistration(int markerId, AnchoredRange range)
      {
         markerId_ = markerId;
         range_ = range;
      }

      public void detach()
      {
         editor_.getSession().removeMarker(getMarkerId());
         range_.detach();
      }

      public int getMarkerId()
      {
         return markerId_;
      }

      public AnchoredRange getRange()
      {
         return range_;
      }

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
   
   // Injected ----
   private GlobalDisplay globalDisplay_;
   private FileTypeRegistry fileTypeRegistry_;
   private EventBus events_;
   private FilesServerOperations server_;
   private UserPrefs userPrefs_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);

   // constants
   private static final Pattern ISSUE_LINK_PATTERN = Pattern.create("((?:github|gitlab)::)?([-a-zA-Z0-9.]+/[-a-zA-Z0-9.]+)?#([0-9]+)");
}
