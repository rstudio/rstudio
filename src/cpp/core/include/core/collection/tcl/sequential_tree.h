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
#include "basic_tree.h"
#include "child_iterator.h"
#include "child_node_iterator.h"
#include "descendant_iterator.h"
#include "descendant_node_iterator.h"
#include "reverse_iterator.h"
#include "reverse_node_iterator.h"
#include <vector>
#include <algorithm>
#include <stdexcept>

namespace tcl 
{
  template<typename T> class sequential_tree;

  // overloaded comparison operations
  template<typename T> bool operator == (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs);
  template<typename T> bool operator <  (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs);
  template<typename T> bool operator != (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs) { return !(lhs == rhs);}
  template<typename T> bool operator >  (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs) { return rhs < lhs;}
  template<typename T> bool operator <= (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs) { return !(rhs < lhs);}
  template<typename T> bool operator >= (const sequential_tree<T>& lhs, const sequential_tree<T>& rhs) { return !(lhs < rhs);}
}

template<typename stored_type>
class tcl::sequential_tree : public tcl::basic_tree<stored_type, sequential_tree<stored_type>, std::vector<sequential_tree<stored_type>* > >
{
public:
  // typedefs
  typedef sequential_tree<stored_type> tree_type;
  typedef std::vector<tree_type*> container_type;
  typedef basic_tree<stored_type, tree_type, container_type > basic_tree_type;
  typedef typename basic_tree_type::size_type basic_size_type;
  using basic_tree_type::size_type;

  // element iterator typedefs
  typedef sequential_iterator<stored_type, tree_type, const tree_type*, container_type, const stored_type*, const stored_type&>                             const_iterator;
  typedef sequential_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>                                               iterator;
  typedef sequential_reverse_iterator<stored_type, tree_type, const tree_type*, container_type, const stored_type*, const stored_type&>                     const_reverse_iterator;
  typedef sequential_reverse_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>                                       reverse_iterator;
  typedef pre_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&>   const_pre_order_iterator;
  typedef pre_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                           pre_order_iterator;
  typedef post_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&>  const_post_order_iterator;
  typedef post_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                          post_order_iterator;
  typedef level_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&> const_level_order_iterator;
  typedef level_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                         level_order_iterator;

  // node iterator typedefs
  typedef sequential_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>                                  const_node_iterator;
  typedef sequential_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>                                              node_iterator;
  typedef sequential_reverse_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>                          const_reverse_node_iterator;
  typedef sequential_reverse_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>                                      reverse_node_iterator;
  typedef pre_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&>   const_pre_order_node_iterator;
  typedef pre_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                     pre_order_node_iterator;
  typedef post_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&>  const_post_order_node_iterator;
  typedef post_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                    post_order_node_iterator;
  typedef level_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&> const_level_order_node_iterator;
  typedef level_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                   level_order_node_iterator;

  // constructors/destructor
  explicit sequential_tree(const stored_type& value = stored_type()) : basic_tree_type(value) {}
  explicit sequential_tree(typename basic_tree_type::size_type sz, const stored_type& value = stored_type()) : basic_tree_type(stored_type()) { insert(end(), sz, value);}
  sequential_tree(const tree_type& rhs);  // copy constructor
  template<typename iterator_type> sequential_tree(iterator_type it_beg, iterator_type it_end, const stored_type& value = stored_type()) : basic_tree_type(value) { while (it_beg != it_end) { 
      insert(*it_beg); ++it_beg;
    }}
  ~sequential_tree() {clear();}

  // assignment operator
  tree_type& operator = (const tree_type& rhs);

  // child element iterator accessors
  const_iterator begin() const { return const_iterator(basic_tree_type::children.begin(), this);}
  const_iterator end() const { return const_iterator(basic_tree_type::children.end(), this);}
  iterator begin() { return iterator(basic_tree_type::children.begin(), this);}
  iterator end() { return iterator(basic_tree_type::children.end(), this);}

  // child node iterator accessors
  const_node_iterator node_begin() const { return const_node_iterator(basic_tree_type::children.begin(), this);}
  const_node_iterator node_end() const { return const_node_iterator(basic_tree_type::children.end(), this);}
  node_iterator node_begin() { return node_iterator(basic_tree_type::children.begin(), this);}
  node_iterator node_end() { return node_iterator(basic_tree_type::children.end(), this);}


  // child element reverse iterator accessors
  const_reverse_iterator rbegin() const {return const_reverse_iterator(end());}
  const_reverse_iterator rend() const {return const_reverse_iterator(begin());}
  reverse_iterator rbegin() {return reverse_iterator(end());}
  reverse_iterator rend() { return reverse_iterator(begin());}

