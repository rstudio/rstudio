/*
 * DocDisplayIdleCommands.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplayIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplayIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DocDisplayIdleCommands
{
   public DocDisplayIdleCommands()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      PREVIEW_LINK = previewLink();
   }
   
   // Private Methods ----
   
   @Inject
   private void initialize(FilesServerOperations filesServer)
   {
      filesServer_ = filesServer;
   }
   
   private IdleCommand previewLink()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(DocDisplay docDisplay, IdleState idleState)
         {
            onPreviewLink(docDisplay, idleState);
         }
      };
   }
   
   private void onPreviewLink(DocDisplay docDisplay, IdleState idleState)
   {
      Position position = resolvePosition(docDisplay, idleState);
      Token token = docDisplay.getTokenAt(position);
      if (token == null)
         return;

      if (!token.hasAllTypes("href"))
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
         onPreviewImage(docDisplay, href, position, tokenRange);
      }
   }
   
   private void onPreviewImage(DocDisplay docDisplay,
                               String href,
                               Position position,
                               Range tokenRange)
   {
      // for local images, construct a path that the server URI redirects
      // will handle properly
      String srcPath = href;
      if (!href.startsWith("http"))
      {
         srcPath = href.startsWith("~/")
               ? "files/" + href.substring(2)
               : "file_show?path=" + StringUtil.encodeURIComponent(href);
      }
      
      // check to see if we already have a popup showing for this image;
      // if we do then bail early
      String encoded = StringUtil.encodeURIComponent(srcPath);
      Element el = Document.get().getElementById(encoded);
      if (el != null)
         return;
      
      // construct image el, place in popup, and show
      ImageElement imgEl = Document.get().createImageElement();
      imgEl.setAttribute("src", srcPath);
      imgEl.getStyle().setProperty("maxWidth", "100px");
      
      ScreenCoordinates coordinates =
            docDisplay.documentPositionToScreenCoordinates(position);
      
      AnchoredPopupPanel panel = new AnchoredPopupPanel(docDisplay, tokenRange);
      panel.getElement().setId(encoded);
      panel.getElement().appendChild(imgEl);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
   }
   
   private static Position resolvePosition(DocDisplay docDisplay, IdleState state)
   {
      int type = state.getType();
      if (type == IdleState.STATE_CURSOR_IDLE)
         return docDisplay.getCursorPosition();
      else if (type == IdleState.STATE_MOUSE_IDLE)
         return docDisplay.screenCoordinatesToDocumentPosition(state.getMouseX(), state.getMouseY());
      
      assert false : "Unhandled idle state type '" + type + "'";
      return Position.create(0, 0);
   }
   
   public final IdleCommand PREVIEW_LINK;
   
   private static class AnchoredPopupPanel extends MiniPopupPanel
   {
      public AnchoredPopupPanel(DocDisplay docDisplay, Range range)
      {
         super(true, false);
         
         anchor_ = docDisplay.createAnchoredSelection(
               range.getStart(),
               range.getEnd());
         
         handler_ = docDisplay.addCursorChangedHandler(new CursorChangedHandler()
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
      
      private void detachHandlers()
      {
         anchor_.detach();
         handler_.removeHandler();
      }
      
      private final AnchoredSelection anchor_;
      private final HandlerRegistration handler_;
   }
   
   // Injected ----
   private FilesServerOperations filesServer_;
}
