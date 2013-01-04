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
#include "child_node_iterator.h"
#include <set>
#include <stack>
#include <queue>
#include <algorithm>

namespace tcl 
{
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class pre_order_descendant_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class post_order_descendant_node_iterator;
  template<typename T, typename U, typename V, typename W, typename X, typename Y> class level_order_descendant_node_iterator;

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    void pre_order_node_it_init(pre_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* dest, const pre_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& src) 
  { 
    dest->it = src.it; 
    dest->pTop_node = src.pTop_node; 
    dest->at_top = src.at_top; 
    std::stack<typename TT::node_iterator> s(src.node_stack); 
    std::stack<typename TT::node_iterator> temp;  
    while (!s.empty()) { temp.push(s.top()); s.pop(); } 
    while (!temp.empty()) { dest->node_stack.push(temp.top()); temp.pop(); } 
  }

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    bool pre_order_node_it_eq(const pre_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* lhs, const pre_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& rhs) { return lhs->it == rhs.it && lhs->at_top == rhs.at_top; }

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    void post_order_node_it_init(post_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* dest, const post_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& src) 
  { 
    dest->it = src.it; 
    dest->pTop_node = src.pTop_node; 
    dest->at_top = src.at_top; 
    std::stack<typename TT::node_iterator> s(src.node_stack); 
    std::stack<typename TT::node_iterator> temp;  
    while (!s.empty()) { temp.push(s.top()); s.pop(); } 
    while (!temp.empty()) { dest->node_stack.push(temp.top()); temp.pop(); } 
  }

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    bool post_order_node_it_eq(const post_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* lhs, const post_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& rhs) { return lhs->it == rhs.it && lhs->at_top == rhs.at_top; }

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    void level_order_node_it_init(level_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* dest, const level_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& src) 
  { 
    dest->it = src.it; 
    dest->pTop_node = src.pTop_node; 
    dest->at_top = src.at_top; 
    std::queue<typename TT::node_iterator> temp(src.node_queue); 
    while (!temp.empty()) {dest->node_queue.push(temp.front()); temp.pop();}
  }

  template<typename ST, typename TT, typename CT, typename BIT1, typename PT1, typename RT1, typename BIT2, typename PT2, typename RT2>
    bool level_order_node_it_eq(const level_order_descendant_node_iterator<ST, TT, CT, BIT1, PT1, RT1>* lhs, const level_order_descendant_node_iterator<ST, TT, CT, BIT2, PT2, RT2>& rhs) { return lhs->it == rhs.it && lhs->at_top == rhs.at_top; }

}

/************************************************************************/
/* pre_order_descendant_node_iterator                                   */
/************************************************************************/

template<typename stored_type, typename tree_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
class tcl::pre_order_descendant_node_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::bidirectional_iterator_tag, tree_type>
#else
  : public std::iterator<std::bidirectional_iterator_tag, tree_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
public:
  // constructors/destructor
  pre_order_descendant_node_iterator() : pTop_node(0), at_top(false) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  pre_order_descendant_node_iterator(const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>& src) { pre_order_node_it_init(this, src); }
  // destructor, copy constructor, and assignment operator will be compiler generated correctly
protected:
  explicit pre_order_descendant_node_iterator(pointer_type pCalled_node, bool beg) : it(beg ? pCalled_node->node_begin() : pCalled_node->node_end()), pTop_node(pCalled_node), at_top(beg) {}

public:
  // overloaded operators
  pre_order_descendant_node_iterator& operator ++(); 
  pre_order_descendant_node_iterator operator ++(int) { pre_order_descendant_node_iterator old(*this); ++*this; return old;}
  pre_order_descendant_node_iterator& operator --();
  pre_order_descendant_node_iterator operator --(int) { pre_order_descendant_node_iterator old(*this); --*this; return old;}

  reference_type operator*() const { return at_top ? *pTop_node : it.operator *();}
  pointer_type operator->() const { return at_top ? pTop_node : it.operator ->();}
  base_iterator_type base() const { return it; }

  // comparison operators
  bool operator == (const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return pre_order_node_it_eq(this, rhs); }
  bool operator != (const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return !(*this == rhs); }

