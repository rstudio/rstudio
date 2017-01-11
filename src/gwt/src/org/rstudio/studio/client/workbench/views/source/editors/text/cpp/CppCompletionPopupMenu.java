package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;

public class CppCompletionPopupMenu extends ScrollableToolbarPopupMenu
{
   CppCompletionPopupMenu(DocDisplay docDisplay, 
                          CompletionPosition completionPosition)
   {  
      docDisplay_ = docDisplay;
      completionPosition_ = completionPosition;
      
      addToolTipHandler();
      
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }
   
   public void setText(String text)
   {
      JsArray<CppCompletion> completions = JsArray.createArray().cast();
      completions.push(CppCompletion.create(text));
      setCompletions(completions, null);
   }
     
   public void setCompletions(JsArray<CppCompletion> completions, 
                              CommandWithArg<CppCompletion> onSelected)
   {
      // save completions and selectable state
      completions_ = completions;
      onSelected_ = onSelected;
      
      // clear existing items
      updatingMenu_ = true;
      menuBar_.clearItems();
      
      // add items (remember first item for programmatic selection)
      MenuItem firstItem = null;
      for (int i = 0; i<completions.length(); i++)
      {
         final CppCompletion completion = completions.get(i);
         
         SafeHtmlBuilder sb = new SafeHtmlBuilder();
         SafeHtmlUtil.appendImage(sb, 
                                  RES.styles().itemImage(), 
                                  completion.getIcon());
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.styles().itemName(), 
                                 completion.getTypedText());   
         
         
         MenuItem menuItem = new MenuItem(sb.toSafeHtml(), 
               new ScheduledCommand() {
            @Override
            public void execute()
            {
               docDisplay_.setFocus(true); 
               if (isSelectable())
                  onSelected_.execute(completion);
            }
         });
         menuItem.addStyleName(RES.styles().itemMenu());
           
         FontSizer.applyNormalFontSize(menuItem);
         
         addItem(menuItem);
         
         if (i == 0)
            firstItem = menuItem;
      }
      updatingMenu_ = false;
      
      
      // select first item
      if (isSelectable() && (firstItem != null))
         selectItem(firstItem);
      
      if (completions.length() > 0)
      {
         showMenu();
      }
      else
      {
         setVisible(false);
         if (toolTip_ != null)
            toolTip_.setVisible(false);
      }
   }
   
   public void acceptSelected()
   {
      int index = getSelectedIndex();
      if (index != -1)
      {
         if (isSelectable())
            onSelected_.execute(completions_.get(index));
      }
      hide();
   }
   
   private void showMenu()
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            InputEditorPosition position = 
               docDisplay_.createInputEditorPosition(
                                    completionPosition_.getPosition());  
            Rectangle bounds = docDisplay_.getPositionBounds(position);
            
            int windowBottom = Window.getScrollTop() + 
                               Window.getClientHeight() ;
            int cursorBottom = bounds.getBottom() ;
            
            // figure out whether we should show below (do this 
            // only once so that we maintain the menu orientation
            // while filtering)
            if (showBelow_ == null)
               showBelow_ = windowBottom - cursorBottom >= offsetHeight;
            
            final int PAD = 3;
            if (showBelow_)
               setPopupPosition(bounds.getLeft(), cursorBottom + PAD) ;
            else
               setPopupPosition(bounds.getLeft(), 
                                bounds.getTop() - offsetHeight) ;
         }
      });
   }
   
   private boolean isSelectable()
   {
      return onSelected_ != null;
   }
   
   private void addToolTipHandler()
   {
      toolTip_ = new CppCompletionToolTip();
      
      addSelectionHandler(new SelectionHandler<MenuItem>()
      {
         public void onSelection(SelectionEvent<MenuItem> event)
         { 
            // bail if we are updating the menu
            if (updatingMenu_)
               return;
            
            // bail if we have no more tooltip
            if (toolTip_ == null)
               return;
            
            // screen unselectable
            if (!isSelectable())
            {
               toolTip_.setVisible(false);
               return;
            }
               
            // screen unable to find menu or selected item
            final MenuItem selectedItem = event.getSelectedItem();
            if (selectedItem == null)
            {
               toolTip_.setVisible(false);
               return;
            }
            
            int index = menuBar_.getItemIndex(selectedItem);
            if (index == -1)
            {
               toolTip_.setVisible(false);
               return;
            }
            
            // screen no completion text
            CppCompletion completion = completions_.get(index);
            if (completion.getText() == null)
            {
               toolTip_.setVisible(false);
               return;
            }
            
            // only do tooltips for functions and variables
            if (completion.getType() != CppCompletion.FUNCTION && 
                completion.getType() != CppCompletion.VARIABLE &&
                completion.getType() != CppCompletion.SNIPPET)
            {
               toolTip_.setVisible(false);
               return;
            }
            
            // set the tooltip text
            toolTip_.setText(completion.getText().get(0));
           
            // position it in the next event loop
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

               @Override
               public void execute()
               { 
                  // bail if there is no longer a tooltip
                  if (toolTip_ == null)
                     return;
                  
                  // some constants
                  final int H_PAD = 12;
                  final int V_PAD = -3;
                  final int H_BUFFER = 100;
                  final int MIN_WIDTH = 300;
                  
                  // candidate left and top
                  final int left = selectedItem.getAbsoluteLeft() 
                             + selectedItem.getOffsetWidth() + H_PAD;
                  final int top = selectedItem.getAbsoluteTop() + V_PAD;
                  
                  // do we have enough room to the right? if not then
                  int roomRight = Window.getClientWidth() - left;
                  int maxWidth = Math.min(roomRight - H_BUFFER, 500);
                  final boolean showLeft = maxWidth < MIN_WIDTH;
                  if (showLeft)
                     maxWidth = selectedItem.getAbsoluteLeft() - H_BUFFER;
                  
                  if (toolTip_.getAbsoluteLeft() != left ||
                      toolTip_.getAbsoluteTop() != top)
                  {
                     toolTip_.setMaxWidth(maxWidth);  
                     toolTip_.setPopupPositionAndShow(new PositionCallback(){

                        @Override
                        public void setPosition(int offsetWidth,
                                                int offsetHeight)
                        {
                           // if we are showing left then adjust
                           int adjustedLeft = left;
                           if (showLeft)
                           {
                              adjustedLeft = selectedItem.getAbsoluteLeft() -
                                             offsetWidth - H_PAD;
                           }
                           toolTip_.setPopupPosition(adjustedLeft, top);
                        }
                     });
                  }
                  else
                  {
                     if (!toolTip_.isVisible())
                        toolTip_.setVisible(true);
                  }

                  
               }
            });
         }
      });
      
      addCloseHandler(new CloseHandler<PopupPanel>() {

         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            toolTip_.hide();
            toolTip_ = null;
         }
      });
   }
   
   @Override
   protected int getMaxHeight()
   {
      return 180;
   }
   
   public CompletionPosition getCompletionPosition()
   {
      return completionPosition_;
   }
   
   public boolean selectPrev()
   {
      menuBar_.moveSelectionUp();
      return true;
   }
   
   public boolean selectNext()
   {
      menuBar_.moveSelectionDown();
      return true;
   }
   
   private final DocDisplay docDisplay_;
   private final CompletionPosition completionPosition_;
   private JsArray<CppCompletion> completions_;
   private CommandWithArg<CppCompletion> onSelected_ = null;
   private Boolean showBelow_ = null;
   private boolean updatingMenu_ = false;
   private CppCompletionToolTip toolTip_;
   
   private static CppCompletionResources RES = CppCompletionResources.INSTANCE;
   
}
