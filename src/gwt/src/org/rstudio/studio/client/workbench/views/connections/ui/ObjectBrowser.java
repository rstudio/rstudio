/*
 * ObjectBrowser.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.CellTree.CellTreeMessages;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ObjectBrowser extends Composite implements RequiresResize
{
   public ObjectBrowser()
   {
      hostPanel_ = new SimplePanelWithProgress();
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.setSize("100%", "100%");
      hostPanel_.setWidget(scrollPanel_);
      hostPanel_.setSize("100%", "100%");
      connection_ = null;
       
      // init widget
      initWidget(hostPanel_);
   }
  
   public void clear()
   {
      if (objectsModel_ != null)
         objectsModel_.clear();
      connection_ = null;
      objectsModel_ = null;
   }
   
   public void update(Connection connection, String hint)
   { 
      // create tables model and widget
      objectsModel_ = new ObjectBrowserModel();
      
      // show progress while updating the connection
      hostPanel_.showProgress(50, "Loading objects");
            
      // update the table then restore expanded nodes
      objectsModel_.update(
         connection ,      // connection 
         null,             // expanded nodes (none for refresh)
         () -> 
         {
            // clear progress and show the object tree again
            hostPanel_.setWidget(scrollPanel_);
         }, null);

      // create new widget
      objects_ = new CellTree(objectsModel_, null, RES, MESSAGES, 512);
      
      // create the top level list of objects
      objects_.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
      objects_.setWidth("100%");
      
      // wrap in vertical panel to get correct scrollbar behavior
      objectsWrapper_ = new VerticalPanel();
      objectsWrapper_.setWidth("100%");
      objectsWrapper_.add(objects_);
      
      scrollPanel_.setWidget(objectsWrapper_);
      // cache connection
      connection_ = connection;
   }
   
   @Override
   public void onResize()
   {
   }
   
   @Override
   public void onAttach()
   {
      // this works around an issue in CellTree; it has a "Show more" link which
      // displays when there are more than defaultNodeSize items, but clicking
      // on it causes the hosting scroll panel to jump to the top. unfortunately
      // neither this link nor its activation is visible, so listen for clicks
      // on the link in the capture phase and scroll the user to the bottom when
      // expansion is complete.
      registration_ = Event.addNativePreviewHandler(event ->
      {
         // look only for click events
         if (event.getTypeInt() != Event.ONMOUSEDOWN)
            return;

         // look only for those that are targeted at the Show More button
         Element target = Element.as(event.getNativeEvent().getEventTarget());
         if (!StringUtil.equals(target.getTagName().toLowerCase(), "a"))
            return;
         if (!target.getInnerText().equals("Show more"))
            return;
         
         // if we got here, the user has clicked Show More, so scroll to the
         // bottom when they're done
         final Value<HandlerRegistration> registration = 
               new Value<HandlerRegistration>(null);
         registration.setValue(scrollPanel_.addScrollHandler(e -> 
         {
            scrollPanel_.scrollToBottom();
            registration.getValue().removeHandler();
         }));
      });
      super.onAttach();
   }
   
   @Override
   public void onDetach()
   {
      registration_.removeHandler();
      super.onDetach();
   }
   
   public interface Resources extends CellTree.Resources {
      
      @Source("zoomDataset_2x.png")
      ImageResource zoomDataset2x();
      
      @ImageOptions(flipRtl = true)
      @Source("ExpandIcon_2x.png")
      ImageResource cellTreeClosedItem();

      /**
       * An image indicating that a node is loading.
       */
      @ImageOptions(flipRtl = true)
      @Source("ExpandingIcon_2x.png")
      ImageResource cellTreeLoading();
    
      @ImageOptions(flipRtl = true)
      @Source("CollapseIcon_2x.png")
      ImageResource cellTreeOpenItem();
      
      @Source({CellTree.Style.DEFAULT_CSS, "ObjectBrowser.css"})
      public Style cellTreeStyle();   
      
      public interface Style extends CellTree.Style
      {
         String fieldName();
         String fieldType();
         String tableViewDataset();
         String containerIcon();
         String searchMatches();
         String searchHidden();
      }
   }
   
   static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.cellTreeStyle().ensureInjected();
   }
   
   @DefaultLocale("en_US")
   public interface TableBrowserMessages extends CellTreeMessages {
     @DefaultMessage("Show more")
     String showMore();
     @DefaultMessage("(No tables)")
     String emptyTree();
   }
   
   public void setFilterText(String text)
   {
      objectsModel_.setFilterText(text);
      
      // defer execution of the matched element filter so the celltree can
      // render
      Scheduler.get().scheduleDeferred(() ->
         hideUnmatchedElements(objects_.getElement()));
   }
   
   /**
    * Hides nodes in the hierarchy which contain objects that don't match the
    * query. GWT's CellTree doesn't provide a way to temporarily remove nodes
    * from the tree or hide them, so we work around this by labeling the values
    * to be hidden, and then making pass through them here to hide elements
    * that don't match using the DOM directly.
    * @param ele Root element of the 
    */
   private void hideUnmatchedElements(Element ele)
   {
      // get all the rendered nodes in the CellTree
      Element[] parents = DomUtils.getElementsByClassName(
            ele, RES.cellTreeStyle().cellTreeItem());
      for (int i = 0; i < parents.length; i++)
      {
         // see if this rendered node contains any matches for the search
         Element[] matches = DomUtils.getElementsByClassName(parents[i], 
               RES.cellTreeStyle().searchMatches());
         if (matches.length == 0) 
         {
            // none of the child nodes has a match, so hide this parent
            parents[i].addClassName(RES.cellTreeStyle().searchHidden());
         }
         else
         {
            // at least one child node hsa a match
            parents[i].removeClassName(RES.cellTreeStyle().searchHidden());
         }
      }
   }
   
   private static final TableBrowserMessages MESSAGES 
                              = GWT.create(TableBrowserMessages.class);
   
   private final SimplePanelWithProgress hostPanel_;
   private final ScrollPanel scrollPanel_;
   private CellTree objects_;
   private VerticalPanel objectsWrapper_;
   private ObjectBrowserModel objectsModel_;
   @SuppressWarnings("unused")
   private Connection connection_;
   private HandlerRegistration registration_;
}