  // descendant element iterator accessors
  pre_order_iterator pre_order_begin() { return pre_order_iterator(this, true);}
  pre_order_iterator pre_order_end() { return pre_order_iterator(this, false);}
  const_pre_order_iterator pre_order_begin() const { return const_pre_order_iterator(this, true);}
  const_pre_order_iterator pre_order_end() const { return const_pre_order_iterator(this, false);}
  post_order_iterator post_order_begin() { return post_order_iterator(this, true);}
  post_order_iterator post_order_end() { return post_order_iterator(this, false);}
  const_post_order_iterator post_order_begin() const { return const_post_order_iterator(this, true);}
  const_post_order_iterator post_order_end() const { return const_post_order_iterator(this, false);}
  level_order_iterator level_order_begin() { return level_order_iterator(this, true);}
  level_order_iterator level_order_end() { return level_order_iterator(this, false);}
  const_level_order_iterator level_order_begin() const { return const_level_order_iterator(this, true);}
  const_level_order_iterator level_order_end() const { return const_level_order_iterator(this, false);}

  // child node reverse iterator accessors
  const_reverse_node_iterator node_rbegin() const {return const_reverse_node_iterator(node_end());}
  const_reverse_node_iterator node_rend() const {return const_reverse_node_iterator(node_begin());}
  reverse_node_iterator node_rbegin() {return reverse_node_iterator(node_end());}
  reverse_node_iterator node_rend() { return reverse_node_iterator(node_begin());}

  // descendant node iterator accessors
  pre_order_node_iterator pre_order_node_begin() { return pre_order_node_iterator(this, true);}
  pre_order_node_iterator pre_order_node_end() { return pre_order_node_iterator(this, false);}
  const_pre_order_node_iterator pre_order_node_begin() const { return const_pre_order_node_iterator(this, true);}
  const_pre_order_node_iterator pre_order_node_end() const { return const_pre_order_node_iterator(this, false);}
  post_order_node_iterator post_order_node_begin() { return post_order_node_iterator(this, true);}
  post_order_node_iterator post_order_node_end() { return post_order_node_iterator(this, false);}
  const_post_order_node_iterator post_order_node_begin() const { return const_post_order_node_iterator(this, true);}
  const_post_order_node_iterator post_order_node_end() const { return const_post_order_node_iterator(this, false);}
  level_order_node_iterator level_order_node_begin() { return level_order_node_iterator(this, true);}
  level_order_node_iterator level_order_node_end() { return level_order_node_iterator(this, false);}
  const_level_order_node_iterator level_order_node_begin() const { return const_level_order_node_iterator(this, true);}
  const_level_order_node_iterator level_order_node_end() const { return const_level_order_node_iterator(this, false);}


  // public interface
  typename basic_tree_type::size_type capacity() const { return basic_tree_type::children.capacity();}
  void reserve(typename basic_tree_type::size_type sz) { basic_tree_type::children.reserve(sz);}
  tree_type& front() { return *basic_tree_type::children.front();}
  tree_type& back() { return *basic_tree_type::children.back();}
  const tree_type& front() const { return *basic_tree_type::children.front();}
  const tree_type& back() const { return *basic_tree_type::children.back();}
  void push_back(const stored_type& value);
  void pop_back() { iterator it = end(); erase(--it);}

  iterator insert(const stored_type& value);
  iterator insert(const tree_type& tree_obj );
  iterator insert(const_iterator pos, const stored_type& value);
  iterator insert(const const_iterator& pos, const tree_type& tree_obj);
  void insert(const_iterator pos, typename basic_tree_type::size_type num, const stored_type& value);
#if !defined(_MSC_VER) || _MSC_VER >= 1300 // insert range not available for VC6
  template<typename iterator_type> void insert(const_iterator pos, iterator_type it_beg, iterator_type it_end) 
  { while (it_beg != it_end) { 
      pos = insert(pos, *it_beg++); ++pos;
    }}
#endif
  void set(const stored_type& value) { basic_tree_type::set(value);}
  void set(const tree_type& tree_obj);
  void swap(tree_type& rhs);
  iterator erase(iterator it);
  iterator erase(iterator beg_it, iterator end_it);
  void clear();

  // subscript operators
  tree_type& operator [](basic_size_type index);
  const tree_type& operator [](basic_size_type index) const; 

  // children sort operations
  template<typename T> void sort(const T& comparer) { std::sort(basic_tree_type::children.begin(), basic_tree_type::children.end(), sort_functor_deref<T>(comparer));}
  void sort() { std::sort(basic_tree_type::children.begin(), basic_tree_type::children.end(), sort_deref());}