private:
  // data
  base_iterator_type it;
  std::stack<base_iterator_type> node_stack;   
  pointer_type pTop_node;
  typename container_type::const_reverse_iterator rit;
  bool at_top;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    typedef tree_type tr_type;
    friend typename tr_type;
    friend class sequential_tree<stored_type>;
    friend void pre_order_node_it_init(pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void pre_order_node_it_init(pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool pre_order_node_it_eq(const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool pre_order_node_it_eq(const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #else
    template<typename T, typename U> friend class tree;
    template<typename T, typename U> friend class multitree;
    template<typename T, typename U, typename V> friend class unique_tree;
    template<typename T> friend class sequential_tree;
    friend void pre_order_node_it_init<>(pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void pre_order_node_it_init<>(pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool pre_order_node_it_eq<>(const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool pre_order_node_it_eq<>(const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const pre_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #endif
};


/************************************************************************/
/* post_order_descendant_node_iterator                                  */
/************************************************************************/

template<typename stored_type, typename tree_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
class tcl::post_order_descendant_node_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::bidirectional_iterator_tag, tree_type>
#else
  : public std::iterator<std::bidirectional_iterator_tag, tree_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
public:
  // constructors/destructor
  post_order_descendant_node_iterator() : pTop_node(0), at_top(false) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  post_order_descendant_node_iterator(const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>& src) { post_order_node_it_init(this, src); }
  // destructor, copy constructor, and assignment operator will be compiler generated correctly
private:
  explicit post_order_descendant_node_iterator(pointer_type pCalled_node, bool beg); 

public:
  // overloaded operators
  post_order_descendant_node_iterator& operator ++(); 
  post_order_descendant_node_iterator operator ++(int) { post_order_descendant_node_iterator old(*this); ++*this; return old;}
  post_order_descendant_node_iterator& operator --(); 
  post_order_descendant_node_iterator operator --(int) { post_order_descendant_node_iterator old(*this); --*this; return old;}

  // public interface
  reference_type operator*() const { return at_top ? *pTop_node : it.operator *();}
  pointer_type operator->() const { return at_top ? pTop_node : it.operator ->();}
  base_iterator_type base() const { return it; }

  // comparison operators
  bool operator == (const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return post_order_node_it_eq(this, rhs); }
  bool operator != (const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return !(*this == rhs); }

private:
  // data
  std::stack<base_iterator_type> node_stack;   
  base_iterator_type it;
  pointer_type pTop_node;
  typename container_type::const_reverse_iterator rit;
  bool at_top;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    typedef tree_type tr_type;
    friend typename tr_type;
    friend class sequential_tree<stored_type>;
    friend void post_order_node_it_init(post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void post_order_node_it_init(post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool post_order_node_it_eq(const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool post_order_node_it_eq(const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #else
    template<typename T, typename U> friend class tree;
    template<typename T, typename U> friend class multitree;
    template<typename T, typename U, typename V> friend class unique_tree;
    template<typename T> friend class sequential_tree;
    friend void post_order_node_it_init<>(post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void post_order_node_it_init<>(post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool post_order_node_it_eq<>(const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool post_order_node_it_eq<>(const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const post_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #endif
};



/************************************************************************/
/* level_order_descendant_node_iterator                                 */
/************************************************************************/

template<typename stored_type, typename tree_type, typename container_type, typename base_iterator_type, typename pointer_type, typename reference_type>
class tcl::level_order_descendant_node_iterator 
#if defined(_MSC_VER) && _MSC_VER < 1300
  : public std::iterator<std::forward_iterator_tag, tree_type>
#else
  : public std::iterator<std::forward_iterator_tag, tree_type, ptrdiff_t, pointer_type, reference_type>
#endif
{
public:
  // constructors/destructor
  level_order_descendant_node_iterator() : pTop_node(0), at_top(false) {}
  // conversion constructor for const_iterator, copy constructor for iterator
  level_order_descendant_node_iterator(const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>& src) { level_order_node_it_init(this, src); }
  // destructor, copy constructor, and assignment operator will be compiler generated correctly
private:
  explicit level_order_descendant_node_iterator(pointer_type pCalled_node, bool beg) : it(beg ? pCalled_node->node_begin() : pCalled_node->node_end()), pTop_node(pCalled_node), at_top(beg) {}

public:
  // overloaded operators
  level_order_descendant_node_iterator& operator ++();
  level_order_descendant_node_iterator operator ++(int) { level_order_descendant_node_iterator old(*this); ++*this; return old;}

  // public interface
  reference_type operator*() const { return at_top ? const_cast<reference_type>(*pTop_node) : const_cast<reference_type>(it.operator *());}
  pointer_type operator->() const { return at_top ? const_cast<pointer_type>(pTop_node) : const_cast<pointer_type>(it.operator ->());}
  base_iterator_type base() const { return it; }

  // comparison operators
  bool operator == (const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return level_order_node_it_eq(this, rhs); }
  bool operator != (const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>& rhs) const { return !(*this == rhs); }

private:
  // data
  base_iterator_type it;
  std::queue<base_iterator_type> node_queue;
  pointer_type pTop_node;
  bool at_top;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    typedef tree_type tr_type;
    friend typename tr_type;
    friend class sequential_tree<stored_type>;
    friend void level_order_node_it_init(level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void level_order_node_it_init(level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool level_order_node_it_eq(const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool level_order_node_it_eq(const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #else
    template<typename T, typename U> friend class tree;
    template<typename T, typename U> friend class multitree;
    template<typename T, typename U, typename V> friend class unique_tree;
    template<typename T> friend class sequential_tree;
    friend void level_order_node_it_init<>(level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend void level_order_node_it_init<>(level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>&);
    friend bool level_order_node_it_eq<>(const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
    friend bool level_order_node_it_eq<>(const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::node_iterator, tree_type*, tree_type&>*, const level_order_descendant_node_iterator<stored_type, tree_type, container_type, typename tree_type::const_node_iterator, const tree_type*, const tree_type&>&);
  #endif
};



#include "descendant_node_iterator.inl"
