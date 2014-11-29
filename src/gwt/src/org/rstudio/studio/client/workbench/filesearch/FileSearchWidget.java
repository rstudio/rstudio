package org.rstudio.studio.client.workbench.filesearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.GridWithMouseHandlers;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchOracle;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class FileSearchWidget extends ModalDialogBase
{
   private TextBox textBox_;
   private GridWithMouseHandlers completionGrid_;
   private FlowPanel completionPanel_;
   private ArrayList<HandlerRegistration> handlers_;
   private Cell selectedCell_;
   private DocDisplay docDisplay_;
   
   private static final int WIDGET_WIDTH = 500;
   private static final int WIDGET_HEIGHT = 400;
   
   public FileSearchWidget(DocDisplay docDisplay)
   {
      super();
      setText("Search Files");
      
      docDisplay_ = docDisplay;
      cachedFiles_ = new HashMap<String, ArrayList<String>>();
      
      VerticalPanel outer = new VerticalPanel();
      outer.setStylePrimaryName(RES.styles().outerPanel());
      outer.setWidth(WIDGET_WIDTH + "px");
      outer.setHeight(WIDGET_HEIGHT + "px");
      
      initTextBox();
      initCompletionGrid();
      initCompletionPanel();
      
      outer.add(completionPanel_);
      outer.add(textBox_);
      
      setWidget(outer);
      setWidth(WIDGET_WIDTH + "px");
      
      handlers_ = new ArrayList<HandlerRegistration>();
      addHandlers();
      
      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            docDisplay_.setFocus(true);
         }
      });
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return this;
   }
   
   private void initCompletionPanel()
   {
      completionPanel_ = new FlowPanel();
      completionPanel_.setStylePrimaryName(RES.styles().completionGrid());
      completionPanel_.setWidth("100%");
      completionPanel_.setHeight((WIDGET_HEIGHT - 38) + "px");
      completionPanel_.add(completionGrid_);
   }
   
   private void initTextBox()
   {
      textBox_ = new TextBox();
      textBox_.setWidth((WIDGET_WIDTH - 2) + "px");
      textBox_.setStylePrimaryName(RES.styles().textBox());
      textBox_.getElement().setPropertyString("placeholder",
            "Enter your search query");
      
      textBox_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int keyCode = event.getNativeEvent().getKeyCode();
            if (keyCode == KeyCodes.KEY_UP)
            {
               event.stopPropagation();
               event.preventDefault();
               selectPrevious();
            }
            
            if (keyCode == KeyCodes.KEY_DOWN)
            {
               event.stopPropagation();
               event.preventDefault();
               selectNext();
            }
            
            if (keyCode == KeyCodes.KEY_ENTER && selectedCell_ != null)
            {
               event.stopPropagation();
               event.preventDefault();
               onSelection(selectedCell_);
            }
            
            if (keyCode == KeyCodes.KEY_BACKSPACE)
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     update();
                  }
               });
            }
         }
      });
      
   }
   
   private void handleMouseEvent(MouseEvent event)
   {
      Cell cell = completionGrid_.getCellForEvent(event);
      if (cell != null)
         selectCell(cell.getRowIndex());
   }
   
   private void initCompletionGrid()
   {
      completionGrid_ = new GridWithMouseHandlers(0, 1);
      completionGrid_.setWidth("100%");
      
      completionGrid_.addMouseMoveHandler(new MouseMoveHandler()
      {
         @Override
         public void onMouseMove(MouseMoveEvent event)
         {
            handleMouseEvent(event);
         }
      });
      
      completionGrid_.addMouseOverHandler(new MouseOverHandler()
      {
         @Override
         public void onMouseOver(MouseOverEvent event)
         {
            handleMouseEvent(event);
         }
      });
      
      completionGrid_.addMouseOutHandler(new MouseOutHandler()
      {
         @Override
         public void onMouseOut(MouseOutEvent event)
         {
            selectedCell_.getElement().setAttribute("style", "");
         }
      });
      
      completionGrid_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onSelection(completionGrid_.getCellForEvent(event));
         }
      });
   }
   
   private void onSelection(Cell cell)
   {
      docDisplay_.insertCode(cell.getElement().getInnerText());
      reset();
      setVisible(false);
      hide();
      docDisplay_.setFocus(true);
   }
   
   private void addHandlers()
   {
      handlers_.add(Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONKEYDOWN)
            {
               if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
               {
                  reset();
                  setVisible(false);
                  hide();
                  docDisplay_.setFocus(true);
               }
            }
            
            if (event.getTypeInt() == Event.ONKEYPRESS)
            {
               // Let the insertion happen first
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     update();
                  }
               });
            }
         }
      }));
      
   }
   
   private void selectCell(int row)
   {
      Cell newCell = completionGrid_.getCell(row, 0);
      if (selectedCell_ == null)
      {
         selectedCell_ = newCell;
      }
      else if (selectedCell_ != newCell)
      {
         // Unselect old cell
         selectedCell_.getElement().removeClassName(
               RES.styles().selected());
         
         // Select new cell
         selectedCell_ = newCell;
         selectedCell_.getElement().addClassName(
               RES.styles().selected());
      }
   }
   
   private void selectPrevious()
   {
      if (completionGrid_.getRowCount() == 0)
         return;
      
      if (selectedCell_ == null)
      {
         selectCell(completionGrid_.getRowCount() - 1);
         return;
      }
      
      if (selectedCell_.getRowIndex() == 0 &&
            completionGrid_.getRowCount() > 0)
         selectCell(completionGrid_.getRowCount() - 1);
      else
         selectCell(selectedCell_.getRowIndex() - 1);
   }
   
   private void selectNext()
   {
      if (completionGrid_.getRowCount() == 0)
         return;
      
      if (selectedCell_ == null)
      {
         selectCell(completionGrid_.getRowCount() - 1);
         return;
      }
      
      if (selectedCell_.getRowIndex() == completionGrid_.getRowCount() - 1)
         selectCell(0);
      else
         selectCell(selectedCell_.getRowIndex() + 1);
   }
   
   private void clearHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      handlers_.clear();
   }
   
   private ArrayList<String> currentFiles_;
   private final HashMap<String, ArrayList<String>> cachedFiles_;
   private String query_;
   
   public void setFilesAndShow(JsArrayString files)
   {
      if (handlers_.isEmpty())
         addHandlers();
      
      // Make the popup visible
      setVisible(true);
      center();
      show();
      
      // Set all + current files for this session
      currentFiles_ = new ArrayList<String>(files.length());
      for (int i = 0; i < files.length(); i++)
         currentFiles_.add(files.get(i));
      
      // Give text box focus
      textBox_.setFocus(true);
   }
   
   private ArrayList<String> getTopScoringCompletions(
         ArrayList<String> files)
   {
      Comparator<String> fuzzyComparator =
            Collections.reverseOrder(
                  CodeSearchOracle.createFuzzyComparator(
                        query_, true));
      
      // Use a priority queue to get the top completions
      PriorityQueue<String> queue =
            new PriorityQueue<String>(MAX_GRID_SIZE, fuzzyComparator);
      queue.addAll(files);
      
      ArrayList<String> results = new ArrayList<String>();
      int max = Math.min(queue.size(), MAX_GRID_SIZE);
      for (int i = 0; i < max; i++)
         results.add(queue.poll());
      
      return results;
   }
   
   private void update()
   {
      // Store the current query
      query_ = textBox_.getText();
      
      // Get the current matches based on the query
      // Check the cache in case we already have seen this query
      ArrayList<String> cache =
            cachedFiles_.get(query_);
      
      if (cache != null)
      {
         setCompletions(cache);
         return;
      }
      
      // Get the current set of valid completions
      ArrayList<String> newFiles = new ArrayList<String>();
      for (String file : currentFiles_)
         if (StringUtil.isSubsequence(file, query_, true))
            newFiles.add(file);
      
      // Get the top completions
      ArrayList<String> topCompletions = getTopScoringCompletions(newFiles);
      
      // Cache these for later use
      cachedFiles_.put(query_, topCompletions);
      
      // Set and display
      setCompletions(topCompletions);
      
      // Ensure text box up-to-date and visible
      textBox_.setText("");
      textBox_.setText(query_);
      
   }
   
   private void clearSelection()
   {
      if (selectedCell_ != null)
      {
         selectedCell_.getElement().removeClassName(
               RES.styles().selected());
         selectedCell_ = null;
      }
   }
   
   private void setCompletions(ArrayList<String> completions)
   {
      // Select the top item (if there are any)
      if (!completions.isEmpty())
      {
         completionGrid_.resizeRows(
               Math.min(MAX_GRID_SIZE, completions.size()));
         selectCell(0);
      }
      else
      {
         clearSelection();
      }
      
      // Refresh current completions
      completionGrid_.clearColumn(0);
      for (int i = 0; i < completions.size(); i++)
      {
         if (i >= completionGrid_.getRowCount())
            break;
         completionGrid_.setText(i, 0, completions.get(i));
      }
   }
   
   public void setFocus(boolean focus)
   {
      textBox_.setFocus(true);
   }
   
   private void reset()
   {
      completionGrid_.clearColumn(0);
      completionGrid_.resizeRows(0);
      textBox_.setText("");
      cachedFiles_.clear();
      query_ = null;
      currentFiles_ = null;
      clearHandlers();
      selectedCell_ = null;
   }
   
   private static final int MAX_GRID_SIZE = 20;
   private static final FileSearchWidgetResources RES =
         FileSearchWidgetResources.INSTANCE;
   
   static {
      RES.styles().ensureInjected();
   }
   
}
