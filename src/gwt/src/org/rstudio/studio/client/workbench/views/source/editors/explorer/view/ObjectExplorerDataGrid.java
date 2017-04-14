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
import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
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
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.RowHoverEvent;
import com.google.gwt.user.cellview.client.TextColumn;
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
      
      // TODO: more direct way?
      public final boolean isAttributes()
      {
         return "(attributes)".equals(getDisplayName());
      }
      
      public final native void setMatched(boolean matched)
      /*-{
         this["matched"] = matched;
      }-*/;
      
      public final native boolean isMatched()
      /*-{
         return !!this["matched"];
      }-*/;
      
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
      
      public final native Data getParentData()
      /*-{
         return this["parent"] || null;
      }-*/;
      
      public final native void setParentData(Data data)
      /*-{
         this["parent"] = data;
      }-*/;
      
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
   
   private String generateExtractingRCode(Data data)
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
         return handle_.getName();
      
      // start building up access string by repeatedly
      // substituting in accessors
      String code = accessors.get(0);
      for (int i = 1; i < n; i++)
         code = code.replaceAll("#", accessors.get(i));
      
      // finally, substitute in the original object
      code = code.replaceAll("#", handle_.getName());
      
      return code;
   }
   
   private static interface Filter<T>
   {
      public boolean accept(T data);
   }
   
   private class NameCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         builder.appendHtmlConstant("<table>");
         builder.appendHtmlConstant("<tr>");
         
         builder.appendHtmlConstant("<td>");
         addIndent(builder, data);
         builder.appendHtmlConstant("</td>");
         
         builder.appendHtmlConstant("<td>");
         addExpandIcon(builder, data);
         builder.appendHtmlConstant("</td>");
         
         builder.appendHtmlConstant("<td>");
         addIcon(builder, data);
         builder.appendHtmlConstant("</td>");
         
         builder.appendHtmlConstant("<td>");
         addName(builder, data);
         builder.appendHtmlConstant("</td>");
         
         builder.appendHtmlConstant("</tr>");
         builder.appendHtmlConstant("</table>");
      }
      
      private final void addIndent(SafeHtmlBuilder builder, Data data)
      {
         int indentPx = data.getDepth() * 8;
         if (indentPx == 0)
            return;
         
         String html = "<div style='width: " + indentPx + "px'></div>";
         builder.appendHtmlConstant(html);
      }
      
      private final boolean onNotExpandable(SafeHtmlBuilder builder,
                                            Data data)
      {
         builder.appendHtmlConstant("<div style='width: 20px'></div>");
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
            JsArray<Data> children = data.getChildrenData();
            if (children != null && children.length() == 1)
            {
               Data child = children.get(0);
               if (child.hasTag(TAG_ATTRIBUTES))
                  return onNotExpandable(builder, data);
            }
         }
         
         // add expand button
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
         
         return true;
      }
      
      private final void addIcon(SafeHtmlBuilder builder,
                                 ObjectExplorerInspectionResult result)
      {
         // TODO: icon based on type
         SafeHtml openDiv = SafeHtmlUtil.createDiv(
               "class", RES.dataGridStyle().objectTypeIcon());
         builder.append(openDiv);
         builder.append(IMAGE_TYPE_DATA.getSafeHtml());
         builder.appendHtmlConstant("</div>");
      }
      
      private final void addName(SafeHtmlBuilder builder,
                                 ObjectExplorerInspectionResult result)
      {
         boolean virtual = result.hasTag(TAG_VIRTUAL);
         if (virtual)
         {
            builder.appendHtmlConstant("<div style='font-style: italic'>");
         }
         else
         {
            builder.appendHtmlConstant("<div>");
         }
         
         String name = result.getDisplayName();
         if (name == null)
            name = "<unknown>";
         builder.appendEscaped(name);
         
         builder.appendHtmlConstant("</div>");
      }
   }
   
   private static class ButtonCell extends AbstractCell<Data>
   {
      @Override
      public void render(Context context,
                         Data data,
                         SafeHtmlBuilder builder)
      {
         SafeHtml extractTag = SafeHtmlUtil.createDiv(
               "class",       ThemeStyles.INSTANCE.clickableIcon(),
               "style",       "visibility: hidden;",
               "data-action", ACTION_EXTRACT);
         
         // extract button
         builder.append(extractTag);
         builder.append(IMAGE_EXTRACT_CODE.getSafeHtml());
         builder.appendHtmlConstant("</div>");
      }
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
      setColumnWidth(nameColumn_, "200px");
      
      typeColumn_ = new TextColumn<Data>()
      {
         @Override
         public String getValue(Data data)
         {
            return data.getDisplayType();
         }
      };
      addColumn(typeColumn_, new TextHeader("Type"));
      setColumnWidth(typeColumn_, "200px");
      
      valueColumn_ = new TextColumn<Data>()
      {
         @Override
         public String getValue(Data data)
         {
            return data.getDisplayDesc();
         }
      };
      addColumn(valueColumn_, new TextHeader("Value"));
      
      buttonColumn_ = new IdentityColumn<Data>(new ButtonCell());
      addColumn(buttonColumn_);
      setColumnWidth(buttonColumn_, "30px");
      
      // set updater
      dataProvider_ = new ListDataProvider<Data>();
      dataProvider_.setList(new ArrayList<Data>());
      dataProvider_.addDataDisplay(this);
      
      // register handlers
      setKeyboardSelectionHandler(this);
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
      Element[] buttonEls = DomUtils.getElementsByClassName(rowEl, ThemeStyles.INSTANCE.clickableIcon());
      if (buttonEls == null)
         return;
      
      if (event.isUnHover())
      {
         for (Element el : buttonEls)
            el.getStyle().setVisibility(Visibility.HIDDEN);
      }
      else
      {
         for (Element el : buttonEls)
            el.getStyle().setVisibility(Visibility.VISIBLE);
      }
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
      else
      {
         assert false : "Unexpected action '" + action + "' on row " + row;
      }
   }
   
   private void openRow(final int row)
   {
      final Data data = getData().get(row);
      Debug.logToRConsole("Opening row for object:" + data.getObjectId());
      Debug.logObject(data);
      
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
      Debug.logToRConsole("Closing row for object:" + data.getObjectId());
      Debug.logObject(data);
      
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
      server_.explorerInspectObject(
            data.getObjectId(),
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
            handle_.getName(),
            null,
            null,
            1,
            new ServerRequestCallback<ObjectExplorerInspectionResult>()
            {
               @Override
               public void onResponseReceived(ObjectExplorerInspectionResult result)
               {
                  Debug.logObject(result);
                  
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
   private final TextColumn<Data> typeColumn_;
   private final TextColumn<Data> valueColumn_;
   private final IdentityColumn<Data> buttonColumn_;
   
   private final ListDataProvider<Data> dataProvider_;
   
   private final ObjectExplorerHandle handle_;
   private Data root_;
   
   private boolean showAttributes_;
   private String filter_;
   
   // Injected ----
   private ObjectExplorerServerOperations server_;
   private EventBus events_;
   
   // Static Members ----
   private static final String ACTION_OPEN    = "open";
   private static final String ACTION_CLOSE   = "close";
   private static final String ACTION_EXTRACT = "extract";
   
   private static final String TAG_ATTRIBUTES = "attributes";
   private static final String TAG_VIRTUAL    = "virtual";
   
   private static final ImageResource2x IMAGE_RIGHT_ARROW =
         new ImageResource2x(StandardIcons.INSTANCE.right_arrow2x());

   private static final ImageResource2x IMAGE_DOWN_ARROW =
         new ImageResource2x(ThemeResources.INSTANCE.mediumDropDownArrow2x());

   private static final ImageResource2x IMAGE_TYPE_DATA =
         new ImageResource2x(ThemeResources.INSTANCE.zoomDataset2x());
   
   private static final ImageResource2x IMAGE_EXTRACT_CODE =
         new ImageResource2x(StandardIcons.INSTANCE.run2x());
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ObjectExplorerDataGrid.css"})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String openRowIcon();
      String closeRowIcon();
      String objectTypeIcon();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}
