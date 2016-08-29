/*
 * AceEditorBackgroundLinkHighlighter.java
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

import java.util.ArrayList;
import java.util.List;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class AceEditorBackgroundLinkHighlighter
   implements DocumentChangedEvent.Handler,
              CommandClickEvent.Handler
{
   interface Highlighter
   {
      void highlight(AceEditor editor, String line, int row);
   }
   
   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           FileTypeRegistry fileTypeRegistry,
                           EventBus events,
                           FilesServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      events_ = events;
      server_ = server;
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
      highlighters_.add(webLinkHighlighter());
      
      editor_.addDocumentChangedHandler(this);
      editor_.addCommandClickHandler(this);
   }
   
   private void highlightRow(int row)
   {
      for (Highlighter highlighter : highlighters_)
         highlighter.highlight(editor_, editor_.getLine(row), row);
   }
   
   private void registerActiveMarker(int row, int markerId, final Range range)
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
               editor_.getSession().removeMarker(marker.getId());
               return false;
            }
            return true;
         }
      });
      
      // add our new marker
      filtered.add(new MarkerRegistration(markerId, range));
      activeMarkers_.put(row, filtered);
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
                  editor_.getSession().removeMarker(marker.getId());
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
      
      // treat other URLs as paths to files on the server
      final String finalUrl = url;
      server_.stat(finalUrl, new ServerRequestCallback<FileSystemItem>()
      {
         @Override
         public void onResponseReceived(FileSystemItem file)
         {
            // inform user non-obtrusively if no file found
            if (file == null || !file.exists())
            {
               globalDisplay_.showWarningBar(false, "No file at path '" + finalUrl + "'.");
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
      Pattern reWebLink = Pattern.create("(?:https?://|www.)\\S+");
      for (Match match = reWebLink.match(line, 0);
           match != null;
           match = match.nextMatch())
      {
         int startIdx = match.getIndex();
         int endIdx   = match.getIndex() + match.getValue().length();
         
         // trim off trailing punctuation (characters unlikely
         // to be found at the end of a url)
         String url = match.getValue();
         String trimmed = url.replaceAll("[,.?!@#$%^&*;:-]+$", "");
         endIdx -= (url.length() - trimmed.length());
         url = trimmed;
         
         // attempt to trim off enclosing quotes, etc
         int trimLeftCount = 0;
         for (int index = startIdx - 1; index >= 0; index--)
         {
            char beforeStart = line.charAt(index);
            boolean needsTrim =
                  beforeStart == '('  && url.endsWith(")")  ||
                  beforeStart == '['  && url.endsWith("]")  ||
                  beforeStart == '{'  && url.endsWith("}")  ||
                  beforeStart == '['  && url.endsWith("]")  ||
                  beforeStart == '<'  && url.endsWith(">")  ||
                  beforeStart == '"'  && url.endsWith("\"") ||
                  beforeStart == '\'' && url.endsWith("'");

            if (needsTrim)
            {
               url = url.substring(0, url.length() - 1);
               trimLeftCount++;
            }
         }
         
         // apply trimming
         endIdx -= trimLeftCount;
         
         // check to see if we already have a marker for this range
         Position start = Position.create(row, startIdx);
         Position end = Position.create(row, endIdx);
         Range range = Range.fromPoints(start, end);
         if (activeMarkers_.containsKey(row))
         {
            List<MarkerRegistration> markers = activeMarkers_.get(row);
            for (MarkerRegistration marker : markers)
            {
               if (marker.getRange().isEqualTo(range))
                  continue;
            }
         }
         
         // create an anchored range and add a marker for it
         AnchoredRange anchoredRange = editor.getSession().createAnchoredRange(start, end, true);
         int markerId = editor.getSession().addMarker(anchoredRange, RES.styles().highlight(), "text", false);
         registerActiveMarker(row, markerId, anchoredRange);
      }
   }
   
   // Event Handlers ---
   
   @Override
   public void onDocumentChanged(DocumentChangedEvent event)
   {
      // clear markers within the delete range
      clearMarkers(event.getEvent().getRange());
      
      // prepare highlighter
      int row = event.getEvent().getRange().getStart().getRow();
      nextHighlightStart_ = Math.min(nextHighlightStart_, row);
      timer_.schedule(700);
   }
   
   @Override
   public void onCommandClick(CommandClickEvent event)
   {
      Position position = event.getDocumentPosition();
      int row = position.getRow();
      if (!activeMarkers_.containsKey(row))
         return;
      
      List<MarkerRegistration> markers = activeMarkers_.get(row);
      for (MarkerRegistration registration : markers)
      {
         if (registration.getRange().contains(position))
         {
            String url = editor_.getTextForRange(registration.getRange());
            navigateToUrl(url);
            return;
         }
      }
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
   }
   
   public static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   // Private Members ----
   
   private static class MarkerRegistration
   {
      public MarkerRegistration(int id, Range range)
      {
         id_ = id;
         range_ = range;
      }
      
      public int getId()
      {
         return id_;
      }
      
      public Range getRange()
      {
         return range_;
      }
      
      private final int id_;
      private final Range range_;
   }
   
   private final AceEditor editor_;
   private final List<Highlighter> highlighters_;
   private final SafeMap<Integer, List<MarkerRegistration>> activeMarkers_;
   private final Timer timer_;
   
   private int nextHighlightStart_;
   private static final int N_HIGHLIGHT_ROWS = 200;
   
   // Injected ----
   private GlobalDisplay globalDisplay_;
   private FileTypeRegistry fileTypeRegistry_;
   private EventBus events_;
   private FilesServerOperations server_;
}
