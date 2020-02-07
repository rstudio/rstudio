/*
 * PanmirrorEditorWidget.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror;


import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.events.MouseDragHandler;
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.core.client.promise.PromiseWithProgress;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DockPanelSidebarDragHandler;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.IsHideableWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;
import org.rstudio.studio.client.panmirror.command.PanmirrorToolbar;
import org.rstudio.studio.client.panmirror.findreplace.PanmirrorFindReplace;
import org.rstudio.studio.client.panmirror.findreplace.PanmirrorFindReplaceWidget;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineNavigationEvent;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineVisibleEvent;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineWidthEvent;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineWidget;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.theme.PanmirrorTheme;
import org.rstudio.studio.client.panmirror.theme.PanmirrorThemeCreator;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.HasSelectionChangedHandlers;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


public class PanmirrorWidget extends DockLayoutPanel implements 
   IsHideableWidget,
   RequiresResize, 
   HasChangeHandlers, 
   HasSelectionChangedHandlers,
   PanmirrorOutlineVisibleEvent.HasPanmirrorOutlineVisibleHandlers,
   PanmirrorOutlineWidthEvent.HasPanmirrorOutlineWidthHandlers
   
{
   
   public static class Options 
   {
      public boolean toolbar = true;
      public boolean outline = false;
      public double outlineWidth = 190;
      public boolean border = false;
   }
   
   public static void create(PanmirrorConfig config,
                             Options options,
                             CommandWithArg<PanmirrorWidget> completed) {
      
      PanmirrorWidget editorWidget = new PanmirrorWidget(options);
      
      Panmirror.load(() -> {
         new PromiseWithProgress<PanmirrorEditor>(
            PanmirrorEditor.create(editorWidget.editorParent_.getElement(), config),
            null,
            editor -> {
               editorWidget.attachEditor(editor);
               completed.execute(editorWidget);
            }
         );
       });  
   }
   
   private PanmirrorWidget(Options options)
   {
      super(Style.Unit.PX);
      setSize("100%", "100%");   
     
      // styles
      if (options.border)
         this.addStyleName(ThemeResources.INSTANCE.themeStyles().borderedIFrame());
     
      // toolbar
      toolbar_ =  new PanmirrorToolbar();
      addNorth(toolbar_, toolbar_.getHeight());
      setWidgetHidden(toolbar_, !options.toolbar);
      
      
      // find replace
      findReplace_ = new PanmirrorFindReplaceWidget(new PanmirrorFindReplaceWidget.Container()
      {
         @Override
         public boolean isFindReplaceShowing()
         {
            return findReplaceShowing_;
         }
         @Override
         public void showFindReplace(boolean show)
         {
            findReplaceShowing_ = show;
            setWidgetHidden(findReplace_, !findReplaceShowing_);
            toolbar_.setFindReplaceLatched(findReplaceShowing_);
            if (findReplaceShowing_)
               findReplace_.performFind();
            else
               editor_.getFindReplace().clear();
         }
         @Override
         public PanmirrorFindReplace getFindReplace()
         {
            return editor_.getFindReplace();
         } 
      });
      addNorth(findReplace_, findReplace_.getHeight());
      setWidgetHidden(findReplace_, true);
      
      // outline
      outline_ = new PanmirrorOutlineWidget();
      addEast(outline_, options.outlineWidth);
      setWidgetSize(outline_, options.outline ? options.outlineWidth : 0);
      MouseDragHandler.addHandler(
         outline_.getResizer(),
         new DockPanelSidebarDragHandler(this, outline_) {
            @Override
            public void onResized(boolean visible)
            {
               // hide if we snapped to 0 width
               if (!visible)
                  showOutline(false, 0);
               
               // notify editor for layout
               PanmirrorWidget.this.onResize();
            }
            @Override
            public void onPreferredWidth(double width) 
            {
               PanmirrorOutlineWidthEvent.fire(PanmirrorWidget.this, width);
            }
            @Override
            public void onPreferredVisibility(boolean visible) 
            {
               PanmirrorOutlineVisibleEvent.fire(PanmirrorWidget.this, visible);
            }
         }
      );
      
      RStudioGinjector.INSTANCE.injectMembers(this);
     
      // editor
      editorParent_ = new HTML();
      add(editorParent_);
   }
   
   @Inject
   public void initialize(UserPrefs userPrefs, 
                          UserState userState, 
                          EventBus events)
   {
      userPrefs_ = userPrefs;
      userState_ = userState;
      registrations_.add(events.addHandler(EditorThemeChangedEvent.TYPE, 
         (EditorThemeChangedEvent event) -> {
            new Timer()
            {
               @Override
               public void run()
               {
                  toolbar_.sync(true);
                  syncEditorTheme(event.getTheme());
               }
            }.schedule(150);
      }));
      registrations_.add(events.addHandler(ChangeFontSizeEvent.TYPE, (event) -> {
         syncEditorTheme();
      }));
   }
   
   private void attachEditor(PanmirrorEditor editor) {
      
      editor_ = editor;
       
      // sync theme
      syncEditorTheme();
      
      
      commands_ = editor.commands();
      
      toolbar_.init(commands_, findReplace_);
      
      outline_.addPanmirrorOutlineNavigationHandler(new PanmirrorOutlineNavigationEvent.Handler() {
         @Override
         public void onPanmirrorOutlineNavigation(PanmirrorOutlineNavigationEvent event)
         {
            editor_.navigate(event.getId());
            editor_.focus();
         }
      });
      
      
      editorEventUnsubscribe_.add(editor_.subscribe(Panmirror.EditorEvents.Update, () -> {
         
         // fire to clients
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(), handlers_);
      
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(Panmirror.EditorEvents.SelectionChange, () -> {
         
         // sync toolbar commands
         if (toolbar_ != null)
            toolbar_.sync(false);
         
         // sync outline
         outline_.updateSelection(editor_.getSelection());
         
         // fire to clients
         SelectionChangeEvent.fire(this);
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(Panmirror.EditorEvents.OutlineChange, () -> {
         
         // sync outline
         outline_.updateOutline(editor_.getOutline());
             
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(Panmirror.EditorEvents.Focus, () -> {
         editor_.getFindReplace().clear();
      }));
   }
   
   @Override
   public void onDetach()
   {
      try 
      {
         // detach registrations (outline events)
         registrations_.removeHandler();
         
         if (editor_ != null) 
         {
            // unsubscribe from editor events
            for (JsVoidFunction unsubscribe : editorEventUnsubscribe_) 
               unsubscribe.call();
            editorEventUnsubscribe_.clear();
              
            // destroy editor
            editor_.destroy();
            editor_ = null;
         }
      }
      finally
      {
         super.onDetach();
      }
   }
   
   public void setTitle(String title)
   {
      editor_.setTitle(title);
   }
   
   public String getTitle()
   {
      return editor_.getTitle();
   }
   
  
   
   public void setMarkdown(String markdown, boolean emitUpdate, CommandWithArg<Boolean> completed) 
   {
      new PromiseWithProgress<Boolean>(
         editor_.setMarkdown(markdown, emitUpdate),
         false,
         completed
      );
   }
   
   public void getMarkdown(PanmirrorWriterOptions options, CommandWithArg<String> completed) {
      new PromiseWithProgress<String>(
         editor_.getMarkdown(options),
         null,
         completed   
      );
   }
   
   public HasFindReplace getFindReplace()
   {
      return findReplace_;
   }
   
   public void showOutline(boolean show, double width)
   {
      showOutline(show, width, false);
   }
   
   public void showOutline(boolean show, double width, boolean animate)
   {
      boolean visible = getWidgetSize(outline_) > 0;
      if (show != visible)
      {
         setWidgetSize(outline_, show ? width : 0);
         outline_.setAriaVisible(show);
         if (animate)
         {
            int duration = (userPrefs_.reducedMotion().getValue() ? 0 : 500);
            animate(duration);
         }
         else
         {
            forceLayout();
         }
      }
   }
   
   public void showToolbar(boolean show)
   {
      setWidgetHidden(toolbar_, !show);
   }
  
   
   public PanmirrorCommand[] getCommands()
   {
      return commands_;
   }
  
   public boolean execCommand(String id)
   {
      for (PanmirrorCommand command : commands_)
      {
         if (command.id == id)
         {
            if (command.isEnabled())
            {
               command.execute();
            }
            return true;
          }
      }
      return false;
   }
   
   
   public void navigate(String id)
   {
      editor_.navigate(id);
   }
   
   public void setKeybindings(PanmirrorKeybindings keybindings) 
   {
      editor_.setKeybindings(keybindings);
      commands_ = editor_.commands();
      toolbar_.init(commands_, findReplace_);
   }
   
   public String getHTML()
   {
      return editor_.getHTML();
   }
   
   public PanmirrorPandocFormat getPandocFormat()
   {
      return editor_.getPandocFormat();
   }
   
   public PanmirrorSelection getSelection()
   {
      return editor_.getSelection();
   }
   
   public PanmirrorEditingLocation getEditingLocation()
   {
      return editor_.getEditingLocation();
   }
   
   public void restoreEditingLocation(PanmirrorEditingLocation location)
   {
      editor_.restoreEditingLocation(location);
   }
   
   public void focus()
   {
      editor_.focus();
   }
   
   public void blur()
   {
      editor_.blur();
   }
   
   public void enableDevTools() 
   { 
      ProseMirrorDevTools.load(() -> {
         editor_.enableDevTools(ProseMirrorDevTools.applyDevTools);
      });
   }
   
   @Override
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return handlers_.addHandler(ChangeEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler)
   {
      return handlers_.addHandler(SelectionChangeEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addPanmirrorOutlineWidthHandler(PanmirrorOutlineWidthEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorOutlineWidthEvent.getType(), handler);
   }

   @Override
   public HandlerRegistration addPanmirrorOutlineVisibleHandler(PanmirrorOutlineVisibleEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorOutlineVisibleEvent.getType(), handler);
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }
  

   @Override
   public void onResize()
   {
      if (toolbar_ != null) {
         toolbar_.onResize();
      }
      if (findReplace_ != null) {
         findReplace_.onResize();
      }
      if (editor_ != null) {
         editor_.resize();
      }
   }
   
   private void syncEditorTheme()
   {
      syncEditorTheme(userState_.theme().getGlobalValue().cast());
   }
   
   private void syncEditorTheme(AceTheme theme)
   {
      PanmirrorTheme panmirrorTheme = PanmirrorThemeCreator.themeFromEditorTheme(theme, userPrefs_);
      editor_.applyTheme(panmirrorTheme);;
   }
   
   
   private UserPrefs userPrefs_ = null;
   private UserState userState_ = null;
   
   private PanmirrorToolbar toolbar_ = null;
   private boolean findReplaceShowing_ = false;
   private PanmirrorFindReplaceWidget findReplace_ = null;
   private PanmirrorOutlineWidget outline_ = null;
   private HTML editorParent_ = null;
   
   private PanmirrorEditor editor_ = null;
   private PanmirrorCommand[] commands_ = null;
   
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final ArrayList<JsVoidFunction> editorEventUnsubscribe_ = new ArrayList<JsVoidFunction>();
}


@JsType(isNative = true, namespace = JsPackage.GLOBAL)
class ProseMirrorDevTools
{
   @JsOverlay
   public static void load(ExternalJavaScriptLoader.Callback onLoaded) {    
      devtoolsLoader_.addCallback(onLoaded);
   }
   
   public static JsObject applyDevTools;
 
   @JsOverlay
   private static final ExternalJavaScriptLoader devtoolsLoader_ =
     new ExternalJavaScriptLoader("js/panmirror/prosemirror-dev-tools.min.js");
}





