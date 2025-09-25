package org.rstudio.studio.client.common.shell;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.find.FindBar;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

import elemental2.dom.DOMRect;
import elemental2.dom.Document;
import elemental2.dom.Element;
import elemental2.dom.Node;
import elemental2.dom.NodeFilter;
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

      container_ = container;
      scroller_ = scroller;
      root_ = root;
   }

   @Override
   public void show()
   {
      container_.setWidgetHidden(this, false);
   }

   @Override
   public void hide()
   {
      container_.setWidgetHidden(this, true);
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
      int index = 0;

      Selection selection = getSelection();
      if (selection != null)
      {
         Node anchorNode = Js.cast(selection.anchorNode);
         if (root_.contains(anchorNode))
         {
            node = anchorNode;
            index = selection.anchorOffset;
         }
      }

      if (node != null && node.nodeType == Node.TEXT_NODE)
      {
         String text = StringUtil.notNull(node.textContent);
         if (index + searchText.length() < text.length())
         {
            index = text.indexOf(searchText, index + searchText.length());
            if (index != -1)
            {
               setSelection(node, index, searchText.length());
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
         index = text.indexOf(searchText);
         if (index == -1)
            continue;

         setSelection(node, index, searchText.length());
         break;
      }
   }

   public void findPrev()
   {
      String searchText = getValue();

      Node node = root_;
      int index = 0;

      Selection selection = getSelection();
      if (selection != null)
      {
         Node anchorNode = Js.cast(selection.anchorNode);
         if (root_.contains(anchorNode))
         {
            node = anchorNode;
            index = selection.anchorOffset;
         }
      }

      if (node != null && node.nodeType == Node.TEXT_NODE)
      {
         String text = StringUtil.notNull(node.textContent);
         if (index - searchText.length() >= 0)
         {
            index = text.lastIndexOf(searchText, index - searchText.length());
            if (index != -1)
            {
               setSelection(node, index, searchText.length());
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
         index = text.lastIndexOf(searchText);
         if (index == -1)
            continue;

         setSelection(node, index, searchText.length());
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
      setSelectedRange(node, offset, size);
      scrollSelectionIntoView();
   }

   private static final native void setSelectedRange(Node node, int offset, int size)
   /*-{
      var range = $doc.createRange();
      range.setStart(node, offset);
      range.setEnd(node, offset + size);

      var selection = $wnd.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
   }-*/;

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

   private final DockLayoutPanel container_;
   private final ScrollPanel scroller_;
   private final Element root_;
}
