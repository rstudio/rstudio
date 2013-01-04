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

// find(const stored_type&)
template< typename stored_type, typename tree_type, typename container_type >
typename tcl::associative_tree<stored_type, tree_type, container_type>::iterator 
tcl::associative_tree<stored_type, tree_type, container_type>::find(const stored_type& value) 
{
  tree_type node_obj(value); // create a search node and search local children
  iterator it(basic_tree_type::children.find(&node_obj), this);
  return it;
}

// find(const stored_type&) const
template< typename stored_type, typename tree_type, typename container_type >
typename tcl::associative_tree<stored_type, tree_type, container_type>::const_iterator 
tcl::associative_tree<stored_type, tree_type, container_type>::find(const stored_type& value) const
{
  tree_type node_obj(value);
  const_iterator it(basic_tree_type::children.find(&node_obj), this);
  return it;
}


// erase(const stored_type&)
template< typename stored_type, typename tree_type, typename container_type >
bool tcl::associative_tree<stored_type, tree_type, container_type>::
erase(const stored_type& value)
{
  bool erased_nodes = false;
  tree_type node_obj(value); // create search node
  typename container_type::iterator it = basic_tree_type::children.find(&node_obj);

  while (it != basic_tree_type::children.end()) { // could be multiple nodes (with multitree)
    basic_tree_type::deallocate_tree_type(*it); // delete node and remove from children
    basic_tree_type::children.erase(it);  
    it = basic_tree_type::children.find(&node_obj);  // any more?
    erased_nodes = true;
  }
  return erased_nodes;
}

// count(const stored_type&)
template< typename stored_type, typename tree_type, typename container_type >
typename tcl::basic_tree<stored_type, tree_type, container_type>::size_type 
tcl::associative_tree<stored_type, tree_type, container_type>::count(const stored_type& value) const
{
  const_iterator it = find(value);
  const_iterator it_end = end();

  typename basic_tree_type::size_type cnt = 0;

  while (it != it_end && !(*it < value || value < *it)) {
    ++cnt;
    ++it;
  }
  return cnt;
}

// lower_bound(const stored_type&)
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::iterator
tcl::associative_tree<stored_type, tree_type, container_type>::lower_bound(const stored_type& value)
{
  tree_type node_obj(value); // create a search node and search local children
  iterator it(basic_tree_type::children.lower_bound(&node_obj), this);
  return it;
}

// lower_bound(const stored_type&) const
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::const_iterator
tcl::associative_tree<stored_type, tree_type, container_type>::lower_bound(const stored_type& value) const
{
  tree_type node_obj(value); // create a search node and search local children
  const_iterator it(basic_tree_type::children.lower_bound(&node_obj), this);
  return it;
}

// upper_bound(const stored_type&)
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::iterator
tcl::associative_tree<stored_type, tree_type, container_type>::upper_bound(const stored_type& value)
{
  tree_type node_obj(value); // create a search node and search local children
  iterator it(basic_tree_type::children.upper_bound(&node_obj), this);
  return it;
}

// upper_bound(const stored_type&) const
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::const_iterator
tcl::associative_tree<stored_type, tree_type, container_type>::upper_bound(const stored_type& value) const
{
  tree_type node_obj(value); // create a search node and search local children
  const_iterator it(basic_tree_type::children.upper_bound(&node_obj), this);
  return it;
}

// insert(iterator, const stored_type&, tree_type*)
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::iterator 
tcl::associative_tree<stored_type, tree_type, container_type>::insert(const const_iterator& pos, const stored_type& value, tree_type* pParent)
{
  // create a new tree_type object to hold the node object
  tree_type* pNew_node; 
  basic_tree_type::allocate_tree_type(pNew_node, tree_type(value));
  pNew_node->set_parent(pParent);

  const typename basic_tree_type::size_type sz = basic_tree_type::children.size();

  typename container_type::iterator children_pos;
  if (pos == pParent->end()) {
    children_pos = basic_tree_type::children.end();
  } else {
    // get non-const iterator position from pos
    children_pos = basic_tree_type::children.begin();
    typename container_type::const_iterator const_children_pos = basic_tree_type::children.begin();
    while (const_children_pos != pos.it && const_children_pos != basic_tree_type::children.end()) {
      ++children_pos;
      ++const_children_pos;
    }
  }

  // insert the tree node into the children container
  const typename container_type::iterator it = basic_tree_type::children.insert(children_pos, pNew_node);

  if (sz == basic_tree_type::children.size()) { // check for successful insertion
    basic_tree_type::deallocate_tree_type(pNew_node);  // not successful.  delete new node and return end()
    return iterator(basic_tree_type::children.end(), this);
  }

  return iterator(it, this);
}

