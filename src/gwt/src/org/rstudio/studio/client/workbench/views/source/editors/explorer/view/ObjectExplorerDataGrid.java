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
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.SafeHtmlUtil.TagBuilder;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
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
 * A short, succinct description of the value of the object within.
 *
 * ### Size
 *
 * The amount of memory occupied by this object.
 *
 * ### Inspect
 *
 * A set of one or more widgets, used to e.g. open data.frames in the Data Viewer,
 * to find functions, and so on.
 */

import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerInspectionResult;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.view.ObjectExplorerDataGrid.Data.ExpansionState;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.RowHoverEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Event;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;

public class ObjectExplorerDataGrid
      extends DataGrid<ObjectExplorerDataGrid.Data>
      implements ClickHandler,
                 RowHoverEvent.Handler,
                 CellPreviewEvent.Handler<ObjectExplorerDataGrid.Data>
{
   public static class Data extends ObjectExplorerInspectionResult
   {
      protected Data()
      {
      }
      
      public final boolean isAttributes()
      {
         return hasTag(TAG_ATTRIBUTES);
      }
      
      // The number of child rows that should be shown
      // for this node.
      public final native void setLimit(int limit)
      /*-{
         this["limit"] = limit;
      }-*/;
      
      public final int getLimit()
      {
         return getLimitImpl(DEFAULT_ROW_LIMIT);
      }
      
      private final native int getLimitImpl(int defaultLimit)
      /*-{
         return this["limit"] || defaultLimit;
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
      
      public final native void setChildrenData(JsArray<Data> data)
      /*-{
         this["children"] = data;
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
            children[i].parent = this;
         
      }-*/;
      
      public enum ExpansionState
      {
         OPEN,
         CLOSED
      }
   }
   
   public static class TableBuilder
         extends DefaultCellTableBuilder<Data>
   {
      public TableBuilder(AbstractCellTable<Data> table)
      {
         super(table);
      }
      
      @Override
      public void buildRowImpl(Data data, int index)
      {
         // TODO: bail if this is a row outside of the current
         // drawing limit
         super.buildRowImpl(data, index);
      }
   }
   
   private String generateExtractingRCode(Data data, String finalReplacement)
   {
      if (data == null)
         return null;
      
      // extract all access strings from data + parents
      List<String> accessors = new ArrayList<String>();
      while (data != null && data.getObjectAccess() != null)
      {
         accessors.add(data.getObjectAccess());
         data = data.getParentData();
      }
      
      // if we have no accessors, this must be the root object
      // just return from associated handle
      int n = accessors.size();
      if (n == 0)
         return handle_.getTitle();
      
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
   
   private static interface Filter<T>
   {
      public boolean accept(T data);
   }
   
   private static final int getParentLimit(Data data)
   {
      Data parent = data.getParentData();
      if (parent == null)
         return DEFAULT_ROW_LIMIT;
      return parent.getLimit();
   }
   
   private class NameCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         builder.append(TABLE_OPEN_TAG);
         builder.appendHtmlConstant("<tr>");
         
         addIndent(builder, data);
         addExpandIcon(builder, data);
         addIcon(builder, data);
         addName(builder, data);
         
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
      
      private final boolean onNotExpandable(SafeHtmlBuilder builder,
                                            Data data)
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
            return onNotExpandable(builder, data);
         
         // bail if we're not showing attributes, but a child attributes
         // node is the only thing that exists
         if (!showAttributes_)
         {
            // bail if the object is unnamed & atomic
            if (data.isAtomic() && !data.isNamed())
               return onNotExpandable(builder, data);
            
            JsArray<Data> children = data.getChildrenData();
            if (children != null && children.length() == 1)
            {
               Data child = children.get(0);
               if (child.hasTag(TAG_ATTRIBUTES))
                  return onNotExpandable(builder, data);
            }
         }
         
         // add expand button
         builder.appendHtmlConstant("<td style='width: 20px;'>");
         switch (data.getExpansionState())
         {
         case CLOSED:
            builder.append(SafeHtmlUtil.createOpenTag("div",
                  "class", RES.dataGridStyle().openRowIcon(),
                  "data-action", ACTION_OPEN));
            builder.append(IMAGE_RIGHT_ARROW.getSafeHtml());
            builder.appendHtmlConstant("</div>");
            break;
         case OPEN:
            builder.append(SafeHtmlUtil.createOpenTag("div",
                  "class", RES.dataGridStyle().closeRowIcon(),
                  "data-action", ACTION_CLOSE));
            builder.append(IMAGE_DOWN_ARROW.getSafeHtml());
            builder.appendHtmlConstant("</div>");
            break;
         }
         builder.appendHtmlConstant("</td>");
         
         return true;
      }
      
      private final void addIcon(SafeHtmlBuilder builder,
                                 Data data)
      {
         JsVectorString classes = data.getObjectClass().cast();
         
         // determine appropriate icon
         ImageResource resource = null;
         if (data.isS4())
            resource = CodeIcons.INSTANCE.clazz2x();
         else if (classes.contains("function"))
            resource = CodeIcons.INSTANCE.function2x();
         else if (classes.contains("data.frame") ||
                  classes.contains("matrix") ||
                  classes.contains("array"))
            resource = CodeIcons.INSTANCE.dataFrame2x();
         else if (classes.contains("list"))
            resource = CodeIcons.INSTANCE.clazz2x();
         else if (classes.contains("R6"))
            resource = CodeIcons.INSTANCE.clazz2x();
         else if (classes.contains("environment"))
            resource = CodeIcons.INSTANCE.environment2x();
         else
            resource = CodeIcons.INSTANCE.variable2x();
         
         // add it
         ImageResource2x res2x = new ImageResource2x(resource);
         builder.appendHtmlConstant("<td style='width: 20px;'><div>");
         builder.append(res2x.getSafeHtml());
         builder.appendHtmlConstant("</td></div>");
      }
      
      private final void addName(SafeHtmlBuilder builder,
                                 Data data)
      {
         builder.appendHtmlConstant("<td>");
         
         TagBuilder tagBuilder = new TagBuilder("div");
         tagBuilder.set("title", data.getDisplayName());
         
         JsVectorString classes = JsVectorString.createVector();
         if (data.hasTag(TAG_VIRTUAL))
            classes.push(RES.dataGridStyle().virtual());
         
         if (!classes.isEmpty())
            tagBuilder.set("class", classes.join(" "));
         
         builder.append(tagBuilder.toSafeHtml());
         
         String name = data.getDisplayName();
         if (name == null)
            name = "<unknown>";
         builder.appendEscaped(name);
         
         builder.appendHtmlConstant("</div>");
         builder.appendHtmlConstant("</td>");
         
      }
   }
   
   private static class TypeCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
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
               "class",       CLASS,
               "style",       "visibility: hidden",
               "data-action", ACTION_EXTRACT);
         
         builder.append(extractTag);
         builder.append(IMAGE_EXTRACT_CODE.getSafeHtml());
         builder.appendHtmlConstant("</div>");
      }
      
      private void addViewIcon(Data data,
                               SafeHtmlBuilder builder)
      {
         SafeHtml viewTag = SafeHtmlUtil.createDiv(
               "class",       CLASS,
               "style",       "visibility: hidden",
               "data-action", ACTION_VIEW);
         
         builder.append(viewTag);
         builder.append(IMAGE_VIEW_CODE.getSafeHtml());
         builder.appendHtmlConstant("</div>");
      }
      
      private static final String CLASS = StringUtil.join(new String[] {
            RES.dataGridStyle().clickableIcon(),
            RES.dataGridStyle().buttonIcon()
      }, " ");
      
      private static final String[] VIEWABLE_CLASSES = new String[] {
            "data.frame",
            "function"
      };
   }
   
   public ObjectExplorerDataGrid(ObjectExplorerHandle handle)
   {
      super(1000, RES);
      handle_ = handle;
      
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
      setTableBuilder(new TableBuilder(this));
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
      Element containingRowEl = DomUtils.findParentElement(valueDescEl, new ElementPredicate()
      {
         @Override
         public boolean test(Element el)
         {
            return el.hasTagName("tr");
         }
      });
      
      if (containingRowEl == null)
         return;
         
      int buttonWidth = 0;
      for (int i = 1, n = containingRowEl.getChildCount(); i < n; i++)
         buttonWidth += 24;

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
      
      // bail if we've attempted to open something non-expandable
      if (!data.isExpandable())
         assert false: "Attempted to expand non-recursive row " + row;
      
      // toggle expansion state
      data.setExpansionState(ExpansionState.OPEN);
      
      // resolve children and show
      withChildren(data, new CommandWithArg<JsArray<Data>>()
      {
         @Override
         public void execute(JsArray<Data> children)
         {
            // set all direct children as visible
            for (int i = 0, n = children.length(); i < n; i++)
               children.get(i).setVisible(true);
            
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
      withChildren(data, new CommandWithArg<JsArray<Data>>()
      {
         @Override
         public void execute(JsArray<Data> children)
         {
            // set all direct children as invisible
            for (int i = 0, n = children.length(); i < n; i++)
               children.get(i).setVisible(false);
            
            // force update of data grid
            synchronize();
         }
      });
   }
   
   private void extractCode(int row)
   {
      Data data = getData().get(row);
      String code = generateExtractingRCode(data);
      events_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   private void viewRow(int row)
   {
      Data data = getData().get(row);
      String code = generateExtractingRCode(data);
      code = "View(" + code + ")";
      events_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   private void withChildren(final Data data,
                             final CommandWithArg<JsArray<Data>> command)
   {
      // if we already have children, use them
      JsArray<Data> children = data.getChildrenData();
      if (children != null)
      {
         command.execute(children);
         return;
      }
      
      // no children; make a server RPC request and then call back
      String extractingCode = generateExtractingRCode(data, "`__OBJECT__`");
      server_.explorerInspectObject(
            handle_.getId(),
            extractingCode,
            data.getDisplayName(),
            data.getObjectAccess(),
            data.getTags(),
            1,
            new ServerRequestCallback<ObjectExplorerInspectionResult>()
            {
               @Override
               public void onResponseReceived(ObjectExplorerInspectionResult result)
               {
                  // set parent ownership for children
                  JsArray<Data> children = result.getChildren().cast();
                  data.setChildrenData(children);
                  for (int i = 0, n = children.length(); i < n; i++)
                     children.get(i).setParentData(data);
                  
                  // execute command with children
                  command.execute(children);
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
      server_.explorerInspectObject(
            handle_.getId(),
            null,
            handle_.getName(),
            null,
            null,
            1,
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
            
            // skip attributes if disabled
            if (!showAttributes_ && data.isAttributes())
               return false;
            
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
   }
   
   private List<Data> getData()
   {
      return dataProvider_.getList();
   }
   
   private void setData(List<Data> data)
   {
      dataProvider_.setList(data);
   }
   
   private static final List<Data> flatten(Data data,
                                           Filter<Data> filter) 
   {
      List<Data> list = new ArrayList<Data>();
      flattenImpl(data, filter, list);
      return list;
   }
   
   private static final void flattenImpl(Data data,
                                         Filter<Data> filter,
                                         List<Data> output)
   {
      // exit if we're not accepting data here
      if (filter != null && !filter.accept(data))
         return;
      
      // add data
      output.add(data);
      
      // recurse
      JsArray<Data> children = data.getChildrenData();
      if (children == null)
         return;
      
      for (int i = 0, n = children.length(); i < n; i++)
         flattenImpl(children.get(i), filter, output);
   }
   
   private final IdentityColumn<Data> nameColumn_;
   private final IdentityColumn<Data> typeColumn_;
   private final IdentityColumn<Data> valueColumn_;
   
   private final ListDataProvider<Data> dataProvider_;
   
   private final ObjectExplorerHandle handle_;
   private Data root_;
   
   private TableRowElement hoveredRow_;
   private boolean showAttributes_;
   private String filter_;
   
   // Injected ----
   private ObjectExplorerServerOperations server_;
   private EventBus events_;
   
   // Static Members ----
   private static final int NAME_COLUMN_WIDTH = 180;
   private static final int TYPE_COLUMN_WIDTH = 180;
   
   private static final int DEFAULT_ROW_LIMIT = 6;
   
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
      
      @Source("images/downArrow.png")
      ImageResource downArrow();
      
      @Source("images/downArrow_2x.png")
      ImageResource downArrow2x();
      
      @Source("images/extractCode.png")
      ImageResource extractCode();
      
      @Source("images/extractCode_2x.png")
      ImageResource extractCode2x();
      
      @Source("images/rightArrow.png")
      ImageResource rightArrow();
      
      @Source("images/rightArrow_2x.png")
      ImageResource rightArrow2x();
      
      @Source("images/viewObject.png")
      ImageResource viewObject();
      
      @Source("images/viewObject_2x.png")
      ImageResource viewObject2x();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String openRowIcon();
      String closeRowIcon();
      String objectTypeIcon();
      String valueDesc();
      String buttonIcon();
      String cellInnerTable();
      String virtual();
      String clickableIcon();
   }
   
   private static final Resources RES = GWT.create(Resources.class);

   private static final ImageResource2x IMAGE_RIGHT_ARROW = new ImageResource2x(
         RES.rightArrow(),
         RES.rightArrow2x());

   private static final ImageResource2x IMAGE_DOWN_ARROW = new ImageResource2x(
         RES.downArrow(),
         RES.downArrow2x());

   private static final ImageResource2x IMAGE_EXTRACT_CODE = new ImageResource2x(
         RES.extractCode(),
         RES.extractCode2x());

   private static final ImageResource2x IMAGE_VIEW_CODE = new ImageResource2x(
         RES.viewObject(),
         RES.viewObject2x());
   
   private static final SafeHtml TABLE_OPEN_TAG = SafeHtmlUtil.createOpenTag(
         "table",
         "class", RES.dataGridStyle().cellInnerTable());
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}
