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

// static member variable definition
template< typename stored_type, typename tree_type, typename container_type >
typename tcl::basic_tree<stored_type, tree_type, container_type>::tClone_fcn
tcl::basic_tree<stored_type, tree_type, container_type>::pClone_fcn = 0;

// constructor
template< typename stored_type, typename tree_type, typename container_type >
tcl::basic_tree<stored_type, tree_type, container_type>::basic_tree(const stored_type& value) 
:   children(container_type()), pElement(0), pParent_node(0), 
stored_type_allocator(std::allocator<stored_type>()), tree_type_allocator(std::allocator<tree_type>())
{
  // use clone function if available
  if (pClone_fcn)
    pElement = pClone_fcn(value);
  else
    allocate_stored_type(pElement, value);
}


// copy constructor
template< typename stored_type, typename tree_type, typename container_type >
tcl::basic_tree<stored_type, tree_type, container_type>::basic_tree(const basic_tree_type& rhs) 
:   children(container_type()), pElement(0), pParent_node(0), 
stored_type_allocator(std::allocator<stored_type>()), tree_type_allocator(std::allocator<tree_type>())
{
  pParent_node = 0; // new tree obj is always root node
  set(*rhs.get()); // set data obj
}

// assignment operator
template< typename stored_type, typename tree_type, typename container_type >
tcl::basic_tree<stored_type, tree_type, container_type>& tcl::basic_tree<stored_type, tree_type, container_type>::operator = (const basic_tree_type& rhs)
{
  if (&rhs == this)
    return *this;

  set(*rhs.get());  // set data obj

  return *this;
}

// destructor
template< typename stored_type, typename tree_type, typename container_type >
tcl::basic_tree<stored_type, tree_type, container_type>::~basic_tree()
{
  deallocate_stored_type(pElement);
}



// set(stored_type&)
template< typename stored_type, typename tree_type, typename container_type >
void  tcl::basic_tree<stored_type, tree_type, container_type>::set(const stored_type& value) 
{ 
  if (pElement) // if data node already exists, free memory
    deallocate_stored_type(pElement);

  if (pClone_fcn)  // use clone fcn if available
    pElement = pClone_fcn(value);
  else
    allocate_stored_type(pElement, value);
}




