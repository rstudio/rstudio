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
#include <iterator>

namespace tcl 
{
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class associative_reverse_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class const_sequential_reverse_iterator;
}



template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename pointer_type, typename reference_type>
class tcl::associative_reverse_iterator : public tcl::associative_iterator<stored_type, tree_type, tree_pointer_type, container_type, pointer_type, reference_type>
{
  typedef associative_iterator<stored_type, tree_type, tree_pointer_type, container_type, pointer_type, reference_type> associative_iterator_type;
public:
  associative_reverse_iterator() : associative_iterator_type() {}
  explicit associative_reverse_iterator(const associative_iterator_type& _it) : associative_iterator_type(_it) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  associative_reverse_iterator(const associative_reverse_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>& src) : associative_iterator_type(src) {}

  reference_type operator*() const { associative_iterator_type tmp(*this);  return(*--tmp);}
  pointer_type operator->() const { associative_iterator_type tmp(*this); --tmp; return tmp.operator ->();}
  associative_reverse_iterator& operator ++() { associative_iterator_type::operator --(); return *this;}
  associative_reverse_iterator operator ++(int) { associative_reverse_iterator old(*this); ++*this; return old;}
  associative_reverse_iterator& operator --() { associative_iterator_type::operator ++(); return *this;}
  associative_reverse_iterator operator --(int) { associative_reverse_iterator old(*this); --*this; return old;}

  tree_pointer_type node() const { associative_iterator_type tmp(*this); --tmp; return tmp.node();}
  associative_iterator_type base() const { return associative_iterator_type(*this);}
};




template<typename stored_type, typename tree_type, typename tree_pointer_type, typename container_type, typename pointer_type, typename reference_type>
class tcl::sequential_reverse_iterator : public tcl::sequential_iterator<stored_type, tree_type, tree_pointer_type, container_type, pointer_type, reference_type>
{
  typedef sequential_iterator<stored_type, tree_type, tree_pointer_type, container_type, pointer_type, reference_type> sequential_iterator_type;
public:
  sequential_reverse_iterator() : sequential_iterator_type() {}
  explicit sequential_reverse_iterator(const sequential_iterator_type& _it) : sequential_iterator_type(_it) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  sequential_reverse_iterator(const sequential_reverse_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>& src) : sequential_iterator_type(src) {}

  reference_type operator*() const { sequential_iterator_type tmp(*this);  return(*--tmp);}
  pointer_type operator->() const { sequential_iterator_type tmp(*this); --tmp; return tmp.operator ->();}
  sequential_reverse_iterator& operator ++() { sequential_iterator_type::operator --(); return *this;}
  sequential_reverse_iterator operator ++(int) { sequential_reverse_iterator old(*this); ++*this; return old;}
  sequential_reverse_iterator& operator --() { sequential_iterator_type::operator ++(); return *this;}
  sequential_reverse_iterator operator --(int) { sequential_reverse_iterator old(*this); --*this; return old;}

  tree_pointer_type node() const { sequential_iterator_type tmp(*this); --tmp; return tmp.node();}
  sequential_iterator_type base() const { return sequential_iterator_type(*this);}
};

