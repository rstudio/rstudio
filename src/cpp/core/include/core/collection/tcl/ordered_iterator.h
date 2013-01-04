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
#include "child_iterator.h"

namespace tcl 
{
  template<typename stored_type, typename node_compare_type, typename node_order_compare_type> class unique_tree;
  template< typename stored_type, typename tree_type,  typename container_type > class associative_tree;
  template<typename tree_type, typename node_order_compare_type> struct deref_ordered_compare;
  template<typename stored_type, typename node_compare_type, typename node_order_compare_type > struct unique_tree_deref_less;

  template<typename T, typename U, typename V, typename W, typename X, typename Y> class unique_tree_ordered_iterator;


  template<typename ST, typename NCT, typename NOCT, typename TPT1, typename PT1, typename RT1, typename TPT2, typename PT2, typename RT2>
    void ordered_it_init(unique_tree_ordered_iterator<ST, NCT, NOCT, TPT1, PT1, RT1>* dest, const unique_tree_ordered_iterator<ST, NCT, NOCT, TPT2, PT2, RT2>& src) { dest->it = src.it; }

  template<typename ST, typename NCT, typename NOCT, typename TPT1, typename PT1, typename RT1, typename TPT2, typename PT2, typename RT2>
    bool ordered_it_eq(const unique_tree_ordered_iterator<ST, NCT, NOCT, TPT1, PT1, RT1>* lhs, const unique_tree_ordered_iterator<ST, NCT, NOCT, TPT2, PT2, RT2>& rhs) { return lhs->it == rhs.it; }
}


/************************************************************************/
/* ordered iterator (for unique_tree)                                   */
/************************************************************************/


template<typename stored_type, typename node_compare_type, typename node_order_compare_type, typename tree_pointer_type, typename pointer_type, typename reference_type>
class tcl::unique_tree_ordered_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::bidirectional_iterator_tag, stored_type>
#else
  : public std::iterator<std::bidirectional_iterator_tag, stored_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
protected:
  // typedefs
  typedef unique_tree<stored_type, node_compare_type, node_order_compare_type> tree_type;
  typedef std::multiset<tree_type*, deref_ordered_compare<tree_type, node_order_compare_type> > ordered_container_type;

public:
  // constructors/destructor
  unique_tree_ordered_iterator() {}
  // conversion constructor for const_iterator, copy constructor for iterator
  unique_tree_ordered_iterator(const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>& src) { ordered_it_init(this, src); }
  explicit unique_tree_ordered_iterator(const typename ordered_container_type::const_iterator& iter) : it(iter) {}
  virtual ~unique_tree_ordered_iterator() {}

  // overloaded operators
  unique_tree_ordered_iterator& operator ++() { ++it; return *this;}
  unique_tree_ordered_iterator operator ++(int) { unique_tree_ordered_iterator old(*this); ++*this; return old;}
  unique_tree_ordered_iterator& operator --() { --it; return *this;}
  unique_tree_ordered_iterator operator --(int) { unique_tree_ordered_iterator old(*this); --*this; return old;}

  bool operator == (const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>& rhs) const { return ordered_it_eq(this, rhs); }
  bool operator != (const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>& rhs) const { return !(*this == rhs); }

  reference_type operator*() const { return  const_cast<reference_type>(*(*it)->get());}
  pointer_type operator->() const { return const_cast<pointer_type>((*it)->get());}

  tree_pointer_type node() const { return const_cast<tree_pointer_type>(*it);}

  // data
protected:
  typename ordered_container_type::const_iterator it;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend void ordered_it_init(unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>&);
    friend void ordered_it_init(unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>&);
    friend bool ordered_it_eq(const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>&);
    friend bool ordered_it_eq(const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>&);
  #else
    friend void ordered_it_init<>(unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>&);
    friend void ordered_it_init<>(unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>&);
    friend bool ordered_it_eq<>(const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>&);
    friend bool ordered_it_eq<>(const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, tree_type*, stored_type*, stored_type&>*, const unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const tree_type*, const stored_type*, const stored_type&>&);
  #endif
};


