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
#include <iterator>

namespace tcl 
{
  // forward declaration
  template<typename T, typename U, typename V> class basic_tree;
  template<typename T> class sequential_tree;
  template<typename T, typename U, typename V> class associative_tree;
  template<typename T, typename U, typename V, typename W, typename X, typename Z> class associative_reverse_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Z> class sequential_reverse_iterator;
  template<typename T, typename U, typename V, typename W, typename X> class associative_reverse_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X> class sequential_reverse_node_iterator;

  template<typename T, typename U, typename V, typename W, typename X> class associative_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X> class sequential_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class pre_order_descendant_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class post_order_descendant_node_iterator;

  template<typename ST, typename TT, typename CT, typename PT1, typename RT1, typename PT2, typename RT2>
    void associative_node_it_init(associative_node_iterator<ST, TT, CT, PT1, RT1>* dest, const associative_node_iterator<ST, TT, CT, PT2, RT2>& src) { dest->it = src.it; dest->pParent = src.pParent; }

  template<typename ST, typename TT, typename CT, typename PT1, typename RT1, typename PT2, typename RT2>
    bool associative_node_it_eq(const associative_node_iterator<ST, TT, CT, PT1, RT1>* lhs, const associative_node_iterator<ST, TT, CT, PT2, RT2>& rhs) { return lhs->pParent == rhs.pParent && lhs->it == rhs.it; }

  template<typename ST, typename TT, typename CT, typename PT1, typename RT1, typename PT2, typename RT2>
    void sequential_node_it_init(sequential_node_iterator<ST, TT, CT, PT1, RT1>* dest, const sequential_node_iterator<ST, TT, CT, PT2, RT2>& src) { dest->it = src.it; dest->pParent = src.pParent; }

  template<typename ST, typename TT, typename CT, typename PT1, typename RT1, typename PT2, typename RT2>
    bool sequential_node_it_eq(const sequential_node_iterator<ST, TT, CT, PT1, RT1>* lhs, const sequential_node_iterator<ST, TT, CT, PT2, RT2>& rhs) { return lhs->pParent == rhs.pParent && lhs->it == rhs.it; }

  template<typename ST, typename TT, typename CT, typename PT1, typename RT1, typename PT2, typename RT2>
    bool sequential_node_it_less(const sequential_node_iterator<ST, TT, CT, PT1, RT1>* lhs, const sequential_node_iterator<ST, TT, CT, PT2, RT2>& rhs) { return lhs->pParent == rhs.pParent && lhs->it < rhs.it; }
}


/************************************************************************/
/* associative tree child node iterators                                */
/************************************************************************/


template<typename stored_type, typename tree_type, typename container_type, typename pointer_type, typename reference_type>
class tcl::associative_node_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::bidirectional_iterator_tag, tree_type>
#else
  : public std::iterator<std::bidirectional_iterator_tag, tree_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
public:
  // constructors/destructor
  associative_node_iterator() : pParent(0) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  associative_node_iterator(const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>& src) { associative_node_it_init(this, src); }
protected:
#if defined(_MSC_VER) && _MSC_VER < 1300
public:
#endif
  explicit associative_node_iterator(const typename container_type::const_iterator& iter, const associative_tree<stored_type, tree_type, container_type>* pCalled_node) : it(iter), pParent(pCalled_node) {}
  // destructor, copy constructor, and assignment operator will be compiler generated correctly

public:
  // overloaded operators
  reference_type operator*() const { return  const_cast<reference_type>(*(*it));}
  pointer_type operator->() const { return const_cast<pointer_type>(*it);}

  associative_node_iterator& operator ++() { ++it; return *this;}
  associative_node_iterator operator ++(int) { associative_node_iterator old(*this); ++*this; return old;}
  associative_node_iterator& operator --() { --it; return *this;}
  associative_node_iterator operator --(int) { associative_node_iterator old(*this); --*this; return old;}

  // comparison operators
  bool operator == (const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return associative_node_it_eq(this, rhs); }
  bool operator != (const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return !(*this == rhs); }