// insert(iterator, const tree_type&, tree_type*)
template< typename stored_type, typename tree_type, typename container_type>
typename tcl::associative_tree<stored_type, tree_type, container_type>::iterator 
tcl::associative_tree<stored_type, tree_type, container_type>::insert(const const_iterator pos, const tree_type& tree_obj, tree_type* pParent)
{
  // insert current node
  iterator base_it = pParent->insert(pos, *tree_obj.get());

  if (base_it != pParent->end()) {
    const_iterator it = tree_obj.begin();
    const const_iterator it_end = tree_obj.end();

    // call this function recursively thru derived tree for children
    for (; it != it_end; ++it)
      base_it.node()->insert(*it.node());
  }
  return base_it;
}

// set(const tree_type&, tree_type*)
template< typename stored_type, typename tree_type, typename container_type>
void tcl::associative_tree<stored_type, tree_type, container_type>::set(const tree_type& tree_obj, tree_type* pParent)
{
  set(*tree_obj.get()); // set data for this node

  const_iterator it = tree_obj.begin();
  const const_iterator it_end = tree_obj.end();
  for (; it != it_end; ++it) { // and insert all descendants of passed tree
    insert(*it.node(), pParent );
  }
}

// clear()
template< typename stored_type, typename tree_type, typename container_type>
void tcl::associative_tree<stored_type, tree_type, container_type>::clear()
{
  iterator it = begin();
  const iterator it_end = end();
  for (; it != it_end; ++it)
  {
    basic_tree_type::deallocate_tree_type(it.node()); // delete all child nodes
  }
  basic_tree_type::children.clear();  // and remove them from set
}

// erase(iterator)
template< typename stored_type, typename tree_type, typename container_type>
void tcl::associative_tree<stored_type, tree_type, container_type>::erase(iterator it) 
{ 
  // check for node presence
  if (it.pParent != this)
    return;

  // clear children
  it.node()->clear(); 
  deallocate_tree_type(it.node());

  const iterator beg_it = begin();
  typename container_type::iterator pos_it = basic_tree_type::children.begin();
  for (; it != beg_it; --it, ++pos_it) ;  // get child iterator position

  basic_tree_type::children.erase(pos_it);
}

// erase(iterator, iterator)
template< typename stored_type, typename tree_type, typename container_type>
void tcl::associative_tree<stored_type, tree_type, container_type>::erase(iterator it_beg, iterator it_end)
{
  while (it_beg != it_end) {
    erase(it_beg++);
  }
}


// operator ==
template<typename stored_type, typename tree_type, typename container_type>
bool tcl::operator == (const associative_tree<stored_type, tree_type, container_type>& lhs, const associative_tree<stored_type, tree_type, container_type>& rhs)
{
  // check this node
  if ((*lhs.get() < *rhs.get()) || (*rhs.get() < *lhs.get()))
    return false;

  typename associative_tree<stored_type, tree_type, container_type>::const_iterator lhs_it = lhs.begin();
  const typename associative_tree<stored_type, tree_type, container_type>::const_iterator lhs_end = lhs.end();
  typename associative_tree<stored_type, tree_type, container_type>::const_iterator rhs_it = rhs.begin();
  const typename associative_tree<stored_type, tree_type, container_type>::const_iterator rhs_end = rhs.end();

  for (; lhs_it != lhs_end && rhs_it != rhs_end; ++lhs_it, ++rhs_it) {
    if (*lhs_it.node() != *rhs_it.node()) {
      return false;
    }
  }

  if (lhs_it != lhs.end() || rhs_it != rhs.end())
    return false;

  return true;
}


// operator <
template<typename stored_type, typename tree_type, typename container_type>
bool tcl::operator < (const associative_tree<stored_type, tree_type, container_type>& lhs, const associative_tree<stored_type, tree_type, container_type>& rhs) 
{
  // check this node
  if (*lhs.get() < *rhs.get())
    return true;

  typename associative_tree<stored_type, tree_type, container_type>::const_iterator lhs_it = lhs.begin();
  const typename associative_tree<stored_type, tree_type, container_type>::const_iterator lhs_end = lhs.end();
  typename associative_tree<stored_type, tree_type, container_type>::const_iterator rhs_it = rhs.begin();
  const typename associative_tree<stored_type, tree_type, container_type>::const_iterator rhs_end = rhs.end();

  for (; lhs_it != lhs_end && rhs_it != rhs_end; ++lhs_it, ++rhs_it) {
    if (*lhs_it.node() < *rhs_it.node()) {
      return true;
    }
  }

  if (lhs.size() != rhs.size()) {
    return lhs.size() < rhs.size();
  }

  return false;
}

