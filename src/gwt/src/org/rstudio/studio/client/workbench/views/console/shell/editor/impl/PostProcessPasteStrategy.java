package org.rstudio.studio.client.workbench.views.console.shell.editor.impl;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PasteStrategy;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PlainTextEditor;

public class PostProcessPasteStrategy implements PasteStrategy
{
   public void initialize(PlainTextEditor editor, Element textContainer)
   {
      editor_ = editor;
      textContainer_ = textContainer.cast();
      hookPaste(textContainer);
   }

   private native void hookPaste(Element el) /*-{
      var thiz = this;
      el.addEventListener("paste",
            function (evt) {
               thiz.@org.rstudio.studio.client.workbench.views.console.shell.editor.impl.PostProcessPasteStrategy::cleanup()();
            },
            false);
   }-*/;

   @SuppressWarnings("unused")
   private void cleanup()
   {
      new Timer() {
         @Override
         public void run()
         {
            // Not sure why this would ever be but we've seen some suspicious
            // looking logs that seem to suggest textContainer_ can be null
            if (textContainer_ == null)
               return;

            gentleCleanup();

            final InputEditorSelection selection = editor_.getSelection();
            String sel = (selection == null) ? "null" : selection.toString();

            String innerText = StringUtil.notNull(
                  DomUtils.getInnerText(textContainer_, true));
            innerText = innerText
                  .replace('\u00A0', ' ')
                  .replace("\u200B", "");
            editor_.setText(innerText);

            // If we try to set selection right away after replacing the
            // contents, Firefox plows over it and puts the selection at
            // the end anyway.
            new Timer()
            {
               @Override
               public void run()
               {
                  if (selection != null)
                     editor_.beginSetSelection(selection, null);
               }
            }.schedule(50);
         }
      }.schedule(50);
   }


   /**
    * Jumping through some hoops to try to get Firefox and Webkit to have
    * similar paste behavior.
    *
    * It is important that the cleanup step not cause the cursor to be removed,
    * hence "gentle"--text nodes must not be reparented, for example. However,
    * removing non-visible elements should be fine in most cases.
    *
    * Issues we are working around:
    *
    * - Webkit potentially includes lots of garbage tags like meta, link,
    *   etc. even for quite simple HTML. They all have newlines after them.
    *   If these aren't removed (along with their newlines) you end up with an
    *   insane amount of extra whitespace.
    * - For the markup <div>a</div><div>b</div><div>c</div>, Firefox inserts
    *   an explicit <br> at the end of each div. Webkit does not. Standardize
    *   on putting in these <br>'s on any block element if necessary to cause
    *   newlines to end up in the final text.
    *
    * TODO: maybeAddBreakToDiv needs to be for all block elements, not just div.
    * TODO: Reusable method for reverse depth-first traversal of DOM tree.
    */
   private void gentleCleanup()
   {
      textContainer_.normalize();
      stripComments(textContainer_);
      stripJunk(textContainer_, true);

      NodeList<Element> divs = textContainer_.getElementsByTagName("div");
      for (int i = 0; i < divs.getLength(); i++)
         maybeAddBreakToDiv(divs.getItem(i));
   }

   private void stripComments(Node node)
   {
      if (node.getNodeType() == 8 /* COMMENT_NODE */)
      {
         node.removeFromParent();
      }
      else if (node.getNodeType() == Node.ELEMENT_NODE)
      {
         for (int i = node.getChildCount() - 1; i >= 0; i--)
            stripComments(node.getChild(i));
      }
   }

   private void stripJunk(Element el, boolean preMode)
   {
      String tag = el.getTagName().toLowerCase();
      if (tag.equals("pre"))
         preMode = true;
      else if (tag.matches("^(meta|style|script|title|head)$"))
      {
         if (!preMode)
         {
            Node nextSibling = el.getNextSibling();
            if (nextSibling.getNodeType() == Node.TEXT_NODE)
            {
               String str = StringUtil.notNull(((Text) nextSibling).getData());
               if (str.trim().length() == 0)
                  nextSibling.removeFromParent();
            }
         }
         el.removeFromParent();
         return;
      }

      NodeList<Node> children = el.getChildNodes();
      // Have to iterate backwards because we'll be removing elements
      for (int i = children.getLength() - 1; i >= 0; i--)
         if (children.getItem(i).getNodeType() == Node.ELEMENT_NODE)
            stripJunk((Element) children.getItem(i), preMode);
   }

   private void maybeAddBreakToDiv(Element div)
   {
      if (div.getChildCount() == 0)
         return;

      Node node = div.getChild(div.getChildCount() - 1);
      if (node.getNodeType() == Node.ELEMENT_NODE)
      {
         String tag = ((Element)node).getTagName().toLowerCase();
         if (tag.equalsIgnoreCase("br") || tag.equalsIgnoreCase("div"))
            return;
      }

      div.appendChild(div.getOwnerDocument().createBRElement());
   }

   private PlainTextEditor editor_;
   private ElementEx textContainer_;
}
