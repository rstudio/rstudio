/*
 * PanmirrorEditorWidget.java
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

package org.rstudio.studio.client.panmirror;


import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
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
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.panmirror.command.PanmirrorMenuItem;
import org.rstudio.studio.client.panmirror.command.PanmirrorToolbar;
import org.rstudio.studio.client.panmirror.command.PanmirrorToolbarCommands;
import org.rstudio.studio.client.panmirror.command.PanmirrorToolbarMenu;
import org.rstudio.studio.client.panmirror.events.PanmirrorOutlineNavigationEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorOutlineVisibleEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorFindReplaceVisibleEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorFindReplaceVisibleEvent.Handler;
import org.rstudio.studio.client.panmirror.events.PanmirrorOutlineWidthEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorStateChangeEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorUpdatedEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorNavigationEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorFocusEvent;
import org.rstudio.studio.client.panmirror.findreplace.PanmirrorFindReplace;
import org.rstudio.studio.client.panmirror.findreplace.PanmirrorFindReplaceWidget;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocation;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineItem;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineWidget;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorSpellingDoc;
import org.rstudio.studio.client.panmirror.theme.PanmirrorTheme;
import org.rstudio.studio.client.panmirror.theme.PanmirrorThemeCreator;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;


public class PanmirrorWidget extends DockLayoutPanel implements 
   IsHideableWidget,
   RequiresResize, 
   CommandPaletteEntrySource,
   PanmirrorUpdatedEvent.HasPanmirrorUpdatedHandlers,
   PanmirrorStateChangeEvent.HasPanmirrorStateChangeHandlers,
   PanmirrorOutlineVisibleEvent.HasPanmirrorOutlineVisibleHandlers,
   PanmirrorOutlineWidthEvent.HasPanmirrorOutlineWidthHandlers,
   PanmirrorFindReplaceVisibleEvent.HasPanmirrorFindReplaceVisibleHandlers,
   PanmirrorNavigationEvent.HasPanmirrorNavigationHandlers,
   PanmirrorFocusEvent.HasPanmirrorFocusHandlers
   
{
   
   public static class Options 
   {
      public boolean toolbar = true;
      public boolean outline = false;
      public double outlineWidth = 190;
      public boolean border = false;
   }
   
   public static interface FormatSource
   {
      PanmirrorFormat getFormat(PanmirrorUIToolsFormat formatTools);
   }
   
   public static void create(PanmirrorContext context,
                             FormatSource formatSource,
                             PanmirrorOptions options,
                             Options widgetOptions,
                             int progressDelay,
                             CommandWithArg<PanmirrorWidget> completed) {
      
      PanmirrorWidget editorWidget = new PanmirrorWidget(widgetOptions);
   
      Panmirror.load(() -> {
         
         // get format (now that we have uiTools available)
         PanmirrorFormat format = formatSource.getFormat(new PanmirrorUITools().format);
               
         // create the editor
         new PromiseWithProgress<PanmirrorEditor>(
            PanmirrorEditor.create(editorWidget.editorParent_.getElement(), context, format, options),
            null,
            progressDelay,
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
            
            PanmirrorFindReplaceVisibleEvent.fire(PanmirrorWidget.this, findReplaceShowing_);
            
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
      events_ = events;
   }
   
   private void attachEditor(PanmirrorEditor editor) {
      
      editor_ = editor;
       
      // initialize css
      syncEditorTheme();
      syncContentWidth();
         
      commands_ = new PanmirrorToolbarCommands(editor.commands());
      
      toolbar_.init(commands_, editor_.getMenus(), null);
      
      outline_.addPanmirrorOutlineNavigationHandler(new PanmirrorOutlineNavigationEvent.Handler() {
         @Override
         public void onPanmirrorOutlineNavigation(PanmirrorOutlineNavigationEvent event)
         {
            editor_.navigate(PanmirrorNavigationType.Id, event.getId(), true);
            editor_.focus();
         }
      });
      
      editorEventUnsubscribe_.add(editor_.subscribe(PanmirrorEvent.Update, (data) -> {
         fireEvent(new PanmirrorUpdatedEvent());
      }));
      
      // don't update outline eaglerly (wait for 500ms delay in typing)
      DebouncedCommand updateOutineOnIdle = new DebouncedCommand(500)
      {
         @Override
         protected void execute()
         {
            updateOutline();
         }
      };
      
      // don't sync ui eagerly (wait for 300ms delay in typing)
      DebouncedCommand syncUI = new DebouncedCommand(300) {

         @Override
         protected void execute()
         {
            if (editor_ != null)
            {
               // sync toolbar commands
               if (toolbar_ != null)
                  toolbar_.sync(false);
               
               // sync outline selection
               outline_.updateSelection(editor_.getSelection()); 
            }
         }
         
      };
      
      editorEventUnsubscribe_.add(editor_.subscribe(PanmirrorEvent.StateChange, (data) -> {
         
         // sync ui (debounced)
         syncUI.nudge();
         
         // fire to clients
         fireEvent(new PanmirrorStateChangeEvent());
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(PanmirrorEvent.OutlineChange, (data) -> {
         
         // sync outline
         updateOutineOnIdle.nudge();
         
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(PanmirrorEvent.Navigate, (data) -> {
         
         PanmirrorNavigation nav = Js.uncheckedCast(data);
         fireEvent(new PanmirrorNavigationEvent(nav));  
      }));
      
      editorEventUnsubscribe_.add(editor_.subscribe(PanmirrorEvent.Focus, (data) -> {
         fireEvent(new PanmirrorFocusEvent());
      }));
      
      registrations_.add(events_.addHandler(EditorThemeChangedEvent.TYPE, 
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
      
      registrations_.add(events_.addHandler(ChangeFontSizeEvent.TYPE, (event) -> {
         syncEditorTheme();
      }));
      
      registrations_.add(
         userPrefs_.visualMarkdownEditingMaxContentWidth().addValueChangeHandler((event) -> {
         syncContentWidth();
      }));
      
      registrations_.add(
         userPrefs_.visualMarkdownEditingFontSizePoints().addValueChangeHandler((event) -> {
            syncEditorTheme();
         })
      );   
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
   
  
   
   public void setMarkdown(String code, 
                           PanmirrorWriterOptions options, 
                           boolean emitUpdate, 
                           int progressDelay,
                           CommandWithArg<JsObject> completed) 
   {
      new PromiseWithProgress<JsObject>(
         editor_.setMarkdown(code, options, emitUpdate),
         null,
         progressDelay,
         completed
      );
   }
   
   public void getMarkdown(PanmirrorWriterOptions options, int progressDelay, CommandWithArg<JsObject> completed) {
      new PromiseWithProgress<JsObject>(
         editor_.getMarkdown(options),
         null,
         progressDelay,
         completed   
      );
   }
   
   public void getCanonical(String code, PanmirrorWriterOptions options, int progressDelay, CommandWithArg<String> completed)
   {
      new PromiseWithProgress<String>(
         editor_.getCanonical(code, options),
         null,
         progressDelay,
         completed   
      );
   }
   
   public boolean isInitialDoc()
   {
      return editor_.isInitialDoc();
   }
   
   public HasFindReplace getFindReplace()
   {
      return findReplace_;
   }
   
   public PanmirrorSpellingDoc getSpellingDoc()
   {
      return editor_.getSpellingDoc();
   }
   
   public void spellingInvalidateAllWords()
   {
      if (editor_ != null)
         editor_.spellingInvalidateAllWords();
   }
   
   public void spellingInvalidateWord(String word)
   {
      if (editor_ != null)
         editor_.spellingInvalidateWord(word);
   }
   
   public void showOutline(boolean show, double width)
   {
      showOutline(show, width, false);
   }
   
   public void showOutline(boolean show, double width, boolean animate)
   {
      // update outline if we are showing
      if (show)
         updateOutline();
      
      boolean visible = getWidgetSize(outline_) > 0;
      if (show != visible)
      {
         setWidgetSize(outline_, show ? width : 0);
         outline_.setAriaVisible(show);
         if (animate)
         {
            int duration = (userPrefs_.reducedMotion().getValue() ? 0 : 500);
            animate(duration, new AnimationCallback() {
               @Override
               public void onAnimationComplete()
               {
                  resizeEditor();
               }
               @Override
               public void onLayout(Layer layer, double progress) 
               {
                  resizeEditor();
               }
            });
         }
         else
         {
            forceLayout();
            resizeEditor();
         }
      }
   }
   
   public void showToolbar(boolean show)
   {
      setWidgetHidden(toolbar_, !show);
   }
   
   public void insertChunk(String chunkPlaceholder, int rowOffset, int colOffset)
   {
      editor_.insertChunk(chunkPlaceholder, rowOffset, colOffset);
   }
  
   public boolean execCommand(String id)
   {
      return commands_.exec(id);
   }
   
   public void navigate(String type, String location, boolean recordCurrent)
   {
      // perform navigation
      editor_.navigate(type, location, recordCurrent);
   }
   
   public void setKeybindings(PanmirrorKeybindings keybindings) 
   {
      editor_.setKeybindings(keybindings);
      commands_ = new PanmirrorToolbarCommands(editor_.commands());
      toolbar_.init(commands_, editor_.getMenus(), null);
   }
   
   public String getHTML()
   {
      return editor_.getHTML();
   }
   
   public PanmirrorFormat getEditorFormat()
   {
      return editor_.getEditorFormat();
   }
   
   public PanmirrorPandocFormat getPandocFormat()
   {
      return editor_.getPandocFormat();
   }
   
   public PanmirrorPandocFormatConfig getPandocFormatConfig(boolean isRmd)
   {
      return editor_.getPandocFormatConfig(isRmd);
   }
   
   public String getSelectedText()
   {
      return editor_.getSelectedText();
   }
   
   public void replaceSelection(String value)
   {
      editor_.replaceSelection(value);
   }
   
   public PanmirrorSelection getSelection()
   {
      return editor_.getSelection();
   }
   
   public PanmirrorEditingLocation getEditingLocation()
   {
      return editor_.getEditingLocation();
   }
   
   public void setEditingLocation(
      PanmirrorEditingOutlineLocation outlineLocation, 
      PanmirrorEditingLocation previousLocation) 
   {
      editor_.setEditingLocation(outlineLocation, previousLocation);
   }
   
   public void focus()
   {
      editor_.focus();
   }
   
   public void blur()
   {
      editor_.blur();
   }
   
   public Promise<Boolean> showContextMenu(PanmirrorMenuItem[] items, int clientX, int clientY)
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         
         final PanmirrorToolbarMenu menu = new PanmirrorToolbarMenu(commands_);
         menu.addCloseHandler((event) -> {
            resolve.onInvoke(true);
         });
         menu.addItems(items);
         menu.setPopupPositionAndShow(new PositionCallback() {
            @Override
            public void setPosition(int offsetWidth, int offsetHeight)
            {
               menu.setPopupPosition(clientX, clientY);
            }
         });
      });
   }
   
   public String getYamlFrontMatter()
   {
      return editor_.getYamlFrontMatter();
   }

   public void applyYamlFrontMatter(String yaml)
   {
      editor_.applyYamlFrontMatter(yaml);
   }
   
   public void activateDevTools() 
   { 
      ProseMirrorDevTools.load(() -> {
         editor_.enableDevTools(ProseMirrorDevTools.applyDevTools);
      });
   }
   
   public boolean devToolsLoaded()
   {
      return ProseMirrorDevTools.isLoaded();
   }
   
   @Override
   public HandlerRegistration addPanmirrorUpdatedHandler(PanmirrorUpdatedEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorUpdatedEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addPanmirrorStateChangeHandler(PanmirrorStateChangeEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorStateChangeEvent.getType(), handler);
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
   public HandlerRegistration addPanmirrorFindReplaceVisibleHandler(Handler handler)
   {
      return handlers_.addHandler(PanmirrorFindReplaceVisibleEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addPanmirrorNavigationHandler(PanmirrorNavigationEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorNavigationEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addPanmirrorFocusHandler(org.rstudio.studio.client.panmirror.events.PanmirrorFocusEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorFocusEvent.getType(), handler);
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
         resizeEditor();
      }
   }

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      return commands_.getCommandPaletteItems();
   } 
   
   private void updateOutline()
   {
      if (editor_ != null) // would be null during teardown of tab
      {
         PanmirrorOutlineItem[] outline = editor_.getOutline();
         outline_.updateOutline(outline);
         outline_.updateSelection(editor_.getSelection());
      }
   }
   
   private void resizeEditor() 
   {
      editor_.resize();
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
   
   private void syncContentWidth()
   {
      int contentWidth = userPrefs_.visualMarkdownEditingMaxContentWidth().getValue();
      editor_.setMaxContentWidth(contentWidth, 20);
   }
   
   
   private UserPrefs userPrefs_ = null;
   private UserState userState_ = null;
   private EventBus events_ = null;
   
   private PanmirrorToolbar toolbar_ = null;
   private boolean findReplaceShowing_ = false;
   private PanmirrorFindReplaceWidget findReplace_ = null;
   private PanmirrorOutlineWidget outline_ = null;
   private HTML editorParent_ = null;
   
   private PanmirrorEditor editor_ = null;
   private PanmirrorToolbarCommands commands_ = null;
   
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final ArrayList<JsVoidFunction> editorEventUnsubscribe_ = new ArrayList<JsVoidFunction>();
}


@JsType(isNative = true, namespace = JsPackage.GLOBAL)
class ProseMirrorDevTools
{
   @JsOverlay
   public static void load(ExternalJavaScriptLoader.Callback onLoaded) 
   {    
      devtoolsLoader_.addCallback(onLoaded);
   }
   
   @JsOverlay
   public static boolean isLoaded() 
   {
      return devtoolsLoader_.isLoaded();
   }
   
   public static JsObject applyDevTools;
 
   @JsOverlay
   private static final ExternalJavaScriptLoader devtoolsLoader_ =
     new ExternalJavaScriptLoader("js/panmirror/prosemirror-dev-tools.min.js");
}





