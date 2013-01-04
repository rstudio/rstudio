/*******************************************************************************
Tree Container Library: Generic container library to store data in tree-like structures.
Copyright (c) 2006  Mitchel Haas

This software is provided 'as-is', without any express or implied warranty. 
In no event will the author be held liable for any damages arising from 
the use of this software.

Permission is granted to anyone to use this software for any purpose, 
including commercial applications, and to alter it and redistribute it freely, 
subject to the following restrictions:

1.  The origin of this software must not be misrepresented; 
you must not claim that you wrote the original software. 
If you use this software in a product, an acknowledgment in the product 
documentation would be appreciated but is not required.

2.  Altered source versions must be plainly marked as such, 
and must not be misrepresented as being the original software.

3.  The above copyright notice and this permission notice may not be removed 
or altered from any source distribution.

For complete documentation on this library, see http://www.datasoftsolutions.net
Email questions, comments or suggestions to mhaas@datasoftsolutions.net
*******************************************************************************/

// pre_order_iterator ++()
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::pre_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>& tcl::pre_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::operator ++()
{
  if (at_top) {  // at top node?
    at_top = false; // iterator will be used going forward from here
    it = pTop_node->begin();
  } else if (!it.node()->empty()) { // any children?
    node_stack.push(it); // yes. push current pos
    it = it.node()->begin(); // and goto first child
  } else {
    ++it; // no children. incr to next sibling if present
    // while stack not empty and no next sibling
    while (!node_stack.empty() && it == (node_stack.top()).node()->end()) {
      it = node_stack.top(); // pop parent
      node_stack.pop();
      ++it; // and see if it's got a next sibling
    }
  }
  return *this; 
}

// pre_order_iterator --()
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::pre_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>& tcl::pre_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::operator --()
{
  if (it == pTop_node->end()) { // at end?
    // yes. is top node empty?
    if (pTop_node->empty()) {
      at_top = true;
      return *this;
    }
    //need to set up stack to state just before end
    rit = pTop_node->children.rbegin(); // going backwards
    if (rit != const_cast<const tree_type*>(pTop_node)->children.rend()) { // insure there's children
      if (!(*rit)->empty()) { // last node have children?
        do {  // find the last child of this node
          ++rit; // incr reverse iter..
          it = base_iterator_type(rit.base(), (it != pTop_node->end() ? it.node() : pTop_node)); // ..to convert to fwd iter correctly
          node_stack.push(it); // push parents on the way down
          rit = it.node()->children.rbegin(); // get last child again
        } while (!(*rit)->empty()); // while last child has children
      }
      ++rit; // incr reverse iter
      it = base_iterator_type(rit.base(), (it != pTop_node->end() ? node() : pTop_node)); // to convert to forward iter correctly
    }
  } else { // not at end.
    if (it != it.node()->parent()->begin()) { // is this first sibling?
      --it; // no.  ok to decr to next sibling
      if (!it.node()->empty()) { // children present?
        do { // yes.  get deepest last child
          node_stack.push(it); // first push current 
          it = base_iterator_type(it.node()->children.end(), it.node());
          --it;  // then go to last child
        } while (!it.node()->empty()); // while children present
      }
    } else if (!node_stack.empty()){ // first sibling.  Check for parent
      it = node_stack.top(); // just need to goto parent
      node_stack.pop();
    } else {
      if (!at_top) { // not at top node?
        at_top = true;  // set at top (first) node
      } else {
        --it;  // decrementing beyond top.  this will make the iterator invalid
        at_top = false;
      }
    }
  }
  return *this;
}

// post_order_iterator constructor
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::post_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::post_order_descendant_iterator(tree_pointer_type pCalled_node, bool beg) : pTop_node(pCalled_node), at_top(false)
{
  if (!beg) {
    it = pTop_node->end();
  } else {
    it = pCalled_node->begin();  // goto first child
    if (it != pTop_node->end()) {
      if (!it.node()->empty()) { // have children of it's own?
        do {  // goto deepest first child, while pushing parents
          node_stack.push(it);
          it = it.node()->begin();
        } while (!it.node()->empty());
      }
    } else {
      // no children.  set top node as current
      at_top = true;
    }
  }
}

// post_order_iterator ++()
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::post_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>& tcl::post_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::operator ++()
{
  if (at_top) { // at last (called) node?
    // yes.  
    at_top = false;
    it = pTop_node->end();
    return *this;
  } else if (pTop_node->empty()) {
    ++it;  // iterator has just traversed past end
    return *this;
  }

  const base_iterator_type it_end = it.node()->parent()->end(); // end sibling
  ++it; // advance to next sibling, if present
  if (it != it_end && !it.node()->empty()) { // next sibling present, and has children?
    do {  // goto deepest first child while pushing parents
      node_stack.push(it);
      it = it.node()->begin();
    } while (!it.node()->empty());
  } else { // it is past last sibling, or it has no children
    // if valid it and it has no children, were done
    if (!node_stack.empty() && it == (node_stack.top()).node()->end()) {
      // it is past last sibling, and pushed parents exist.  move back up to parent
      it = node_stack.top();
      node_stack.pop();
    } else if (node_stack.empty() && it == pTop_node->end()) {
      // at top node.  
      at_top = true; // no.  now at top node.
    }
  }
  return *this;
}

// post_order_iterator --()
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::post_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>& tcl::post_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::operator --()
{
  if (at_top) { // at top node
    at_top = false;
    typename container_type::const_reverse_iterator rit = pTop_node->children.rbegin();
    ++rit;
    it = base_iterator_type(rit.base(), pTop_node); // goto last sibling of top node
  } else if (it == pTop_node->end()) { // at end of descendants?
    at_top = true;
  } else { // not at end
    if (!it.node()->empty()) { // children present?
      typename container_type::const_reverse_iterator rit = it.node()->children.rbegin();
      node_stack.push(it);
      ++rit; // push parent and go to last child
      it = base_iterator_type(rit.base(), it.node());
    } else { // no children present
      if (it != it.node()->parent()->begin()) { // at first sibling?
        --it; // no.  just goto prev sibling
      } else { // at first sibling. work our way up until not first sibling
        while (!node_stack.empty() && it == node_stack.top().node()->begin())
        {
          it = node_stack.top();
          node_stack.pop();
        }
        --it; // then goto prev sibling
      }
    }
  }
  return *this;
}

// level_order_iterator ++()
template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
tcl::level_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>& tcl::level_order_descendant_iterator<stored_type, tree_type, tree_pointer_type, container_type, base_iterator_type, pointer_type, reference_type>::operator ++()
{
  if (at_top) {  // at top node?
    // yes.  
    at_top = false;
    it = pTop_node->begin();
    return *this;
  }

  const base_iterator_type it_end = it.node()->parent()->end(); 
  node_queue.push(it); // push current pos node in queue
  ++it;  // and goto next sibling if present

  if (it == it_end) { // past last sibling?  If not, we're done.
    while (!node_queue.empty()) { // yes. Insure queue not empty
      it = node_queue.front(); // pull pos off queue
      node_queue.pop(); // this should be the start pos of level just traversed
      if (!it.node()->empty()) { // have children?
        it = node()->begin(); // yes. descend to start of next level
        break;
      } else if (node_queue.empty()) { // no children.  is queue empty?
        it = pTop_node->end(); // yes. at end
        return *this;
      }
    } 
  }
  return *this;
}

