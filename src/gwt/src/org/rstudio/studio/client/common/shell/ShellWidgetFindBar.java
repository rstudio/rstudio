package org.rstudio.studio.client.common.shell;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.user.client.ui.DockLayoutPanel;

import elemental2.dom.Document;
import elemental2.dom.Element;
import elemental2.dom.Node;
import elemental2.dom.NodeFilter;
import elemental2.dom.Selection;
import elemental2.dom.TreeWalker;
import jsinterop.base.Js;

public class ShellWidgetFindBar extends FindReplaceBar
{
   @Override
   public boolean includeOptionsPanel()
   {
      return false;
   }

   public ShellWidgetFindBar(DockLayoutPanel container, Node root)
   {
      super(false, false, false, false, true);

      container_ = container;
      root_ = root;

      getCloseButton().addClickHandler(event ->
      {
         container_.setWidgetHidden(this, true);
         container_.forceLayout();
      });

      getFindNextButton().addClickHandler(event ->
      {
         findNext();
      });

      getFindPrevButton().addClickHandler(event ->
      {
         findPrevious();
      });
   }

   public void findNext()
   {
      String searchText = getFindValue().getValue();

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
         index = text.indexOf(searchText, index + searchText.length());
         if (index != -1)
         {
            setSelectedRange(node, index, index + searchText.length());
            return;
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

         setSelectedRange(node, index, index + searchText.length());
         Element parentEl = node.parentElement;
         if (parentEl != null)
            parentEl.scrollIntoView();

         break;
      }
   }

   public void findPrevious()
   {
      String searchText = getFindValue().getValue();

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
         index = text.lastIndexOf(searchText, index - searchText.length());
         if (index != -1)
         {
            setSelectedRange(node, index, index + searchText.length());
            return;
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

         setSelectedRange(node, index, index + searchText.length());
         Element parentEl = node.parentElement;
         if (parentEl != null)
            parentEl.scrollIntoView();

         break;
      }
   }

   private static final native void setSelectedRange(Node node, int lhs, int rhs)
   /*-{
      var range = $doc.createRange();
      range.setStart(node, lhs);
      range.setEnd(node, rhs);

      var selection = $wnd.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
   }-*/;

   private static final native Selection getSelection()
   /*-{
      return $wnd.getSelection();
   }-*/;

   private static final native Document getDocument()
   /*-{
      return $doc;
   }-*/;

   private final DockLayoutPanel container_;
   private final Node root_;
}