  // descendant sort operations
  template<typename T> void sort_descendants(const T& comparer)
  {
    post_order_iterator it = post_order_begin(), it_end = post_order_end();
    for (; it != it_end; ++it)
    {
      it.node()->sort(comparer);
    }
  }
  void sort_descendants(); 

  // overloaded iterator arithmetic operators
  friend const_iterator operator +(const const_iterator& lhs, typename basic_tree_type::size_type n) 
  { const_iterator temp(lhs); temp += n; return temp;}

  friend const_iterator operator +(typename basic_tree_type::size_type n, const const_iterator& rhs)
  { const_iterator temp(rhs); temp += n; return temp;}

  friend const_iterator operator -(const const_iterator& lhs, typename basic_tree_type::size_type n)
  { const_iterator temp(lhs); temp -= n; return temp;}

  friend iterator operator +(const iterator& lhs, typename basic_tree_type::size_type n)
  { iterator temp(lhs); temp += n; return temp;}

  friend iterator operator +(typename basic_tree_type::size_type n, const iterator& rhs)
  { iterator temp(rhs); temp += n; return temp;}

  friend iterator operator -(const iterator& lhs, typename basic_tree_type::size_type n)
  { iterator temp(lhs); temp -= n; return temp;}

  // overloaded node iterator arithmetic operators
  friend const_node_iterator operator +(const const_node_iterator& lhs, typename basic_tree_type::size_type n) 
  { const_node_iterator temp(lhs); temp += n; return temp;}

  friend const_node_iterator operator +(typename basic_tree_type::size_type n, const const_node_iterator& rhs)
  { const_node_iterator temp(rhs); temp += n; return temp;}

  friend const_node_iterator operator -(const const_node_iterator& lhs, typename basic_tree_type::size_type n)
  { const_node_iterator temp(lhs); temp -= n; return temp;}

  friend node_iterator operator +(const node_iterator& lhs, typename basic_tree_type::size_type n)
  { node_iterator temp(lhs); temp += n; return temp;}

  friend node_iterator operator +(typename basic_tree_type::size_type n, const node_iterator& rhs)
  { node_iterator temp(rhs); temp += n; return temp;}

  friend node_iterator operator -(const node_iterator& lhs, typename basic_tree_type::size_type n)
  { node_iterator temp(lhs); temp -= n; return temp;}


private:
  // sort() dereference functor
  struct sort_deref
  {
    bool operator() (const tree_type* lhs, const tree_type* rhs)
    {
      return *lhs->get() < *rhs->get();
    }
  };

  // sort<T>() dereference functor
  template<typename T>
  struct sort_functor_deref 
  {
    explicit sort_functor_deref(const T& sort_functor_) : sort_functor(sort_functor_) {}
    bool operator() (const tree_type* lhs, const tree_type* rhs) const
    {
      return sort_functor(*lhs->get(), *rhs->get());
    }
    sort_functor_deref& operator = (const sort_functor_deref& rhs) { sort_functor = rhs->sort_functor; return *this;}
    const T& sort_functor;
  };

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend typename pre_order_iterator;
    friend typename const_pre_order_iterator;
    friend typename post_order_iterator;
    friend typename const_post_order_iterator;
    friend typename pre_order_node_iterator;
    friend typename const_pre_order_node_iterator;
    friend typename post_order_node_iterator;
    friend typename const_post_order_node_iterator;
    friend class pre_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, tree_type, stored_type*, stored_type&>;
    friend class post_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, tree_type, stored_type*, tree_type&>;
    friend class level_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, tree_type, stored_type*, stored_type&>;
    friend class pre_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, tree_type*, tree_type&>;
    friend class post_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, tree_type*, tree_type&>;
    friend class level_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, tree_type*, tree_type&>;

    friend class pre_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, tree_type, const stored_type*, const stored_type&>;
    friend class post_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, tree_type, const stored_type*, const stored_type&>;
    friend class level_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, tree_type, const stored_type*, const stored_type&>;
    friend class pre_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, const tree_type*, const tree_type&>;
    friend class post_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, const tree_type*, const tree_type&>;
    friend class level_order_descendant_node_iterator<stored_type, tree_type, container_type, tree_type, const tree_type*, const tree_type&>;

    friend bool operator == (const tree_type& lhs, const tree_type& rhs);
    friend bool operator <  (const tree_type& lhs, const tree_type& rhs);
  #else
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class pre_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class post_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class level_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class pre_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class post_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class level_order_descendant_node_iterator;
    friend bool operator ==<> (const tree_type& lhs, const tree_type& rhs);
    friend bool operator < <> (const tree_type& lhs, const tree_type& rhs);
  #endif
};




#include "sequential_tree.inl"
