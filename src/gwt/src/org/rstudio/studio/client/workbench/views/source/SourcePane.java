/*
 * SourcePane.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DocTabLayoutPanel;
import org.rstudio.core.client.theme.DocTabSelectionEvent;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.OperationWithInput;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.common.AutoGlassAttacher;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.source.Source.Display;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigationHistory;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;

public class SourcePane extends LazyPanel implements Display,
                                                     BeforeShowCallback,
                                                     EnsureVisibleSourceWindowEvent.Handler,
                                                     HasEnsureVisibleHandlers,
                                                     HasEnsureHeightHandlers,
                                                     MaximizeSourceWindowEvent.Handler,
                                                     ProvidesResize,
                                                     RequiresResize,
                                                     RequiresVisibilityChanged,
                                                     TabClosingHandler,
                                                     TabCloseHandler,
                                                     TabClosedHandler,
                                                     TabReorderHandler
{
   public interface Binder extends CommandBinder<Commands, SourcePane> {}

   @Inject
   public SourcePane()
   {   
      Commands commands_ = RStudioGinjector.INSTANCE.getCommands();
      Binder binder = GWT.<Binder>create(Binder.class);
      binder.bind(commands_, this);
      events_ = RStudioGinjector.INSTANCE.getEventBus();
      events_.addHandler(MaximizeSourceWindowEvent.TYPE, this);
      events_.addHandler(EnsureVisibleSourceWindowEvent.TYPE, this);

      setVisible(true);
      ensureWidget();

      if (getTabCount() > 0 && getActiveTabIndex() >= 0)
         editors_.get(getActiveTabIndex()).onInitiallyLoaded();
   }

   // overriden LazyPanel methods

   @Override
   protected Widget createWidget()
   {
      final int UTILITY_AREA_SIZE = 74;

      panel_ = new LayoutPanel();

      new AutoGlassAttacher(panel_);

      tabPanel_ =  new DocTabLayoutPanel(true, 65, UTILITY_AREA_SIZE);
      panel_.setSize("100%", "100%");
      panel_.add(tabPanel_);
      panel_.setWidgetTopBottom(tabPanel_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetLeftRight(tabPanel_, 0, Unit.PX, 0, Unit.PX);

      utilPanel_ = new HTML();
      utilPanel_.setStylePrimaryName(ThemeStyles.INSTANCE.multiPodUtilityArea());
      panel_.add(utilPanel_);
      panel_.setWidgetRightWidth(utilPanel_,
                                    0, Unit.PX,
                                    UTILITY_AREA_SIZE, Unit.PX);
      panel_.setWidgetTopHeight(utilPanel_, 0, Unit.PX, 22, Unit.PX);

      tabOverflowPopup_ = new TabOverflowPopupPanel();
      tabOverflowPopup_.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            manageChevronVisibility();
         }
      });
      chevron_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.chevron2x()));
      chevron_.setAltText("Switch to tab");
      chevron_.getElement().getStyle().setCursor(Cursor.POINTER);
      chevron_.addClickHandler(event -> tabOverflowPopup_.showRelativeTo(chevron_));

      panel_.add(chevron_);
      panel_.setWidgetTopHeight(chevron_,
                                8, Unit.PX,
                                chevron_.getHeight(), Unit.PX);
      panel_.setWidgetRightWidth(chevron_,
                                 52, Unit.PX,
                                 chevron_.getWidth(), Unit.PX);
      
      tabPanel_.addTabClosingHandler(this);
      tabPanel_.addTabCloseHandler(this);
      tabPanel_.addTabClosedHandler(this);
      tabPanel_.addTabReorderHandler(this);

      return panel_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      Scheduler.get().scheduleDeferred(() -> onResize());

      addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> event)
         {
            if (activeEditor_ != null)
               activeEditor_.onDeactivate();
      
            activeEditor_ = null;
      
            if (event.getSelectedItem() >= 0)
            {
               activeEditor_ = editors_.get(event.getSelectedItem());
               activeEditor_.onActivate();
               // let any listeners know this tab was activated
               events_.fireEvent(new DocTabActivatedEvent(
                     activeEditor_.getPath(),
                     activeEditor_.getId()));
      
               // don't send focus to the tab if we're expecting a debug selection
               // event
               if (source_.getInitialized() && !isDebugSelectionPending())
               {
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     public void execute()
                     {
                        // presume that we will give focus to the tab
                        boolean focus = true;
      
                        if (event instanceof DocTabSelectionEvent)
                        {
                           // however, if this event was generated from a doc tab
                           // selection that did not have focus, don't steal focus
                           DocTabSelectionEvent tabEvent = (DocTabSelectionEvent) event;
                           focus = tabEvent.getFocus();
                        }
      
                        if (focus && activeEditor_ != null)
                           activeEditor_.focus();
                     }
                  });
               }
               else if (isDebugSelectionPending())
               {
                  // we're debugging, so send focus to the console instead of the
                  // editor
                  commands_.activateConsole().execute();
                  clearPendingDebugSelection();
               }
            }
      
            if (source_.getInitialized())
               source_.manageCommands(false);
         }
      });
   }

   // overridden Display methods

   @Override
   public void setSource(Source source)
   {
      source_ = source;
   }

   public String setName(String name)
   {
      if (StringUtil.isNullOrEmpty(name_))
         name_ = name;

      // return the name in case name_ was not empty
      return name_;
   }

   @Override
   public void addEditor(EditingTarget target)
   {
      if (editors_.contains(target))
         Debug.logToConsole("Trying to add editor we already have");
      else
         editors_.add(target);
   }

   @Override
   public void addEditor(Integer position, EditingTarget target)
   {
      if (editors_.contains(target))
         Debug.logToConsole("Trying to add editor we already have");
      else
         editors_.add(position, target);
   }


   @Override
   public EditingTarget addTab(SourceDocument doc, boolean atEnd,
         int mode)
   {
      // by default, add at the tab immediately after the current tab
      return addTab(doc, atEnd ? null : getPhysicalTabIndex() + 1,
            mode);
   }

   @Override
   public void addTab(Widget widget,
                      FileIcon icon,
                      String docId,
                      String name,
                      String tooltip,
                      Integer position,
                      boolean switchToTab)
   {
      tabPanel_.add(widget, icon, docId, name, tooltip, position);
      if (switchToTab)
         tabPanel_.selectTab(widget);
   }

   @Override
   public String getName()
   {
      return name_;
   }

   @Override
   public int getTabCount()
   {
      return tabPanel_.getWidgetCount();
   }

   private int getActiveTabIndex()
   {
      return tabPanel_.getSelectedIndex();
   }

   public boolean hasDoc(String docId)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(docId, target.getId()))
            return true;
      }
      return false;
   }

   public boolean hasDocWithPath(String path)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(path, target.getPath()))
            return true;
      }
      return false;
   }

   public EditingTarget getEditorWithPath(String path)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(path, target.getPath()))
            return target;
      }
      return null;
   }

   public void initialSelect(int index)
   {
      if (index >= 0 && getTabCount() > index)
         selectTab(index);
      if (getTabCount() > 0 && getActiveTabIndex() >= 0)
         editors_.get(index).onInitiallyLoaded();
   }

   @Override
   public void selectTab(int tabIndex)
   {
      tabPanel_.selectTab(tabIndex);
   }

   @Override
   public void selectTab(Widget child)
   {
      tabPanel_.selectTab(child);
   }

   @Override
   public void moveTab(int index, int delta)
   {
      tabPanel_.moveTab(index, delta);
   }

   @Override
   public void renameTab(Widget child,
                         FileIcon icon,
                         String value,
                         String tooltip)
   {
      tabPanel_.replaceDocName(tabPanel_.getWidgetIndex(child),
                               icon,
                               value,
                               tooltip);
   }

   @Override
   public void setDirty(Widget widget, boolean dirty)
   {
      Widget tab = tabPanel_.getTabWidget(widget);
      if (dirty)
         tab.addStyleName(ThemeStyles.INSTANCE.dirtyTab());
      else
         tab.removeStyleName(ThemeStyles.INSTANCE.dirtyTab());
   }

   @Override
   public void setActiveEditor(int index)
   {
      setActiveEditor(editors_.get(index));
   }

   @Override
   public void setActiveEditor(EditingTarget target)
   {
      activeEditor_ = target;
      if (!editors_.contains(activeEditor_))
      {
         addEditor(activeEditor_);
         Debug.logWarning("activeEditor_ set to unknown editor, editor added to list");
      }
   }

   public void closeTabByDocId(String docId, boolean interactive)
   {
      suspendDocumentClose_ = true;
      for (int i = 0; i < editors_.size(); i++)
      {
         if (editors_.get(i).getId() == docId)
         {
            closeTab(i, interactive, null);
            break;
         }
      }
      suspendDocumentClose_ = false;
   }

   @Override
   public void closeTab(boolean interactive)
   {
      closeTab(getActiveTabIndex(), interactive, null);
   }

   @Override
   public void closeTab(Widget child, boolean interactive)
   {
      closeTab(child, interactive, null);
   }

   @Override
   public void closeTab(Widget child, boolean interactive, Command onClosed)
   {
      closeTab(tabPanel_.getWidgetIndex(child), interactive, onClosed);
   }
   
   @Override
   public void showUnsavedChangesDialog(
         String title,
         ArrayList<UnsavedChangesTarget> dirtyTargets,
         OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
         Command onCancelled)
   {
      new UnsavedChangesDialog(title, 
                               dirtyTargets,
                               saveOperation,
                               onCancelled).showModal();
   }

   @Override
   public void manageChevronVisibility()
   {
      int tabsWidth = tabPanel_.getTabsEffectiveWidth();
      setOverflowVisible(tabsWidth > getOffsetWidth() - 50);
   }

   public void showOverflowPopup()
   {
      setOverflowVisible(true);
      tabOverflowPopup_.showRelativeTo(chevron_);
   }
   
   @Override
   public void cancelTabDrag()
   {
      tabPanel_.cancelTabDrag();
   }

   public void ensureVisible()
   {
      events_.fireEvent(new EnsureVisibleEvent(true));
   }

   @Override
   public HandlerRegistration addTabClosingHandler(TabClosingHandler handler)
   {
      return tabPanel_.addTabClosingHandler(handler);
   }

   @Override
   public HandlerRegistration addBeforeShowHandler(BeforeShowHandler handler)
   {
      return addHandler(handler, BeforeShowEvent.TYPE);
   }

   // event handlers 
 
   @Override
   public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler)
   {
      return tabPanel_.addBeforeSelectionHandler(handler);
   }

   @Override
   public void onTabReorder(TabReorderEvent event)
   {
      syncTabOrder();

      // sanity check: make sure we're moving from a valid location and to a
      // valid location
      if (event.getOldPos() < 0 || event.getOldPos() >= tabOrder_.size() ||
          event.getNewPos() < 0 || event.getNewPos() >= tabOrder_.size())
      {
         return;
      }

      // remove the tab from its old position
      int idx = tabOrder_.get(event.getOldPos());
      tabOrder_.remove(new Integer(idx));  // force type box

      // add it to its new position
      tabOrder_.add(event.getNewPos(), idx);

      // sort the document IDs and send to the server
      ArrayList<String> ids = new ArrayList<String>();
      for (int i = 0; i < tabOrder_.size(); i++)
      {
         ids.add(editors_.get(tabOrder_.get(i)).getId());
      }
      source_.getServer().setDocOrder(ids, new VoidServerRequestCallback());

      // activate the tab
      setPhysicalTabIndex(event.getNewPos());

      source_.syncTabOrder();
      source_.fireDocTabsChanged();
   }

   @Override
   public void onTabClose(TabCloseEvent event)
   {
      // can't proceed if there is no active editor or display
      if (activeEditor_ == null)
         return;

      if (event.getTabIndex() >= editors_.size())
         return; // Seems like this should never happen...?

      final String activeEditorId = activeEditor_.getId();

      // !!! THIS NEEDS TO BE DONE IN SOURCE.JAVA
      if (editors_.get(event.getTabIndex()).getId() == activeEditorId)
      {
         // scan the source navigation history for an entry that can
         // be used as the next active tab (anything that doesn't have
         // the same document id as the currently active tab)
         SourceNavigation srcNav = sourceNavigationHistory_.scanBack(
               new SourceNavigationHistory.Filter()
               {
                  public boolean includeEntry(SourceNavigation navigation)
                  {
                     return navigation.getDocumentId() != activeEditorId;
                  }
               });

         // see if the source navigation we found corresponds to an active
         // tab -- if it does then set this on the event
         if (srcNav != null)
         {
            for (int i=0; i<editors_.size(); i++)
            {
               if (srcNav.getDocumentId() == editors_.get(i).getId())
               {
                  selectTab(i);
                  break;
               }
            }
         }
      }
   }

   @Override
   public void onMaximizeSourceWindow(MaximizeSourceWindowEvent e)
   {
      events_.fireEvent(new EnsureVisibleEvent());
      events_.fireEvent(new EnsureHeightEvent(EnsureHeightEvent.MAXIMIZED));
   }

   @Override
   public void onEnsureVisibleSourceWindow(EnsureVisibleSourceWindowEvent e)
   {
      if (getTabCount() > 0)
      {
         events_.fireEvent(new EnsureVisibleEvent());
         events_.fireEvent(new EnsureHeightEvent(EnsureHeightEvent.NORMAL));
      }
   }

   // public methods

   @Override
   public void onTabClosing(final TabClosingEvent event)
   {
      EditingTarget target = editors_.get(event.getTabIndex());
      if (!target.onBeforeDismiss())
         event.cancel();
   }

   @Override
   public void onTabClosed(TabClosedEvent event)
   {
      closeTabIndex(event.getTabIndex(), !suspendDocumentClose_);
   }

   public void addToPanel(Widget w)
   {
      panel_.add(w);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return tabPanel_.addSelectionHandler(handler);
   }

   public Widget asWidget()
   {
      ensureVisible();
      return this;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }
   
   public HandlerRegistration addEnsureHeightHandler(
         EnsureHeightHandler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public void onResize()
   {
      panel_.onResize();
      manageChevronVisibility();
   }

   public void onBeforeShow()
   {
      for (Widget w : panel_)
         if (w instanceof BeforeShowCallback)
            ((BeforeShowCallback)w).onBeforeShow();
      events_.fireEvent(new BeforeShowEvent());
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (getActiveTabIndex() >= 0)
      {
         Widget w = tabPanel_.getTabWidget(getActiveTabIndex());
         if (w instanceof RequiresVisibilityChanged)
            ((RequiresVisibilityChanged)w).onVisibilityChanged(visible);
      }
   }
   
   // private methods

   private void closeTab(int index, boolean interactive, Command onClosed)
   {
      if (index < 0)
         return;

      if (interactive)
      {
         tabPanel_.tryCloseTab(index, onClosed);
      }
      else
      {
         editors_.remove(index);
         tabPanel_.closeTab(index, onClosed);
      }
   }

   private void closeTabIndex(int idx, boolean closeDocument)
   {
      EditingTarget target = editors_.remove(idx);
      source_.syncClosedTab(target);

      tabOrder_.remove(new Integer(idx));
      for (int i = 0; i < tabOrder_.size(); i++)
      {
         if (tabOrder_.get(i) > idx)
         {
            tabOrder_.set(i, tabOrder_.get(i) - 1);
         }
      }

      target.onDismiss(closeDocument ? EditingTarget.DISMISS_TYPE_CLOSE :
         EditingTarget.DISMISS_TYPE_MOVE);
      source_.closeEditorIfActive(target);

      if (closeDocument)
      {
         events_.fireEvent(new DocTabClosedEvent(target.getId()));
         source_.getServer().closeDocument(target.getId(),
                               new VoidServerRequestCallback());
      }

      source_.manageCommands(false);
      source_.fireDocTabsChanged();

      if (getTabCount() == 0)
      {
         sourceNavigationHistory_.clear();
         events_.fireEvent(new LastSourceDocClosedEvent(getName()));
      }
   }

   private void setOverflowVisible(boolean visible)
   {
      utilPanel_.setVisible(visible);
      chevron_.setVisible(visible);
   }

   public boolean isDebugSelectionPending()
   {
      return debugSelectionTimer_ != null;
   }

   private void clearPendingDebugSelection()
   {
      if (debugSelectionTimer_ != null)
      {
         debugSelectionTimer_.cancel();
         debugSelectionTimer_ = null;
      }
   }

   // !!! shouldn't be public or override
   @Override
   public void setPendingDebugSelection()
   {
      if (!isDebugSelectionPending())
      {
         debugSelectionTimer_ = new Timer()
         {
            public void run()
            {
               debugSelectionTimer_ = null;
            }
         };
         debugSelectionTimer_.schedule(250);
      }
   }

   @Override
   public EditingTarget addTab(SourceDocument doc, int mode)
   {
      final String defaultNamePrefix = source_.getEditingTargetSource().getDefaultNamePrefix(doc);
      final EditingTarget target = source_.getEditingTargetSource().getEditingTarget(
            doc, source_.getFileContext(), new Provider<String>()
            {
               public String get()
               {
                  return source_.getNextDefaultName(defaultNamePrefix);
               }
            });
      final Widget widget = createWidget(target);

      Integer position = getActiveTabIndex() + 1;

      if (position == null)
      {
         addEditor(target);
      }
      else
      {
         // we're inserting into an existing permuted tabset -- push aside
         // any tabs physically to the right of this tab
         addEditor(position, target);
         source_.addEditor(target);
         for (int i = 0; i < tabOrder_.size(); i++)
         {
            int pos = tabOrder_.get(i);
            if (pos >= position)
               tabOrder_.set(i, pos + 1);
         }
   
         // add this tab in its "natural" position
         tabOrder_.add(position, position);
      }

      addTab(widget,
             target.getIcon(),
             target.getId(),
             target.getName().getValue(),
             target.getTabTooltip(), // used as tooltip, if non-null
             position,
             true);
      source_.fireDocTabsChanged();

      target.getName().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> event)
         {
            renameTab(widget,
                      target.getIcon(),
                      event.getValue(),
                      target.getPath());
            source_.fireDocTabsChanged();
         }
      });

      setDirty(widget, target.dirtyState().getValue());
      target.dirtyState().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            setDirty(widget, event.getValue());
            source_.manageCommands(false);
         }
      });

      target.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            selectTab(widget);
         }
      });

      target.addCloseHandler(new CloseHandler<Void>()
      {
         public void onClose(CloseEvent<Void> voidCloseEvent)
         {
            closeTab(widget, false);
         }
      });

      events_.fireEvent(new SourceDocAddedEvent(doc, mode, getName()));

      if (target instanceof TextEditingTarget && doc.isReadOnly())
      {
         ((TextEditingTarget) target).setIntendedAsReadOnly(
            JsUtil.toList(doc.getReadOnlyAlternatives()));
      }

      // adding a tab may enable commands that are only available when
      // multiple documents are open; if this is the second document, go check
      if (editors_.size() == 2)
         source_.manageMultiTabCommands();

      // if the target had an editing session active, attempt to resume it
      if (doc.getCollabParams() != null)
         target.beginCollabSession(doc.getCollabParams());

      return target;
   }

   private Widget createWidget(EditingTarget target)
   {
      return target.asWidget();
   }

   private boolean suspendDocumentClose_ = false;

   private String name_;
   private Source source_;

   private DocTabLayoutPanel tabPanel_;
   private HTML utilPanel_;
   private Image chevron_;
   private LayoutPanel panel_;
   private PopupPanel tabOverflowPopup_;

   private EventBus events_;
   private Commands commands_;

   EditingTarget activeEditor_;
   ArrayList<EditingTarget> editors_ = new ArrayList<EditingTarget>();
   ArrayList<Integer> tabOrder_ = new ArrayList<Integer>();

   private Timer debugSelectionTimer_ = null;
   private final SourceNavigationHistory sourceNavigationHistory_ =
                                              new SourceNavigationHistory(30);
   public final static int OPEN_INTERACTIVE = 0;
}
