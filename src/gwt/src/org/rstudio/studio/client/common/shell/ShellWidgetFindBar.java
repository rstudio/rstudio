package org.rstudio.studio.client.common.shell;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.find.FindBar;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

import elemental2.dom.DOMRect;
import elemental2.dom.Document;
import elemental2.dom.Element;
import elemental2.dom.HTMLInputElement;
import elemental2.dom.KeyboardEvent;
import elemental2.dom.Node;
import elemental2.dom.NodeFilter;
import elemental2.dom.Range;
import elemental2.dom.Selection;
import elemental2.dom.TreeWalker;
import elemental2.dom.Window;
import jsinterop.base.Js;

public class ShellWidgetFindBar extends FindBar
{
   public ShellWidgetFindBar(DockLayoutPanel container,
                             ScrollPanel scroller,
                             Element root)
   {
      super();

      txtFind_.addKeyUpHandler(event ->
      {
         String value = txtFind_.getValue();
         if (StringUtil.isNullOrEmpty(value))
         {
            node_ = null;
            offset_ = 0;
         }
      });

      Element el;

      el = Js.cast(getElement());
      el.addEventListener("keydown", event ->
      {
         KeyboardEvent keyEvent = Js.cast(event);
         String key = StringUtil.notNull(keyEvent.key);

         if (key.equals("Enter"))
         {
            saveInputSelection();
            return;
         }
         
         if (key.length() == 1 ||
             key.startsWith("Arrow") ||
             key.startsWith("Backspace"))
         {
            restoreInputSelection();
            return;
         }
      }, true);

      el.addEventListener("blur", event ->
      {
         inputSelectionRange_ = null;
      }, true);

      container_ = container;
      scroller_ = scroller;
      root_ = root;
   }

   @Override
   public void show(boolean focus)
   {
      container_.setWidgetHidden(this, false);
      container_.forceLayout();
      if (focus)
         txtFind_.focus();
   }

   @Override
   public void hide()
   {
      node_ = null;
      offset_ = 0;
      inputSelectionRange_ = null;

      container_.setWidgetHidden(this, true);
      container_.forceLayout();
   }

   @Override
   public void find(Direction dir)
   {
      if (dir == Direction.NEXT)
      {
         findNext();
      }
      else
      {
         findPrev();
      }
   }

   public void findNext()
   {
      String searchText = getValue();

      Node node = root_;
      int offset = 0;

      if (node_ != null)
      {
         node = node_;
         offset = offset_;
      }

      if (node != null && node.nodeType == Node.TEXT_NODE)
      {
         String text = StringUtil.notNull(node.textContent);
         if (offset + searchText.length() < text.length())
         {
            offset = text.indexOf(searchText, offset + searchText.length());
            if (offset != -1)
            {
               setSelection(node, offset, searchText.length());
               return;
            }
         }
      }

      Document doc = getDocument();
      TreeWalker walker = doc.createTreeWalker(
         Js.cast(root_),
         NodeFilter.SHOW_ALL,
         null,
         false);

      
      walker.setCurrentNode(node);
      for (node = walker.nextNode(); node != null; node = walker.nextNode())
      {
         if (node.nodeType != Node.TEXT_NODE)
            continue;

         String text = StringUtil.notNull(node.textContent);
         offset = text.indexOf(searchText);
         if (offset == -1)
            continue;

         setSelection(node, offset, searchText.length());
         break;
      }
   }

   public void findPrev()
   {
      String searchText = getValue();

      Node node = root_;
      int offset = 0;

      if (node_ != null)
      {
         node = node_;
         offset = offset_;
      }

      if (node != null && node.nodeType == Node.TEXT_NODE)
      {
         String text = StringUtil.notNull(node.textContent);
         if (offset - searchText.length() >= 0)
         {
            offset = text.lastIndexOf(searchText, offset - searchText.length());
            if (offset != -1)
            {
               setSelection(node, offset, searchText.length());
               return;
            }
         }
      }

      Document doc = getDocument();
      TreeWalker walker = doc.createTreeWalker(
         Js.cast(root_),
         NodeFilter.SHOW_ALL,
         null,
         false);

      
      walker.setCurrentNode(node);
      for (node = walker.previousNode(); node != null; node = walker.previousNode())
      {
         if (node.nodeType != Node.TEXT_NODE)
            continue;

         String text = StringUtil.notNull(node.textContent);
         offset = text.lastIndexOf(searchText);
         if (offset == -1)
            continue;

         setSelection(node, offset, searchText.length());
         break;
      }
   }

   public void scrollSelectionIntoView()
   {
      Window wnd = getWindow();
      Selection selection = wnd.getSelection();
      if (selection.rangeCount == 0)
         return;
      
      Element scrollerEl = Js.cast(scroller_.getElement());
      DOMRect selectionRect = selection.getRangeAt(0).getBoundingClientRect();
      DOMRect containerRect = scrollerEl.getBoundingClientRect();

      double targetTop = selectionRect.top - containerRect.top + scroller_.getVerticalScrollPosition();
      scrollerEl.scrollTop = targetTop - containerRect.height / 2.0;
   }

   private void setSelection(Node node, int offset, int size)
   {
      node_ = node;
      offset_ = offset;
      setSelectedRange(node, offset, size);
      scrollSelectionIntoView();
   }

   private HTMLInputElement getInputElement()
   {
      return Js.cast(DomUtils.querySelector(txtFind_.getElement(), "input"));
   }

   private void saveInputSelection()
   {
      if (inputSelectionRange_ == null)
      {
         HTMLInputElement inputEl = getInputElement();
         inputSelectionRange_ = new Pair<>(
            inputEl.selectionStart,
            inputEl.selectionEnd);
      }
   }

   private void restoreInputSelection()
   {
      if (inputSelectionRange_ != null)
      {
         getWindow().getSelection().removeAllRanges();
         getInputElement().setSelectionRange(
            inputSelectionRange_.first,
            inputSelectionRange_.second);
         inputSelectionRange_ = null;
      }
   }

   private static final void setSelectedRange(Node node, int offset, int size)
   {
      Range range = new Range();
      range.setStart(node, offset);
      range.setEnd(node, offset + size);

      getSelection().removeAllRanges();
      getSelection().addRange(range);
   }

   private static final native Selection getSelection()
   /*-{
      return $wnd.getSelection();
   }-*/;

   private static final native Window getWindow()
   /*-{
      return $wnd;
   }-*/;

   private static final native Document getDocument()
   /*-{
      return $doc;
   }-*/;

   private Node node_;
   private int offset_;

   private Pair<Integer, Integer> inputSelectionRange_;

   private final DockLayoutPanel container_;
   private final ScrollPanel scroller_;
   private final Element root_;
}
