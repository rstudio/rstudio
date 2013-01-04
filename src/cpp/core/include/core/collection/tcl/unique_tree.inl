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
#include <algorithm>

// copy constructor
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::unique_tree( const tree_type& rhs ) 
: associative_tree_type(rhs), pOrphans(0), allowing_orphans(false), destroy_root(false)
{
  allowing_orphans = rhs.allowing_orphans;  // copy orphan flag

  if (rhs.pOrphans) { // orphans present?
    basic_tree_type::allocate_tree_type(pOrphans, tree_type());
    associative_const_iterator_type it = rhs.pOrphans->begin();
    const associative_const_iterator_type it_end = rhs.pOrphans->end();
    for (; it != it_end; ++it) { // copy orphans
      pOrphans->insert(*it.node());
    }
  } else
    pOrphans = 0;

  associative_const_iterator_type it = rhs.begin();
  const associative_const_iterator_type it_end = rhs.end();
  for (; it != it_end; ++it) { // do deep copy by inserting children (and descendants)
    insert(*it.node());
  }
}

// assignment operator
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>& 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::operator = (const tree_type& rhs)
{
  if (!associative_tree_type::is_root()) // can assign only to root node
    return *this;

  if (this == &rhs)  // check for self assignment
    return *this;

  clear();
  basic_tree_type::operator =(rhs); // base class operation

  allowing_orphans = rhs.allowing_orphans;

  if (rhs.pOrphans) { // orphans present?
    basic_tree_type::allocate_tree_type(pOrphans, tree_type());  // yes.  copy them
    associative_const_iterator_type it = rhs.pOrphans->begin();
    const associative_const_iterator_type it_end = rhs.pOrphans->end();
    for (; it != it_end; ++it) {
      pOrphans->insert(*it.node());
    }
  } else
    pOrphans = 0;

  associative_const_iterator_type it = rhs.begin();
  const associative_const_iterator_type it_end = rhs.end();
  for (; it != it_end; ++it) {  // copy all children (and descendants)
    insert(*it.node());
  }

  return *this;
}


// set(const tree_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::set(const tree_type& tree_obj)
{
  if (!check_for_duplicate(*tree_obj.get(), this)) { // duplicate node exist in tree?
    // no.  OK to set this node
    basic_tree_type::set(*tree_obj.get());

    associative_const_iterator_type it = tree_obj.begin(), it_end = tree_obj.end();
    for (; it != it_end; ++it) { // insert any children
      insert(*it.node());
    }

    if (tree_obj.pOrphans && allow_orphans()) { // copy orphans if any present
      get_root()->pOrphans->set(*tree_obj.pOrphans );
    }

  }
}


// insert(const stored_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::child_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::insert(const stored_type& value) 
{ 
  const tree_type* const pRoot = get_root();
  if (allow_orphans() && pRoot->pOrphans) { // orphans present?
    // yes.  check orphans for child
    const associative_iterator_type oit = pRoot->pOrphans->find_deep(value);
    if (oit != pRoot->pOrphans->end()) { 
      // child is an orphan.  update orphan with new data
      oit.node()->set(value);
      tree_type orphan;
      orphan.set(*oit.node());
      pRoot->pOrphans->erase(*oit);
      return insert(orphan);
    }
  }

  // stored obj doesn't already exist in an orphan
  if (!check_for_duplicate(value, this)) { // check for duplication
    const associative_iterator_type it = associative_tree_type::insert(value, this);
    ordered_children.insert(it.node());  // no duplicate exists.  insert new node
    inform_grandparents(it.node(), this );
    return it;
  } else
    return associative_tree_type::end(); // duplicate node exists.  don't insert

}

// insert(const tree_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::child_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::insert(const tree_type& tree_obj )
{
  if (tree_obj.pOrphans && allow_orphans()) { // have orphans?
    get_root()->pOrphans->insert(*tree_obj.pOrphans ); // yes.  copy orphans
  }

  // insert current node
  associative_iterator_type base_it = insert(*tree_obj.get());

  if (base_it == associative_tree_type::end()) { // insert successful?
    // no.  but, the node may have existed here previously.  check if so
    base_it = associative_tree_type::find(*tree_obj.get()); 
  }

  if (base_it != associative_tree_type::end()) {  // node exist?
    associative_const_iterator_type it = tree_obj.begin();
    const associative_const_iterator_type it_end = tree_obj.end();

    // call this function recursively to insert children and descendants
    for (; it != it_end; ++it)
      base_it.node()->insert(*it.node());
  }
  return base_it;
}

