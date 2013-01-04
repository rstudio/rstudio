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
#pragma once
#include <set>
#include <stack>
#include <queue>
#include <algorithm>
#include <limits>

namespace tcl 
{
  template<typename T, typename U, typename V> class basic_tree;
}

// stored_type:         type stored in container
// tree_type:           one of three tree types derived from this base
// container_type:      type of contain to hold children (can be set or multiset)

template< typename stored_type, typename tree_type,  typename container_type >
class tcl::basic_tree 
{
public:
  // typedefs
  typedef basic_tree<stored_type, tree_type, container_type> basic_tree_type;
  typedef stored_type* (*tClone_fcn) (const stored_type&);
  typedef stored_type value_type;
  typedef stored_type& reference;
  typedef const stored_type& const_reference;
  typedef size_t size_type;
  typedef std::allocator<stored_type> allocator_type;
  typedef typename allocator_type::difference_type difference_type;

protected:
  // constructors/destructor
  basic_tree() : pElement(0), pParent_node(0)  {}
  explicit basic_tree(const stored_type& value);
  basic_tree(const basic_tree_type& rhs);  // copy constructor
  virtual ~basic_tree();

public:
  // public interface
  const stored_type* get() const { return pElement;}
  stored_type* get() { return pElement;}
  bool is_root() const { return pParent_node == 0;}
  size_type size() const { return children.size();}
  size_type max_size() const { return(std::numeric_limits<int>().max)();}
  bool empty() const { return children.empty();}
  tree_type* parent() { return pParent_node;}
  const tree_type* parent() const { return pParent_node;}
  static void set_clone(const tClone_fcn& fcn) { pClone_fcn = fcn;}


protected:
  void set_parent(tree_type* pParent) { pParent_node = pParent;}
  basic_tree_type& operator = (const basic_tree_type& rhs); // assignment operator
  void set(const stored_type& stored_obj);
  void allocate_stored_type(stored_type*& element_ptr, const stored_type& value) 
  { element_ptr = stored_type_allocator.allocate(1,0); stored_type_allocator.construct(element_ptr, value);}
  void deallocate_stored_type(stored_type* element_ptr) 
  { stored_type_allocator.destroy(element_ptr); stored_type_allocator.deallocate(element_ptr, 1);}
  void allocate_tree_type(tree_type*& tree_ptr, const tree_type& tree_obj)
  { tree_ptr = tree_type_allocator.allocate(1,0); tree_type_allocator.construct(tree_ptr, tree_obj);}
  void deallocate_tree_type(tree_type* tree_ptr)
  { tree_type_allocator.destroy(tree_ptr); tree_type_allocator.deallocate(tree_ptr, 1);}

  // data
protected:
  container_type children;
private:
  stored_type* pElement;   // data accessor
  mutable tree_type* pParent_node;
  static tClone_fcn pClone_fcn;
  std::allocator<stored_type> stored_type_allocator;
  std::allocator<tree_type> tree_type_allocator;
};

#include "basic_tree.inl"
