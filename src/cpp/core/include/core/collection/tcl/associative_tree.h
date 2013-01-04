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
#include <memory>

namespace tcl 
{
  template<typename T, typename U, typename X> class associative_tree;

  // overloaded comparison operations
  template<typename T, typename U, typename V> bool operator == (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs);
  template<typename T, typename U, typename V> bool operator <  (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs);
  template<typename T, typename U, typename V> bool operator != (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs) { return !(lhs == rhs);}
  template<typename T, typename U, typename V> bool operator >  (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs) { return rhs < lhs;}
  template<typename T, typename U, typename V> bool operator <= (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs) { return !(rhs < lhs);}
  template<typename T, typename U, typename V> bool operator >= (const associative_tree<T, U, V>& lhs, const associative_tree<T, U, V>& rhs) { return !(lhs < rhs);}
}

// stored_type:             type stored in container
// tree_type:               one of three tree types derived from this base
// container_type:      type of contain to hold children (can be set or multiset)

template< typename stored_type, typename tree_type,  typename container_type >
class tcl::associative_tree : public basic_tree<stored_type, tree_type, container_type>
{
protected:
  typedef basic_tree<stored_type, tree_type, container_type> basic_tree_type;
  explicit associative_tree( const stored_type& stored_obj ) : basic_tree_type(stored_obj) {}
  virtual ~associative_tree() {}

public:
  // typedefs
  typedef associative_tree<stored_type, tree_type, container_type> associative_tree_type;
  typedef stored_type key_type;
  using typename basic_tree_type::size_type;

  // element iterator typedefs
  typedef associative_iterator<stored_type, tree_type, const tree_type*, container_type, const stored_type*, const stored_type&>                            const_iterator;
  typedef associative_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>                                              iterator;
  typedef associative_reverse_iterator<stored_type, tree_type, const tree_type*, container_type, const stored_type*, const stored_type&>                    const_reverse_iterator;
  typedef associative_reverse_iterator<stored_type, tree_type, tree_type*, container_type, stored_type*, stored_type&>                                      reverse_iterator;
  typedef pre_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&>   const_pre_order_iterator;
  typedef pre_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                           pre_order_iterator;
  typedef post_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&>  const_post_order_iterator;
  typedef post_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                          post_order_iterator;
  typedef level_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, const_iterator, const stored_type*, const stored_type&> const_level_order_iterator;
  typedef level_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, iterator, stored_type*, stored_type&>                         level_order_iterator;

  // node iterator typedefs
  typedef associative_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>                                 const_node_iterator;
  typedef associative_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>                                             node_iterator;
  typedef associative_reverse_node_iterator<stored_type, tree_type, container_type, const tree_type*, const tree_type&>                         const_reverse_node_iterator;
  typedef associative_reverse_node_iterator<stored_type, tree_type, container_type, tree_type*, tree_type&>                                     reverse_node_iterator;
  typedef pre_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&>   const_pre_order_node_iterator;
  typedef pre_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                     pre_order_node_iterator;
  typedef post_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&>  const_post_order_node_iterator;
  typedef post_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                    post_order_node_iterator;
  typedef level_order_descendant_node_iterator<stored_type, tree_type, container_type, const_node_iterator, const tree_type*, const tree_type&> const_level_order_node_iterator;
  typedef level_order_descendant_node_iterator<stored_type, tree_type, container_type, node_iterator, tree_type*, tree_type&>                   level_order_node_iterator;

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

  // child node reverse iterator accessors
  const_reverse_node_iterator node_rbegin() const {return const_reverse_node_iterator(node_end());}
  const_reverse_node_iterator node_rend() const {return const_reverse_node_iterator(node_begin());}
  reverse_node_iterator node_rbegin() {return reverse_node_iterator(node_end());}
  reverse_node_iterator node_rend() { return reverse_node_iterator(node_begin());}

  // public interface
  iterator find(const stored_type& value);
  const_iterator find(const stored_type& value) const;
  bool erase(const stored_type& value);
  void erase(iterator it);
  void erase(iterator it_beg, iterator it_end);
  void clear();
  typename basic_tree_type::size_type count(const stored_type& value) const;
  iterator lower_bound(const stored_type& value);
  const_iterator lower_bound(const stored_type& value) const;
  iterator upper_bound(const stored_type& value);
  const_iterator upper_bound(const stored_type& value) const;
  std::pair<iterator, iterator> equal_range(const stored_type& value)
  {
    tree_type node_obj(value); // create a search node and search local children
    iterator lower_it(basic_tree_type::children.lower_bound(&node_obj), this);
    iterator upper_it(basic_tree_type::children.upper_bound(&node_obj), this);
    return std::make_pair(lower_it, upper_it);
  }
  std::pair<const_iterator, const_iterator> equal_range(const stored_type& value) const
  {
    tree_type node_obj(value); // create a search node and search local children
    const_iterator lower_it(basic_tree_type::children.lower_bound(&node_obj), this);
    const_iterator upper_it(basic_tree_type::children.upper_bound(&node_obj), this);
    return std::make_pair(lower_it, upper_it);
  }

protected:
  iterator insert( const stored_type& value, tree_type* parent ) { return insert(end(), value, parent);}
  iterator insert(const const_iterator& pos, const stored_type& value, tree_type* parent);
  iterator insert(const tree_type& tree_obj, tree_type* parent) { return insert(end(), tree_obj, parent);}
  iterator insert(const const_iterator pos, const tree_type& tree_obj, tree_type* parent);
  void set(const tree_type& tree_obj, tree_type* parent);

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend typename pre_order_iterator;
    friend typename const_pre_order_iterator;
    friend typename post_order_iterator;
    friend typename const post_order_iterator;
    friend typename pre_order_node_iterator;
    friend typename const_pre_order_node_iterator;
    friend typename post_order_node_iterator;
    friend typename const_post_order_node_iterator;

    friend class pre_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, associative_tree_type, stored_type*, stored_type&>;
    friend class post_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, associative_tree_type, stored_type*, stored_type&>;
    friend class level_order_descendant_iterator<stored_type, tree_type, tree_type*, container_type, associative_tree_type, stored_type*, stored_type&>;
    friend class pre_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, stored_type*, stored_type&>;
    friend class post_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, stored_type*, stored_type&>;
    friend class level_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, stored_type*, stored_type&>;

    friend class pre_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, associative_tree_type, const stored_type*, const stored_type&>;
    friend class post_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, associative_tree_type, const stored_type*, const stored_type&>;
    friend class level_order_descendant_iterator<stored_type, tree_type, const tree_type*, container_type, associative_tree_type, const stored_type*, const stored_type&>;
    friend class pre_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, const stored_type*, const stored_type&>;
    friend class post_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, const stored_type*, const stored_type&>;
    friend class level_order_descendant_node_iterator<stored_type, tree_type, container_type, associative_tree_type, const stored_type*, const stored_type&>;

    friend bool operator == (const associative_tree_type& lhs, const associative_tree_type& rhs);
    friend bool operator < (const associative_tree_type& lhs, const associative_tree_type& rhs);
  #else 
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class pre_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class post_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y, typename Z> friend class level_order_descendant_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class pre_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class post_order_descendant_node_iterator;
    template<typename T, typename U, typename V, typename W, typename X, typename Y> friend class level_order_descendant_node_iterator;
    friend bool operator ==<> (const associative_tree_type& lhs, const associative_tree_type& rhs);
    friend bool operator < <> (const associative_tree_type& lhs, const associative_tree_type& rhs);
  #endif
};

#include "associative_tree.inl"