protected:
  // data
  typename container_type::const_iterator it;
  const associative_tree<stored_type, tree_type, container_type>* pParent;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend class basic_tree<stored_type, tree_type, container_type>;
    friend class associative_tree<stored_type, tree_type, container_type>;
    friend void associative_node_it_init(associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend void associative_node_it_init(associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend bool associative_node_it_eq(const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool associative_node_it_eq(const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
  #else
    template<typename T, typename U, typename V> friend class basic_tree;
    template<typename T, typename U, typename V> friend class associative_tree;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class pre_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class post_order_descendant_node_iterator;
    friend void associative_node_it_init<>(associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend void associative_node_it_init<>(associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend bool associative_node_it_eq<>(const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool associative_node_it_eq<>(const associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
  #endif
};




/************************************************************************/
/* sequential tree child iterators                                      */
/************************************************************************/

template<typename stored_type, typename tree_type, typename container_type, typename pointer_type, typename reference_type>
class tcl::sequential_node_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::random_access_iterator_tag, tree_type>
#else
  : public std::iterator<std::bidirectional_iterator_tag, tree_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
public:
  // constructors/destructor
  sequential_node_iterator() : pParent(0) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  sequential_node_iterator(const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>& src) { sequential_node_it_init(this, src); }
protected:
#if defined(_MSC_VER) && _MSC_VER < 1300
public:
#endif
  explicit sequential_node_iterator(typename container_type::const_iterator it_, const tree_type* pParent_) : it(it_), pParent(pParent_) {}
  // copy constructor, and assignment operator will be compiler generated correctly

public:
  // typedefs
  typedef size_t size_type;
#if defined(_MSC_VER) && _MSC_VER < 1300
  typedef std::iterator_traits<std::iterator<std::random_access_iterator_tag, tree_type> >::distance_type difference_type;
#else
  typedef typename std::iterator_traits<sequential_node_iterator>::difference_type difference_type;
#endif

  // overloaded operators
  reference_type operator*() const { return  const_cast<reference_type>(*(*it));}
  pointer_type operator->() const { return const_cast<pointer_type>(*it);}

  sequential_node_iterator& operator ++() { ++it; return *this;}
  sequential_node_iterator operator ++(int) { sequential_node_iterator old(*this); ++*this; return old;}
  sequential_node_iterator& operator --() { --it; return *this;}
  sequential_node_iterator operator --(int) { sequential_node_iterator old(*this); --*this; return old;}
  sequential_node_iterator& operator +=(size_type n) { it += n; return *this;}
  sequential_node_iterator& operator -=(size_type n) { it -= n; return *this;}
  difference_type operator -(const sequential_node_iterator& rhs) const { return it - rhs.it;}

  // comparison operators
  bool operator == (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return sequential_node_it_eq(this, rhs); }
  bool operator != (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return !(*this == rhs); }
  bool operator < (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return sequential_node_it_less(this, rhs); }
  bool operator <= (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return *this < rhs || *this == rhs; }
  bool operator > (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return !(*this <= rhs); }
  bool operator >= (const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>& rhs) const { return !(*this < rhs); }

protected:
  // data
  typename container_type::const_iterator it;
  const tree_type* pParent;

  // friends 
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend class sequential_tree<stored_type>;
    friend void sequential_node_it_init(sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend void sequential_node_it_init(sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend bool sequential_node_it_eq(const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_eq(const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_less(const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_less(const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
  #else
    template<typename T> friend class sequential_tree;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class pre_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class post_order_descendant_node_iterator;
    friend void sequential_node_it_init<>(sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend void sequential_node_it_init<>(sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>&);
    friend bool sequential_node_it_eq<>(const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_eq<>(const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_less<>(const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
    friend bool sequential_node_it_less<>(const sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>*, const sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>&);
  #endif
};