// insert(const stored_type&, const stored_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::child_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::insert( const stored_type& parent_obj, const stored_type& value)
{
  if (!(parent_obj < (*basic_tree_type::get())) && !((*basic_tree_type::get()) < parent_obj)) { // is this node the parent? 
    return insert(value);  // yes.  insert the node here.
  }

  // find parent node
  associative_iterator_type it;
  const associative_iterator_type it_parent = find_deep(parent_obj);

  const tree_type* const pRoot = get_root();
  if (it_parent != associative_tree_type::end()) {
    // found parent node, 
    if (allow_orphans() && pRoot->pOrphans) {
      // orphans present.  check orphans for child
      const associative_iterator_type oit = pRoot->pOrphans->find_deep(value);
      if (oit != pRoot->pOrphans->end()) {
        // child is an orphan.  update orphan with new data
        oit.node()->set(value);
        tree_type orphan;
        orphan.set(*oit.node());
        pRoot->pOrphans->erase(*oit);
        it = it_parent.node()->insert(orphan);
      } else
        it = it_parent.node()->insert(value); // child not an orphan. inset child node in parent 
    } else {
      it = it_parent.node()->insert(value); // no orphans.  insert child node in parent
    }
    if (it == it_parent.node()->end()) // was node inserted successfully?
      return associative_tree_type::end(); // no.  return proper end()
  } else if (allow_orphans()) { 
    // parent not found.  do we have orphans?
    if (!pRoot->pOrphans) {
      basic_tree_type::allocate_tree_type(pRoot->pOrphans, tree_type());  // no, instanciate them
    }

    associative_iterator_type oit = pRoot->pOrphans->find_deep(parent_obj);

    // orphans contain parent?
    if (oit == pRoot->pOrphans->end()) {
      // no.  create parent in orphans
      oit = pRoot->pOrphans->insert(parent_obj);
      pRoot->pOrphans->ordered_children.clear();  // orphans need no ordered children
    }

    const associative_iterator_type child_oit = pRoot->pOrphans->find_deep(value);
    if (child_oit != pRoot->pOrphans->end()) {
      // child is an orphan.  update orphan with new data
      child_oit.node()->set(value);
      tree_type orphan;
      orphan.set(*child_oit.node());
      pRoot->pOrphans->erase(*child_oit);
      it = oit.node()->insert(orphan);
      oit.node()->ordered_children.clear();
    } else {
      it = oit.node()->insert(value); // child not an orphan.  insert child in parent orphan
      oit.node()->ordered_children.clear();
    }

    if (it == oit.node()->end()) // was child inserted as orphan?
      return associative_tree_type::end();  // no.  return proper end()
  } else {
    return associative_tree_type::end(); // couldn't find parent, and orphans not allowed
  }

  return it;
}


// find_deep(const stored_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::child_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::find_deep(const stored_type& value) 
{
  tree_type tree_node(value);  // create seach node
  const typename std::set<tree_type*, key_compare>::iterator desc_it = descendents.find(&tree_node);
  if (desc_it == descendents.end()) // node found in descendants?
    return associative_tree_type::end();  // no.  node not a descendant of this node

  // node is some type of descendant.  check if it's an immediate child
  associative_iterator_type it = associative_tree_type::find(value);
  if (it != associative_tree_type::end())
    return it;

  // node not an immediate child.  
  it = associative_tree_type::begin();
  const associative_iterator_type it_end = associative_tree_type::end();
  for (; it != it_end; ++it) {  // iterate over children and call this fcn recursively
    const associative_iterator_type grandchild_it = it.node()->find_deep(value);
    if (grandchild_it != it.node()->end())
      return grandchild_it;  // found it
  }

  return associative_tree_type::end();
}

// find_deep(const stored_type&) const
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::const_child_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::find_deep(const stored_type& value) const
{
  associative_const_iterator_type it_end = associative_tree_type::end();
  tree_type tree_node(value);  // create seach node
  typename std::set<tree_type*, key_compare>::const_iterator desc_it = descendents.find(&tree_node);
  if (desc_it == descendents.end())  // node found in descendants?
    return it_end;  // no.  node not a descendant of this node

  // node is some type of descendant.  check if it's an immediate child
  associative_const_iterator_type it = associative_tree_type::find(value);
  if (it != it_end)
    return it;

  // node not an immediate child.  
  it = associative_tree_type::begin();
  for (; it != it_end; ++it) { // iterate over children and call this fcn recursively
    associative_const_iterator_type grandchild_it = it.node()->find_deep(value);
    associative_const_iterator_type grandchild_it_end = it.node()->end();
    if (grandchild_it != grandchild_it_end)
      return grandchild_it;  // found it
  }

  return it_end;
}


