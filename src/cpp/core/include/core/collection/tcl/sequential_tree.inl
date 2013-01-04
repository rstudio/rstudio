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

// copy constructor
template<typename stored_type>
tcl::sequential_tree<stored_type>::sequential_tree(const tree_type& rhs) : basic_tree_type(rhs)
{
  const_iterator it = rhs.begin();
  const const_iterator it_end = rhs.end();
  for (; it != it_end; ++it) // do a deep copy by inserting children (and descendants)
  {
    insert(*it.node());
  }
}

// assignment operator
template<typename stored_type>
tcl::sequential_tree<stored_type>& tcl::sequential_tree<stored_type>::operator = (const tree_type& rhs)
{
  if (this == &rhs) // check for self assignment
    return *this;

  clear();
  basic_tree_type::operator =(rhs); // call base class operation

  const_iterator it = rhs.begin(), it_end = rhs.end();
  for (; it != it_end; ++it) // insert children and descendants
  {
    insert(*it.node());
  }
  return *this;
}

// swap
template<typename stored_type>
void tcl::sequential_tree<stored_type>::swap(tree_type& rhs)
{
  tree_type temp(*this);

  clear();
  *this = rhs;

  rhs.clear();
  rhs = temp;
}

// insert(const stored_type&)
template< typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::insert( const stored_type& value)
{
  // create a new tree_type object to hold the node object
  tree_type* pNew_node; 
  basic_tree_type::allocate_tree_type(pNew_node, tree_type(value));
  pNew_node->set_parent(this);

  const typename basic_tree_type::size_type sz = basic_tree_type::children.size();

  // insert the tree node into the children container
  const typename container_type::iterator it = basic_tree_type::children.insert(basic_tree_type::children.end(), pNew_node);

  if (sz == basic_tree_type::children.size()) { // check for successful insertion
    basic_tree_type::deallocate_tree_type(pNew_node);  // not successful.  delete new node and return end()
    return iterator(basic_tree_type::children.end(), this);
  }

  return iterator(it, this);
}

// insert(const tree_type&)
template< typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::insert(const tree_type& tree_obj)
{
  // insert current node
  const iterator base_it = insert(*tree_obj.get());

  if (base_it != end()) {
    const_iterator it = tree_obj.begin();
    const const_iterator it_end = tree_obj.end();

    // call this function recursively thru derived tree for children
    for (; it != it_end; ++it)
      base_it.node()->insert(*it.node());
  }
  return base_it;
}

// push_back(const stored_type&)
template< typename stored_type>
void tcl::sequential_tree<stored_type>::push_back(const stored_type& value)
{
  // create a new tree_type object to hold the node object
  tree_type* pNew_node; 
  basic_tree_type::allocate_tree_type(pNew_node, tree_type(value));
  pNew_node->set_parent(this);

  basic_tree_type::children.push_back(pNew_node);
}

// insert(const_iterator, const stored_type&)
template<typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::insert(const_iterator pos, const stored_type& value)
{
  // create a new tree_type object to hold the node object
  tree_type* pNew_node = 0; 
  basic_tree_type::allocate_tree_type(pNew_node, tree_type(value));
  pNew_node->set_parent(this);

  const typename std::vector<stored_type>::size_type sz = basic_tree_type::children.size();

  // calculate the insertion point
  const const_iterator beg_it = begin();
  typename container_type::iterator pos_it = basic_tree_type::children.begin(); 
  for (; pos != beg_it; --pos, ++pos_it) ;
  // insert the tree node into the children container
  const typename container_type::iterator it = basic_tree_type::children.insert(pos_it, pNew_node);

  if (sz == basic_tree_type::children.size()) { // check for successful insertion
    basic_tree_type::deallocate_tree_type(pNew_node);  // not successful.  delete new node and return end()
    iterator end_it(basic_tree_type::children.end(), this);
    return end_it;
  }

  iterator node_it(it, this);
  return node_it;
}

// insert(const_iterator, size_type, const stored_type&)
template<typename stored_type>
void 
tcl::sequential_tree<stored_type>::insert(const_iterator pos, const typename basic_tree_type::size_type num, const stored_type& value)
{
  for (typename basic_tree_type::size_type i = 0; i < num; ++i) {
    pos = insert(pos, value);
    ++pos;
  }
}

