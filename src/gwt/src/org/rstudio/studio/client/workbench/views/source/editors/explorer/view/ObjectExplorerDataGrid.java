/*
 * ObjectExplorerDataGrid.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer.view;

import java.util.ArrayList;
import java.util.List;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.VirtualizedDataGrid;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerServerOperations;

/*
 * This widget provides a tabular, drill-down view into an R object.
 *
 * ## Columns
 *
 * ### Name
 *
 * The name column contains three elements:
 *
 *    1) An (optional) 'drill-down' icon, that expands the node such that
 *       children of that object are shown and added to the table,
 *
 *    2) An icon, denoting the object's type (list, environment, etc.)
 *
 *    3) The object's name; that is, the binding through which is can be accessed
 *       from the parent object.
 *
 * ### Type
 *
 * A text column, giving a short description of the object's type. Typically, this
 * will be the object's class, alongside the object's length (if relevant).
 *
 * ### Value
 *
 * A short, succinct description of the value of the object within. Icons that can
 * be used for interacting with this row (e.g. generating the R code that can access
 * that value) are drawn in this cell.
 */

import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerInspectionResult;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.view.ObjectExplorerDataGrid.Data.ExpansionState;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlDivBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.RowHoverEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;

public class ObjectExplorerDataGrid
      extends VirtualizedDataGrid<ObjectExplorerDataGrid.Data>
      implements AttachEvent.Handler,
                 ClickHandler,
                 RowHoverEvent.Handler,
                 CellPreviewEvent.Handler<ObjectExplorerDataGrid.Data>
{
   private static interface Filter<T>
   {
      public boolean accept(T data);
   }
   
   public static class Data extends ObjectExplorerInspectionResult
   {
      protected Data()
      {
      }
      
      public static final native Data createMorePlaceholder(Data parent)
      /*-{
         return {
            "parent": parent,
            "placeholder": true
         };
      }-*/;
      
      public final native boolean isMorePlaceholder()
      /*-{
         return !!this["placeholder"];
      }-*/;
      
      public final boolean isAttributes()
      {
         return hasTag(TAG_ATTRIBUTES);
      }
      
      // The number of child rows that should be shown
      // for this node.
      public final native void setMaximumChildRowsShown(int limit)
      /*-{
         this["maxChildRowsShown"] = limit;
      }-*/;
      
      public final int getMaximumChildRowsShown()
      {
         return getMaximumChildRowsShownImpl(DEFAULT_ROW_LIMIT);
      }
      
      private final native int getMaximumChildRowsShownImpl(int defaultLimit)
      /*-{
         return this["maxChildRowsShown"] || defaultLimit;
      }-*/;
      
      // Whether this node is matched, according to the
      // current search query term.
      public final native void setMatched(boolean matched)
      /*-{
         this["matched"] = matched;
      }-*/;
      
      public final native boolean isMatched()
      /*-{
         return !!this["matched"];
      }-*/;
      
      // The current expansion state of this row.
      // Rows can either be expanded (children are visible),
      // or not expanded (children are hidden).
      public final ExpansionState getExpansionState()
      {
         String state = getExpansionStateImpl();
         return ExpansionState.valueOf(state.toUpperCase());
      }
      
      private final native String getExpansionStateImpl()
      /*-{
         return this["expansion_state"] || "closed";
      }-*/;
      
      public final void setExpansionState(ExpansionState state)
      {
         setExpansionStateImpl(state.name());
      }
      
      private final native void setExpansionStateImpl(String state)
      /*-{
         this["expansion_state"] = state;
      }-*/;
      
      // Whether a particular row is visible (that is, whether
      // the CellTable should draw this row).
      private final native boolean isVisible()
      /*-{
         var visible = this["visible"];
         if (typeof visible === "undefined")
            return true;
         return visible;
      }-*/;
      
      public final native void setVisible(boolean visible)
      /*-{
         this["visible"] = visible;
      }-*/;
      
      // The parent data associated with a node.
      public final native Data getParentData()
      /*-{
         return this["parent"] || null;
      }-*/;
      
      public final native void setParentData(Data data)
      /*-{
         this["parent"] = data;
      }-*/;
      
      // Whether this node has the parent object 'data'.
      public final native boolean hasParentData(Data data)
      /*-{
         
         for (var parent = this["parent"];
              parent != null;
              parent = parent["parent"])
         {
            if (parent == data)
               return true;
         }
         
         return false;
         
      }-*/;
      
      public final native JsArray<Data> getChildrenData()
      /*-{
         return this["children"] || null;
      }-*/;
      
      public final native void addChildrenData(JsArray<Data> data)
      /*-{
         var children = this["children"] || [];
         for (var i = 0, n = data.length; i < n; i++)
            children.push(data[i]);
         this["children"] = children;
      }-*/;
      
      // Return the node's depth, or the number of parents.
      public final int getDepth()
      {
         int depth = 0;
         for (Data parent = getParentData();
              parent != null;
              parent = parent.getParentData())
         {
            depth++;
         }
         
         return depth;
      }
      
      // Used to update ownership of children within the tree.
      // This is necessary as nodes received from the server
      // side will not have marked ownership (no known parent).
      public final void updateChildOwnership()
      {
         updateChildOwnership(getChildrenData(), this);
      }
      
      private static final void updateChildOwnership(JsArray<Data> children,
                                                     Data parent)
      {
         if (children == null)
            return;

         for (int i = 0, n = children.length(); i < n; i++)
         {
            // update parent data on this child
            Data child = children.get(i);
            child.setParentData(parent);
            
            // recurse
            updateChildOwnership(child.getChildrenData(), child);
         }
      }
      
      public final native void updateChildOwnershipImpl()
      /*-{
         var children = this["children"] || [];
         for (var i = 0, n = children.length; i < n; i++)
         {
            var child = children[i];
            child["parent"] = this;
         }
         
      }-*/;
      
      public enum ExpansionState
      {
         OPEN,
         CLOSED
      }
   }
   
   private String generateExtractingRCode(Data data, String finalReplacement)
   {
      if (data == null || data.isMorePlaceholder())
         return null;
      
      // extract all access strings from data + parents
      List<String> accessors = new ArrayList<String>();
      while (data != null && data.getObjectAccess() != null)
      {
         accessors.add(data.getObjectAccess());
         data = data.getParentData();
      }
      
      int n = accessors.size();
      if (n == 0)
         return finalReplacement;
      
      // start building up access string by repeatedly
      // substituting in accessors
      String code = accessors.get(0);
      for (int i = 1; i < n; i++)
         code = code.replaceAll("#", accessors.get(i));
      
      // finally, substitute in the original object
      code = code.replaceAll("#", finalReplacement);
      
      return code;
   }
   
   private String generateExtractingRCode(Data data)
   {
      return generateExtractingRCode(data, handle_.getTitle());
   }
   
   private static final int getParentLimit(Data data)
   {
      Data parent = data.getParentData();
      if (parent == null)
         return DEFAULT_ROW_LIMIT;
      return parent.getMaximumChildRowsShown();
   }
   
   private class NameCell extends AbstractCell<Data>
   {
      public NameCell()
      {
         super();
         String moreButtonCell = "<td><input type='button' value='More...' data-action='open'></input></td>";
         moreButtonCellHtml_ = SafeHtmlUtils.fromTrustedString(moreButtonCell);
      }
      
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         builder.append(TABLE_OPEN_TAG);
         builder.appendHtmlConstant("<tr>");
         
         if (data == null || data.isMorePlaceholder())
         {
            onNotExpandable(builder);
            addViewMoreIcon(builder);
         }
         else
         {
            addIndent(builder, data);
            addExpandIcon(builder, data);
            addName(builder, data);
         }
         
         builder.appendHtmlConstant("</tr>");
         builder.appendHtmlConstant("</table>");
      }
      
      private final void addIndent(SafeHtmlBuilder builder, Data data)
      {
         int indentPx = data.getDepth() * 10;
         if (indentPx == 0)
            return;
         
         String html = "<td style='width: " + indentPx + "px'></td>";
         builder.appendHtmlConstant(html);
      }
      
      private final boolean onNotExpandable(SafeHtmlBuilder builder)
      {
         builder.appendHtmlConstant("<td style='width: 20px'></td>");
         return false;
      }
      
      // Returns true if an icon was drawn; false if indent was drawn instead
      private final boolean addExpandIcon(final SafeHtmlBuilder builder,
                                          final Data data)
      {
         // bail if this node is not expandable
         if (!data.isExpandable())
            return onNotExpandable(builder);
         
         // bail if we're not showing attributes, but a child attributes
         // node is the only thing that exists
         if (!showAttributes_)
         {
            // bail if the object is unnamed & atomic
            if (data.isAtomic() && !data.isNamed())
               return onNotExpandable(builder);
            
            JsArray<Data> children = data.getChildrenData();
            if (children != null && children.length() == 1)
            {
               Data child = children.get(0);
               if (child.hasTag(TAG_ATTRIBUTES))
                  return onNotExpandable(builder);
            }
         }
         
         // add expand button
         builder.appendHtmlConstant("<td style='width: 20px;'>");
         switch (data.getExpansionState())
         {
         case CLOSED:
            builder.append(SafeHtmlUtil.createOpenTag("div",
                  "class", RES.dataGridStyle().spriteExpandIcon(),
                  "data-action", ACTION_OPEN));
            builder.appendHtmlConstant("</div>");
            break;
         case OPEN:
            builder.append(SafeHtmlUtil.createOpenTag("div",
                  "class", RES.dataGridStyle().spriteCollapseIcon(),
                  "data-action", ACTION_CLOSE));
            builder.appendHtmlConstant("</div>");
            break;
         }
         builder.appendHtmlConstant("</td>");
         
         return true;
      }
      
      private final void addName(SafeHtmlBuilder builder,
                                 Data data)
      {
         builder.appendHtmlConstant("<td>");
         
         HtmlDivBuilder divBuilder = HtmlBuilderFactory.get().createDivBuilder();
         divBuilder.title(data.getDisplayName());
         
         JsVectorString classes = JsVectorString.createVector();
         if (data.hasTag(TAG_VIRTUAL))
            classes.push(RES.dataGridStyle().virtual());
         
         if (!classes.isEmpty())
            divBuilder.className(classes.join(" "));
         
         String name = data.getDisplayName();
         if (name == null)
            name = "<unknown>";
         divBuilder.text(name);
         builder.append(divBuilder.asSafeHtml());
         
         builder.appendHtmlConstant("</td>");
      }
      
      private final void addViewMoreIcon(SafeHtmlBuilder builder)
      {
         builder.append(moreButtonCellHtml_);
      }
      
      private final SafeHtml moreButtonCellHtml_;
   }
   
   private static class TypeCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         if (data == null || data.isMorePlaceholder())
            return;
         
         builder.append(SafeHtmlUtil.createDiv(
               "title", data.getDisplayType()));
         builder.appendEscaped(data.getDisplayType());
         builder.appendHtmlConstant("</div>");
      }
      
   }
   
   private static class ValueCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         if (data == null || data.isMorePlaceholder())
            return;
         
         builder.append(TABLE_OPEN_TAG);
         builder.appendHtmlConstant("<tr>");
         
         // add description
         builder.appendHtmlConstant("<td style='width: 100%;'>");
         builder.append(SafeHtmlUtil.createDiv(
               "title", data.getDisplayDesc(),
               "class", RES.dataGridStyle().valueDesc()));
         builder.appendEscaped(data.getDisplayDesc());
         builder.appendHtmlConstant("</div>");
         builder.appendHtmlConstant("</td>");
         
         // for data.frames and functions, add a 'View' icon
         JsVectorString classes = data.getObjectClass().cast();
         for (String viewableClass : VIEWABLE_CLASSES)
         {
            if (classes.contains(viewableClass))
            {
               builder.appendHtmlConstant("<td>");
               addViewIcon(data, builder);
               builder.appendHtmlConstant("</td>");
               break;
            }
         }
         
         // add extract icon
         builder.appendHtmlConstant("<td>");
         addExtractIcon(data, builder);
         builder.appendHtmlConstant("</td>");
         
         builder.appendHtmlConstant("</tr>");
         builder.appendHtmlConstant("</table>");
      }
      
      private void addExtractIcon(Data data,
                                  SafeHtmlBuilder builder)
      {
         SafeHtml extractTag = SafeHtmlUtil.createDiv(
               "class",       CLASS + " " + RES.dataGridStyle().spriteExtractCodeIcon(),
               "style",       "visibility: hidden",
               "data-action", ACTION_EXTRACT);
         builder.append(extractTag);
         builder.appendHtmlConstant("</div>");
      }
      
      private void addViewIcon(Data data,
                               SafeHtmlBuilder builder)
      {
         SafeHtml viewTag = SafeHtmlUtil.createDiv(
               "class",       CLASS + " " + RES.dataGridStyle().spriteViewObjectIcon(),
               "style",       "visibility: hidden",
               "data-action", ACTION_VIEW);
         builder.append(viewTag);
         builder.appendHtmlConstant("</div>");
      }
      
      private static final String CLASS = RES.dataGridStyle().clickableIcon();      
      
      private static final String[] VIEWABLE_CLASSES = new String[] {
            "data.frame",
            "function"
      };
   }
   
   public ObjectExplorerDataGrid(ObjectExplorerHandle handle,
                                 SourceDocument document)
   {
      super(RES);
      handle_ = handle;
      document_ = document;
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setSize("100%", "100%");
      
      // add columns
      nameColumn_ = new IdentityColumn<Data>(new NameCell());
      addColumn(nameColumn_, new TextHeader("Name"));
      setColumnWidth(nameColumn_, NAME_COLUMN_WIDTH + "px");
      
      typeColumn_ = new IdentityColumn<Data>(new TypeCell());
      addColumn(typeColumn_, new TextHeader("Type"));
      setColumnWidth(typeColumn_, TYPE_COLUMN_WIDTH + "px");
      
      valueColumn_ = new IdentityColumn<Data>(new ValueCell());
      addColumn(valueColumn_, new TextHeader("Value"));
      
      // set updater
      dataProvider_ = new ListDataProvider<Data>();
      dataProvider_.setList(new ArrayList<Data>());
      dataProvider_.addDataDisplay(this);
      
      // register handlers
      setKeyboardSelectionHandler(this);
      addAttachHandler(this);
      addRowHoverHandler(this);
      addDomHandler(this, ClickEvent.getType());
      
      // populate the view once initially
      initializeRoot();
   }
   
   @Inject
   private void initialize(ObjectExplorerServerOperations server,
                           EventBus events)
   {
      server_ = server;
      events_ = events;
   }
   
   // Public methods ----
   
   public void refresh()
   {
      initializeRoot();
   }
   
   public void toggleShowAttributes(boolean showAttributes)
   {
      if (showAttributes == showAttributes_)
         return;
      
      showAttributes_ = showAttributes;
      synchronize();
   }
   
   public void setFilter(String filter)
   {
      filter_ = filter;
      synchronize();
   }
   
   // Handlers ---
   
   @Override
   public void onAttachOrDetach(AttachEvent event)
   {
      if (event.isAttached())
         return;
   }
   
   @Override
   public void onClick(ClickEvent event)
   {
      // extract target element
      Element targetEl = event.getNativeEvent().getEventTarget().cast();
      if (targetEl == null)
         return;
      
      // determine action associated with this row
      Element dataEl = DomUtils.findParentElement(targetEl, true, new DomUtils.ElementPredicate()
      {
         @Override
         public boolean test(Element el)
         {
            return el.hasAttribute("data-action");
         }
      });
      
      // find associated row index by looking up through the DOM
      Element rowEl = DomUtils.findParentElement(targetEl, new DomUtils.ElementPredicate()
      {
         @Override
         public boolean test(Element el)
         {
            return el.hasAttribute("__gwt_row");
         }
      });
      
      if (rowEl == null)
         return;
      
      int row = StringUtil.parseInt(rowEl.getAttribute("__gwt_row"), -1);
      if (row == -1)
         return;
      
      // if the user has clicked on the expand button, handle that
      if (dataEl != null)
      {
         // perform action
         String action = dataEl.getAttribute("data-action");
         performAction(action, row);
         return;
      }
      
      // otherwise, just select the row the user clicked on
      setKeyboardSelectedRow(row);
      setKeyboardSelectedColumn(0);
   }
   
   @Override
   public void onCellPreview(CellPreviewEvent<Data> preview)
   {
      Event event = Event.getCurrentEvent();
      int code = event.getKeyCode();
      int type = event.getTypeInt();
      int row = getKeyboardSelectedRow();
      boolean isDefault = false;
      
      if (type == Event.ONKEYDOWN || type == Event.ONKEYPRESS)
      {
         switch (code)
         {
         case KeyCodes.KEY_UP:
            selectRowRelative(-1);
            break;

         case KeyCodes.KEY_DOWN:
            selectRowRelative(+1);
            break;

         case KeyCodes.KEY_PAGEUP:
            selectRowRelative(-10);
            break;

         case KeyCodes.KEY_PAGEDOWN:
            selectRowRelative(+10);
            break;

         case KeyCodes.KEY_LEFT:
            selectParentOrClose(row);
            break;

         case KeyCodes.KEY_RIGHT:
            selectChildOrOpen(row);
            break;
            
         default:
            isDefault = true;
            break;
         }
      }
      
      else if (type == Event.ONKEYUP)
      {
         switch (code)
         {
         case KeyCodes.KEY_ENTER:
         case KeyCodes.KEY_SPACE:
            toggleExpansion(row);
            break;

         default:
            isDefault = true;
            break;
         }
      }
      else
      {
         isDefault = true;
      }
      
      // eat any non-default handled events
      if (!isDefault)
      {
         preview.setCanceled(true);
         event.stopPropagation();
         event.preventDefault();
      }
   }
   
   @Override
   public void onRowHover(RowHoverEvent event)
   {
      TableRowElement rowEl = event.getHoveringRow();
      Element[] buttonEls = DomUtils.getElementsByClassName(rowEl, RES.dataGridStyle().clickableIcon());
      if (buttonEls == null)
         return;
      
      if (event.isUnHover())
      {
         for (Element el : buttonEls)
            el.getStyle().setVisibility(Visibility.HIDDEN);
         
         // unset any element-specific maximum width that might've been set
         // on hover (see below)
         Element valueDescEl = DomUtils.getFirstElementWithClassName(rowEl, RES.dataGridStyle().valueDesc());
         if (valueDescEl != null)
            valueDescEl.getParentElement().getStyle().setWidth(100, Unit.PCT);
         
         // unset hovered row
         hoveredRow_ = null;
      }
      else
      {
         for (Element el : buttonEls)
            el.getStyle().setVisibility(Visibility.VISIBLE);
         
         // set hovered row (so that we can respond to resize events)
        hoveredRow_ = rowEl;
        onResize();
      }
   }
   
   @Override
   public void onResize()
   {
      super.onResize();
      updateHoverRowWidth();
   }
   
   private void updateHoverRowWidth()
   {
      if (hoveredRow_ == null)
         return;
      
      Element valueDescEl = DomUtils.getFirstElementWithClassName(
            hoveredRow_,
            RES.dataGridStyle().valueDesc());
      
      if (valueDescEl == null)
         return;
         
      // iterate through other table cells to compute width of
      // buttons available here
      Element containingRowEl = DomUtils.findParentElement(
            valueDescEl,
            new ElementPredicate()
            {
               @Override
               public boolean test(Element el)
               {
                  return el.hasTagName("tr");
               }
            });
      
      if (containingRowEl == null)
         return;
      
      // TODO: do a better job of automatically computing these widths,
      // rather than hard-coding them
      int buttonWidth = 0;
      int n = containingRowEl.getChildCount();
      if (n == 2)
         buttonWidth = 20;
      else if (n == 3)
         buttonWidth = 48;

      int totalWidth = getOffsetWidth();
      int remainingWidth =
            totalWidth -
            NAME_COLUMN_WIDTH -
            TYPE_COLUMN_WIDTH -
            buttonWidth -
            20;
      
      Element parentEl = valueDescEl.getParentElement();
      parentEl.getStyle().setPropertyPx("width", Math.max(0, remainingWidth));
   }
   
   // Private Methods ----
   
   private void selectRowRelative(int delta)
   {
      setKeyboardSelectedColumn(0);
      setKeyboardSelectedRow(getKeyboardSelectedRow() + delta);
   }
   
   private void selectParentOrClose(int row)
   {
      Data data = getData().get(row);
      
      // if this node has children and is currently expanded, close it
      if (data.isExpandable() && data.getExpansionState() == ExpansionState.OPEN)
      {
         closeRow(row);
         return;
      }
      
      // otherwise, select the parent associated with this row (if any)
      Data parent = data.getParentData();
      if (parent == null)
         return;
      
      List<Data> list = getData();
      for (int i = 0, n = row; i < n; i++)
      {
         if (list.get(i).equals(parent))
         {
            setKeyboardSelectedRow(i);
            break;
         }
      }
   }
   
   private void selectChildOrOpen(int row)
   {
      Data data = getData().get(row);
      
      if (data.isMorePlaceholder())
      {
         retrieveMore(row);
         return;
      }
      
      // if this node has children but is not expanded, expand it
      if (data.isExpandable() && data.getExpansionState() == ExpansionState.CLOSED)
      {
         openRow(row);
         return;
      }
      
      // otherwise, select the first child of this row (the next row)
      selectRowRelative(1);
   }
   
   private void toggleExpansion(int row)
   {
      Data data = getData().get(row);
      
      switch (data.getExpansionState())
      {
      case OPEN:
         closeRow(row);
         break;
      case CLOSED:
         openRow(row);
         break;
      }
   }
   
   private void performAction(String action, int row)
   {
      if (action.equals(ACTION_OPEN))
      {
         openRow(row);
      }
      else if (action.equals(ACTION_CLOSE))
      {
         closeRow(row);
      }
      else if (action.equals(ACTION_EXTRACT))
      {
         extractCode(row);
      }
      else if (action.equals(ACTION_VIEW))
      {
         viewRow(row);
      }
      else
      {
         assert false : "Unexpected action '" + action + "' on row " + row;
      }
   }
   
   private void openRow(final int row)
   {
      final Data data = getData().get(row);
      
      if (data.isMorePlaceholder())
      {
         retrieveMore(row);
         return;
      }
      
      // bail if we've attempted to open something non-expandable
      if (!data.isExpandable())
         assert false: "Attempted to expand non-recursive row " + row;
      
      // toggle expansion state
      data.setExpansionState(ExpansionState.OPEN);
      
      // resolve children and show
      withChildren(data, false, new Command()
      {
         @Override
         public void execute()
         {
            // set all direct children as visible
            JsArray<Data> children = data.getChildrenData();
            for (int i = 0, n = children.length(); i < n; i++)
               children.get(i).setVisible(true);
            
            // set attributes as visible if available
            Data attributes = data.getObjectAttributes().<Data>cast();
            if (attributes != null)
               attributes.setVisible(true);
            
            // force update of data grid
            synchronize();
         }
      });
   }
   
   private void closeRow(int row)
   {
      final Data data = getData().get(row);
      
      // bail if we've attempted to close something non-expandable
      if (!data.isExpandable())
         assert false: "Attempted to close non-recursive row " + row;
      
      // toggle expansion state
      data.setExpansionState(ExpansionState.CLOSED);
      
      // set direct children as non-visible
      withChildren(data, false, new Command()
      {
         @Override
         public void execute()
         {
            // set all direct children as invisible
            JsArray<Data> children = data.getChildrenData();
            for (int i = 0, n = children.length(); i < n; i++)
               children.get(i).setVisible(false);
            
            // set attributes as invisible if available
            Data attributes = data.getObjectAttributes().<Data>cast();
            if (attributes != null)
               attributes.setVisible(false);
            
            // force update of data grid
            synchronize();
         }
      });
   }
   
   private void extractCode(int row)
   {
      Data data = getData().get(row);
      String code = generateExtractingRCode(data);
      events_.fireEvent(new SendToConsoleEvent(code, false));
   }
   
   private void viewRow(int row)
   {
      Data data = getData().get(row);
      String code = generateExtractingRCode(data);
      code = "View(" + code + ")";
      events_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   private void retrieveMore(int row)
   {
      Data data = getData().get(row);
      Data parent = data.getParentData();
      if (parent == null)
         return;
      
      // select the previous row (so that we don't end up scrolling all over the place)
      selectRowRelative(row);
      
      // update the limit on the number of children we're showing
      parent.setMaximumChildRowsShown(parent.getMaximumChildRowsShown() + DEFAULT_ROW_LIMIT);
      
      withChildren(parent, true, new Command()
      {
         @Override
         public void execute()
         {
            synchronize();
         }
      });
   }
   
   private void withChildren(final Data data,
                             final boolean forceRequest,
                             final Command command)
   {
      // if we already have children, exit early
      JsArray<Data> children = data.getChildrenData();
      if (!forceRequest && children != null)
      {
         if (command != null)
            command.execute();
         return;
      }
      
      // no children; make a server RPC request and then call back
      String extractingCode = generateExtractingRCode(data, "`__OBJECT__`");
      server_.explorerInspectObject(
            handle_.getId(),
            extractingCode,
            data.getDisplayName(),
            data.getObjectAccess(),
            data.getTags().<JsArrayString>cast(),
            data.getNumChildren(),
            new ServerRequestCallback<ObjectExplorerInspectionResult>()
            {
               @Override
               public void onResponseReceived(ObjectExplorerInspectionResult result)
               {
                  // set parent ownership for children
                  JsArray<Data> children = result.getChildren().cast();
                  data.addChildrenData(children);
                  for (int i = 0, n = children.length(); i < n; i++)
                     children.get(i).setParentData(data);
                  
                  // set parent ownership for attributes
                  Data attributes = result.getObjectAttributes().<Data>cast();
                  if (attributes != null)
                  {
                     data.setObjectAttributes(attributes);
                     attributes.setParentData(data);
                  }
                  
                  // execute command
                  if (command != null)
                     command.execute();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void initializeRoot()
   {
      server_.explorerBeginInspect(
            handle_.getId(),
            handle_.getName(),
            new ServerRequestCallback<ObjectExplorerInspectionResult>()
            {
               @Override
               public void onResponseReceived(ObjectExplorerInspectionResult result)
               {
                  root_ = result.cast();
                  root_.updateChildOwnership();
                  root_.setExpansionState(ExpansionState.OPEN);
                  synchronize();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
      
   }
   
   private void synchronize()
   {
      final String filter = StringUtil.notNull(filter_).trim();
      
      // only include visible data in the table
      // TODO: we should consider how to better handle
      // piecewise updates to the table, rather than
      // shunting a whole new list in
      List<Data> data = flatten(root_, new Filter<Data>()
      {
         @Override
         public boolean accept(Data data)
         {
            // detect if this matches the current filter
            if (!filter.isEmpty())
            { 
               data.setMatched(false);
               String[] fields = {
                     data.getDisplayName(),
                     data.getDisplayType(),
                     data.getDisplayDesc()
               };

               for (String field : fields)
               {
                  int index = field.toLowerCase().indexOf(filter.toLowerCase());
                  if (index != -1)
                  {
                     data.setMatched(true);
                     break;
                  }
               }
            }
            
            // otherwise, check whether it's currently visible
            return data.isVisible();
         }
      });
      
      // remove entries that don't match filter
      if (!filter.isEmpty())
      {
         data = ListUtil.filter(data, new FilterPredicate<Data>()
         {
            @Override
            public boolean test(Data object)
            {
               for (Data self = object;
                    self != null;
                    self = self.getParentData())
               {
                  if (self.isMatched())
                     return true;
               }
               
               return false;
            }
         });
      }
      
      setData(data);
      redraw();
   }
   
   @Override
   public int getRowHeight()
   {
      return 24;
   }
   
   @Override
   public int getTotalNumberOfRows()
   {
      if (dataProvider_ == null)
         return 0;
      return getData().size();
   }
   
   public List<Data> getData()
   {
      return dataProvider_.getList();
   }
   
   private void setData(List<Data> data)
   {
      dataProvider_.setList(data);
   }
   
   private final List<Data> flatten(Data data,
                                    Filter<Data> filter) 
   {
      List<Data> list = new ArrayList<Data>();
      flattenImpl(data, filter, list);
      return list;
   }
   
   private final void flattenImpl(Data data,
                                  Filter<Data> filter,
                                  List<Data> output)
   {
      // exit if we're not accepting data here
      if (filter != null && !filter.accept(data))
         return;
      
      // add data
      output.add(data);
      
      // recurse through children
      JsArray<Data> children = data.getChildrenData();
      if (children == null)
         return;
      
      // only add children within the drawing limit to this list
      int n = Math.min(children.length(), data.getMaximumChildRowsShown());
      for (int i = 0; i < n; i++)
         flattenImpl(children.get(i), filter, output);
      
      // add a dummy 'More...' element
      boolean drawMore = 
            data.getExpansionState() == ExpansionState.OPEN &&
            data.isMoreAvailable();
      
      if (drawMore)
         output.add(Data.createMorePlaceholder(data));
      
      // add attributes if relevant
      if (showAttributes_)
      {
         Data attributes = data.getObjectAttributes().<Data>cast();
         if (attributes != null)
            flattenImpl(data.getObjectAttributes().<Data>cast(), filter, output);
      }
   }
   
   // Members ----
   
   private final ObjectExplorerHandle handle_;
   private Data root_;
   
   @SuppressWarnings("unused")
   private final SourceDocument document_;
   
   private final IdentityColumn<Data> nameColumn_;
   private final IdentityColumn<Data> typeColumn_;
   private final IdentityColumn<Data> valueColumn_;
   
   private final ListDataProvider<Data> dataProvider_;
   
   private TableRowElement hoveredRow_;
   private boolean showAttributes_;
   private String filter_;
   
   // Injected ----
   private ObjectExplorerServerOperations server_;
   private EventBus events_;
   
   // Static Members ----
   private static final int NAME_COLUMN_WIDTH = 180;
   private static final int TYPE_COLUMN_WIDTH = 180;
   
   private static final int DEFAULT_ROW_LIMIT = 200;
   
   private static final String ACTION_OPEN    = "open";
   private static final String ACTION_CLOSE   = "close";
   private static final String ACTION_EXTRACT = "extract";
   private static final String ACTION_VIEW    = "view";
   
   private static final String TAG_ATTRIBUTES = "attributes";
   private static final String TAG_VIRTUAL    = "virtual";
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ObjectExplorerDataGrid.css"})
      Styles dataGridStyle();
      
      @Source("images/expandIcon.png")
      ImageResource expandIcon();
      
      @Source("images/expandIcon_2x.png")
      ImageResource expandIcon2x();
      
      @Source("images/collapseIcon.png")
      ImageResource collapseIcon();
      
      @Source("images/collapseIcon_2x.png")
      ImageResource collapseIcon2x();
      
      @Source("images/extractCode.png")
      ImageResource extractCode();
      
      @Source("images/extractCode_2x.png")
      ImageResource extractCode2x();
      
      @Source("images/viewObject.png")
      ImageResource viewObject();
      
      @Source("images/viewObject_2x.png")
      ImageResource viewObject2x();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String virtual();
      String verticalAlignHelper();
      
      String spriteExpandIcon();
      String spriteCollapseIcon();
      String spriteExtractCodeIcon();
      String spriteViewObjectIcon();
      
      String cellInnerTable();
      String valueDesc();
      String clickableIcon();
   }
   
   private static final Resources RES = GWT.create(Resources.class);

   private static final SafeHtml TABLE_OPEN_TAG = SafeHtmlUtil.createOpenTag(
         "table",
         "class", RES.dataGridStyle().cellInnerTable());
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}
