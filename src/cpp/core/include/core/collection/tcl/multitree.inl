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
template<typename stored_type, typename node_compare_type>
tcl::multitree<stored_type, node_compare_type>::multitree( const tree_type& rhs ) : associative_tree_type(rhs)
{
  typename associative_tree_type::const_iterator it = rhs.begin();
  const typename associative_tree_type::const_iterator it_end = rhs.end();
  for (; it != it_end; ++it)  // do deep copy by inserting children (and descendants)
  {
    associative_tree_type::insert(*it.node(), this);
  }
}


// assignment operator
template<typename stored_type, typename node_compare_type>
tcl::multitree<stored_type, node_compare_type>& 
tcl::multitree<stored_type, node_compare_type>::operator = (const tree_type& rhs)
{
  if (!associative_tree_type::is_root()) // can assign only to root node
    return *this;

  if (this == &rhs) // check for self assignment
    return *this;

  associative_tree_type::clear();
  basic_tree_type::operator =(rhs);  // call base class operation

  typename associative_tree_type::const_iterator it = rhs.begin();
  const typename associative_tree_type::const_iterator it_end = rhs.end();
  for (; it != it_end; ++it)  // insert children and descendants
  {
    associative_tree_type::insert(*it.node(), this);
  }
  return *this;
}

// swap
template<typename stored_type, typename node_compare_type>
void tcl::multitree<stored_type, node_compare_type>::swap(tree_type& rhs)
{
  tree_type temp(*this);

  associative_tree_type::clear();
  *this = rhs;

  rhs.clear();
  rhs = temp;
}