// insert(const_iterator, const tree_type&)
template<typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::insert(const const_iterator& pos, const tree_type& tree_obj)
{
  // insert current node
  const iterator base_it = insert(pos, *tree_obj.get());

  if (base_it != end()) {
    const_iterator it = tree_obj.begin();
    const const_iterator it_end = tree_obj.end();

    // call this function recursively thru derived tree for children
    for (; it != it_end; ++it)
      base_it.node()->insert(*it.node());
  }
  return base_it;
}


// set(const tree_type&)
template< typename stored_type >
void tcl::sequential_tree<stored_type>::set(const sequential_tree<stored_type>& tree_obj)
{
  set(*tree_obj.get()); // set data for this node

  const_iterator it = tree_obj.begin();
  const const_iterator it_end = tree_obj.end();
  for (; it != it_end; ++it) { // and insert all descendants of passed tree
    insert(*it.node());
  }
}

// clear()
template< typename stored_type >
void tcl::sequential_tree<stored_type>::clear()
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
template<typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::erase(iterator it)
{
  // check for node presence
  if (it.pParent != this)
    return end();

  // clear children
  it.node()->clear(); 
  deallocate_tree_type(it.node());

  const iterator beg_it = begin();
  typename container_type::iterator pos_it = basic_tree_type::children.begin();
  for (; it != beg_it; --it, ++pos_it) ;  // get child iterator position

  return iterator(basic_tree_type::children.erase(pos_it), this);
}

// erase(iterator, iterator)
template<typename stored_type>
typename tcl::sequential_tree<stored_type>::iterator 
tcl::sequential_tree<stored_type>::erase(iterator beg_it, iterator end_it) 
{
  int delete_count = 0;
  for (; beg_it != end_it; --end_it)
    ++delete_count;

  for (int i = 0; i < delete_count; ++i) {
    beg_it = erase(beg_it);
  }

  return beg_it;
}

// operator [](size_type)
template<typename stored_type>
tcl::sequential_tree<stored_type>& tcl::sequential_tree<stored_type>::operator [](basic_size_type index) 
{ 
  if (index >= basic_tree_type::size())
    throw std::out_of_range("sequential_tree index out of range");

  return *(begin() + index).node();
}

// operator [](size_type) const
template<typename stored_type>
const tcl::sequential_tree<stored_type>& tcl::sequential_tree<stored_type>::operator [](basic_size_type index) const 
{ 
  if (index >= basic_tree_type::size())
    throw std::out_of_range("sequential_tree index out of range");

  return *(begin() + index).node();
}


// operator ==
template<typename stored_type>
bool tcl::operator == (const sequential_tree<stored_type>& lhs, const sequential_tree<stored_type>& rhs) 
{
  // check this node
  if (!(*lhs.get() == *rhs.get()))
    return false;

  typename sequential_tree<stored_type>::const_iterator lhs_it = lhs.begin();
  const typename sequential_tree<stored_type>::const_iterator lhs_end = lhs.end();
  typename sequential_tree<stored_type>::const_iterator rhs_it = rhs.begin();
  const typename sequential_tree<stored_type>::const_iterator rhs_end = rhs.end();

  for (; lhs_it != lhs_end && rhs_it != rhs_end; ++lhs_it, ++rhs_it) {
    if (!(*lhs_it.node() == *rhs_it.node())) {
      return false;
    }
  }

  if (lhs_it != lhs.end() || rhs_it != rhs.end())
    return false;

  return true;
}


// operator <
template<typename stored_type>
bool tcl::operator < (const sequential_tree<stored_type>& lhs, const sequential_tree<stored_type>& rhs) 
{
  // check this node
  if (*lhs.get() < *rhs.get())
    return true;

  typename sequential_tree<stored_type>::const_iterator lhs_it = lhs.begin();
  const typename sequential_tree<stored_type>::const_iterator lhs_end = lhs.end();
  typename sequential_tree<stored_type>::const_iterator rhs_it = rhs.begin();
  const typename sequential_tree<stored_type>::const_iterator rhs_end = rhs.end();

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

// sort_descendants()
template<typename stored_type>
void tcl::sequential_tree<stored_type>::sort_descendants() 
{
  post_order_iterator it = post_order_begin();
  const post_order_iterator it_end = post_order_end();
  for (; it != it_end; ++it)
  {
    it.node()->sort();
  }
}