// clear()
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::clear()
{
  if (pOrphans) {
    destroy_root = true;
  }

  // if not destroying root, need to selectively remove parents descendants
  if (!get_root()->destroy_root) {
    // create descendant remove set
    std::set<tree_type*, key_compare> remove_set;
    remove_set.swap(descendents);  // get a copy of the descendants, and clear them

    tree_type* pParent = basic_tree_type::parent();
    while (pParent != 0) {  // climb up to the root node
      std::set<tree_type*, key_compare> dest_set;  // create a difference set
      std::set_difference( pParent->descendents.begin(), pParent->descendents.end(),
      remove_set.begin(), remove_set.end(), std::inserter(dest_set, dest_set.begin()), key_compare() );
      pParent->descendents.swap(dest_set);  // and remove the deleted descendants
      pParent = pParent->parent();
    }
  }

  associative_tree_type::clear(); // call base class operation
  ordered_children.clear();
  descendents.clear();

  if (pOrphans) { // if this is the root, clear orphans also
    pOrphans->clear();
    destroy_root = false;
  }
}


// inform_grandparents(tree_type*, tree_type*)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::inform_grandparents( tree_type* new_child, tree_type* pParent  )
{
  if (pParent) {  // traverse to root, adding new child to descendants to every node
    pParent->descendents.insert(new_child);
    inform_grandparents(new_child, pParent->parent());
  }
}

// find_ordered(const stored_type&)  
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::ordered_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::find_ordered(const stored_type& value) 
{
  tree_type tree_node(value);  // search node
  return ordered_iterator(ordered_children.find(&tree_node));
}

// find_ordered(const stored_type&) const 
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
typename tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::const_ordered_iterator 
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::find_ordered(const stored_type& value) const
{
  tree_type tree_node(value);  // search node
  return const_ordered_iterator(ordered_children.find(&tree_node));
}

// erase(const stored_type&)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
bool tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::
erase(const stored_type& value)
{
  const associative_iterator_type it = find_deep(value);  // see if node is a descendant
  if (it != associative_tree_type::end()) {
    tree_type* const pParent = it.node()->parent();  // it is.  get it's parent
    tree_type* pAncestor = pParent;

    while (pAncestor) {  // update all ancestors of removed child
      pAncestor->descendents.erase(it.node());
      pAncestor = pAncestor->parent();
    }

    tree_type* const pNode = it.node();
    pParent->ordered_children.erase(pNode);
    dynamic_cast<associative_tree_type*>(pParent)->erase(*pNode->get()); // erase node

    return true;
  }

  return false;
}

// erase(iterator)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::erase(associative_iterator_type it) 
{
  tree_type* pAncestor = this;

  while (pAncestor) { // update all ancestors of removed child
    pAncestor->descendents.erase(it.node());
    pAncestor = pAncestor->parent();
  }

  tree_type* pNode = it.node();
  ordered_children.erase(pNode);

  associative_tree_type::erase(*pNode->get()); 
}

// erase(iterator, iterator)
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::erase(associative_iterator_type it_beg, associative_iterator_type it_end) 
{
  while (it_beg != it_end) {
    erase(it_beg++);
  }
}

// check_for_duplicate()
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
bool tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::check_for_duplicate(const stored_type& value, const tree_type* pParent) const
{
  while (pParent && !pParent->is_root()) {  // find root node
    pParent = pParent->parent();
  }

  // check if node is root
  if (!(value < *pParent->get()) && !(*pParent->get() < value))
    return true;

  associative_const_iterator_type it = pParent->find_deep(value);  // check if node is descendant of root
  associative_const_iterator_type it_end = pParent->end();

  return( it != it_end );  
}

// get_root()
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
const tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>*
tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::get_root() const
{
  const tree_type* pParent = this;

  while (pParent->parent()) {  // traverse up to root
    pParent = pParent->parent();
  }

  return pParent;
}

// swap
template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::swap(tree_type& rhs)
{
  tree_type temp(*this);

  clear();
  *this = rhs;

  rhs.clear();
  rhs = temp;
}

template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::reindex_children()
{
  ordered_children.clear();
  for (associative_iterator_type it = associative_tree_type::begin(); it != associative_tree_type::end(); it++)   {
    ordered_children.insert(it.node());
  }
}

template<typename stored_type, typename node_compare_type, typename node_order_compare_type>
void tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>::reindex_descendants()
{
  // reorder immediate node
  reindex_children();

  // reorder descendants
  typename associative_tree_type::pre_order_iterator it = pre_order_begin();
  const typename associative_tree_type::pre_order_iterator it_end = pre_order_end();

  for ( ; it != it_end; ++it ) 
  {
    it.node()->reindex_children();
  }
}
