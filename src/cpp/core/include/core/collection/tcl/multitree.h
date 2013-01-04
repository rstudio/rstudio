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
#include "associative_tree.h"
#include <set>

namespace tcl
{
  // forward declaration for deref comparison functor
  template<typename stored_type, typename node_compare_type > class multitree;

  // deref comparison functor, derive from binary function per Scott Meyer
  template<typename stored_type, typename node_compare_type >
  struct multitree_deref_less : public std::binary_function<const multitree<stored_type, node_compare_type>*, const multitree<stored_type, node_compare_type>*, bool>
  {
    bool operator () (const multitree<stored_type, node_compare_type>* lhs, const multitree<stored_type, node_compare_type>* rhs) const
    {
      // call < on actual object
      return node_compare_type()(*lhs->get(), *rhs->get());
    }
  };
}



// node object type.  forwards most operations to base_tree_type, 
// instanciates base_tree_type with type of container (set of unique_tree ptrs) to use for node and key comparisons
template<typename stored_type, typename node_compare_type = std::less<stored_type> >
class tcl::multitree : public tcl::associative_tree<stored_type, tcl::multitree<stored_type, node_compare_type>,  std::multiset<tcl::multitree<stored_type, node_compare_type>*, tcl::multitree_deref_less<stored_type, node_compare_type> > >
{
public:
  // typedefs
  typedef multitree<stored_type, node_compare_type> tree_type;
  typedef multitree_deref_less<stored_type, node_compare_type> key_compare;
  typedef multitree_deref_less<stored_type, node_compare_type> value_compare;
  typedef std::multiset<tree_type*, key_compare> container_type;
  typedef basic_tree<stored_type, tree_type, container_type> basic_tree_type;
  typedef associative_tree<stored_type, tree_type, container_type> associative_tree_type;

  // constructors/destructor
  explicit multitree( const stored_type& value = stored_type() ) : associative_tree_type(value) {}
  template<typename iterator_type> multitree(iterator_type it_beg, iterator_type it_end, const stored_type& value = stored_type()) : associative_tree_type(value) { while (it_beg != it_end) { 
      insert(*it_beg); ++it_beg;
    }}
  multitree( const tree_type& rhs ); // copy constructor
  ~multitree() { associative_tree_type::clear();}

  // assignment operator
  tree_type& operator = (const tree_type& rhs);

  // public interface
public:
  typename associative_tree_type::iterator insert(const stored_type& value) { return associative_tree_type::insert(value, this);}
  typename associative_tree_type::iterator insert(const typename associative_tree_type::const_iterator pos, const stored_type& value) { return associative_tree_type::insert(pos, value, this);}
  typename associative_tree_type::iterator insert(const tree_type& tree_obj ) { return associative_tree_type::insert(tree_obj, this);}
  typename associative_tree_type::iterator insert(const typename associative_tree_type::const_iterator pos, const tree_type& tree_obj) { return associative_tree_type::insert(pos, tree_obj, this);}
#if !defined(_MSC_VER) || _MSC_VER >= 1300 // insert range not available for VC6
  template<typename iterator_type> void insert(iterator_type it_beg, iterator_type it_end) { while (it_beg != it_end) insert(*it_beg++);}
#endif
  void swap(tree_type& rhs);

  // descendant element iterator accessors
  typedef typename associative_tree_type::post_order_iterator post_order_iterator_type;
  typedef typename associative_tree_type::const_post_order_iterator const_post_order_iterator_type;
  typedef typename associative_tree_type::pre_order_iterator pre_order_iterator_type;
  typedef typename associative_tree_type::const_pre_order_iterator const_pre_order_iterator_type;
  typedef typename associative_tree_type::level_order_iterator level_order_iterator_type;
  typedef typename associative_tree_type::const_level_order_iterator const_level_order_iterator_type;

  pre_order_iterator_type pre_order_begin() { return pre_order_iterator_type(this, true);}
  pre_order_iterator_type pre_order_end() { return pre_order_iterator_type(this, false);}
  const_pre_order_iterator_type pre_order_begin() const { return const_pre_order_iterator_type(this, true);}
  const_pre_order_iterator_type pre_order_end() const { return const_pre_order_iterator_type(this, false);}
  post_order_iterator_type post_order_begin() { return post_order_iterator_type(this, true);}
  post_order_iterator_type post_order_end() { return post_order_iterator_type(this, false);}
  const_post_order_iterator_type post_order_begin() const { return const_post_order_iterator_type(this, true);}
  const_post_order_iterator_type post_order_end() const { return const_post_order_iterator_type(this, false);}
  level_order_iterator_type level_order_begin() { return level_order_iterator_type(this, true);}
  level_order_iterator_type level_order_end() { return level_order_iterator_type(this, false);}
  const_level_order_iterator_type level_order_begin() const { return const_level_order_iterator_type(this, true);}
  const_level_order_iterator_type level_order_end() const { return const_level_order_iterator_type(this, false);}

  // descendant node iterator accessors
  typedef typename associative_tree_type::pre_order_node_iterator pre_order_node_iterator_type;
  typedef typename associative_tree_type::const_pre_order_node_iterator const_pre_order_node_iterator_type;
  typedef typename associative_tree_type::post_order_node_iterator post_order_node_iterator_type;
  typedef typename associative_tree_type::const_post_order_node_iterator const_post_order_node_iterator_type;
  typedef typename associative_tree_type::level_order_node_iterator level_order_node_iterator_type;
  typedef typename associative_tree_type::const_level_order_node_iterator const_level_order_node_iterator_type;

  pre_order_node_iterator_type pre_order_node_begin() { return pre_order_node_iterator_type(this, true);}
  pre_order_node_iterator_type pre_order_node_end() { return pre_order_node_iterator_type(this, false);}
  const_pre_order_node_iterator_type pre_order_node_begin() const { return const_pre_order_node_iterator_type(this, true);}
  const_pre_order_node_iterator_type pre_order_node_end() const { return const_pre_order_node_iterator_type(this, false);}
  post_order_node_iterator_type post_order_node_begin() { return post_order_node_iterator_type(this, true);}
  post_order_node_iterator_type post_order_node_end() { return post_order_node_iterator_type(this, false);}
  const_post_order_node_iterator_type post_order_node_begin() const { return const_post_order_node_iterator_type(this, true);}
  const_post_order_node_iterator_type post_order_node_end() const { return const_post_order_node_iterator_type(this, false);}
  level_order_node_iterator_type level_order_node_begin() { return level_order_node_iterator_type(this, true);}
  level_order_node_iterator_type level_order_node_end() { return level_order_node_iterator_type(this, false);}
  const_level_order_node_iterator_type level_order_node_begin() const { return const_level_order_node_iterator_type(this, true);}
  const_level_order_node_iterator_type level_order_node_end() const { return const_level_order_node_iterator_type(this, false);}

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend class basic_tree<stored_type, tree_type, container_type>;
  #else
    template<typename T, typename U, typename V> friend class basic_tree;
  #endif
};


#include "multitree.inl"
