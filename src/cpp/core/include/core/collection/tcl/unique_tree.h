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
#include "ordered_iterator.h"
#include <set>

namespace tcl
{
  // deref less for ordered children set
  template<typename tree_type, typename node_order_compare_type>
  struct deref_ordered_compare
  {
    bool operator() (const tree_type* lhs, const tree_type* rhs) const { return node_order_compare_type() (*lhs->get(), *rhs->get());}
  };

  // forward declaration for deref comparison functor
  template<typename stored_type, typename node_compare_type, typename node_order_compare_type >
  class unique_tree;

  // deref comparison functor, derive from binary function per Scott Meyer
  template<typename stored_type, typename node_compare_type, typename node_order_compare_type >
  struct unique_tree_deref_less : public std::binary_function<const unique_tree<stored_type, node_compare_type, node_order_compare_type>*, const unique_tree<stored_type, node_compare_type, node_order_compare_type>*, bool>
  {
    bool operator () (const unique_tree<stored_type, node_compare_type, node_order_compare_type>* lhs, const unique_tree<stored_type, node_compare_type, node_order_compare_type>* rhs) const
    {
      // call < on actual object
      return node_compare_type()(*lhs->get(), *rhs->get());
    }
  };
}




// instanciates base_tree_type with type of container (set of unique_tree ptrs) to use for node and key comparisons
template<typename stored_type, typename node_compare_type = std::less<stored_type>, typename node_order_compare_type = node_compare_type >
class tcl::unique_tree : public tcl::associative_tree<stored_type, tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>,  std::set<tcl::unique_tree<stored_type, node_compare_type, node_order_compare_type>*, tcl::unique_tree_deref_less<stored_type, node_compare_type, node_order_compare_type> > >
{
public:
  // typedefs
  typedef unique_tree<stored_type, node_compare_type, node_order_compare_type> tree_type;
  typedef unique_tree_deref_less<stored_type, node_compare_type, node_order_compare_type> key_compare;
  typedef unique_tree_deref_less<stored_type, node_compare_type, node_order_compare_type> value_compare;
  typedef std::set<tree_type*, key_compare> container_type;
  typedef basic_tree<stored_type, tree_type, container_type> basic_tree_type;
  typedef associative_tree<stored_type, tree_type, container_type> associative_tree_type;
  typedef typename associative_tree_type::iterator associative_iterator_type;
  typedef typename associative_tree_type::const_iterator associative_const_iterator_type;

  typedef unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, const unique_tree*, const stored_type*, const stored_type&> const_ordered_iterator;
  typedef unique_tree_ordered_iterator<stored_type, node_compare_type, node_order_compare_type, unique_tree*, stored_type*, stored_type&> ordered_iterator;

  // needed for inl function definitions in VC8
  typedef typename associative_tree_type::iterator child_iterator;
  typedef typename associative_tree_type::const_iterator const_child_iterator;

  // constructors/destructor
  explicit unique_tree( const stored_type& value = stored_type() ) : associative_tree_type(value), pOrphans(0), allowing_orphans(false), destroy_root(false) {}
  unique_tree( const tree_type& rhs ); // copy constructor
  template<typename iterator_type> unique_tree(iterator_type it_beg, iterator_type it_end, const stored_type& value = stored_type()) : associative_tree_type(value), pOrphans(0), allowing_orphans(false), destroy_root(false) { while (it_beg != it_end) { 
      insert(*it_beg); ++it_beg;
    }}
  ~unique_tree() { clear(); if (pOrphans) basic_tree_type::deallocate_tree_type(pOrphans);}

  // public interface
public:
  tree_type& operator = (const tree_type& rhs);  // assignment operator
  typename associative_tree_type::iterator insert(const stored_type& value);
  typename associative_tree_type::iterator insert(const typename associative_tree_type::const_iterator pos, const stored_type& value) { return associative_tree_type::insert(pos, value, this);}
  typename associative_tree_type::iterator insert(const tree_type& tree_obj );
  typename associative_tree_type::iterator insert(const typename associative_tree_type::const_iterator pos, const tree_type& tree_obj) { return associative_tree_type::insert(pos, tree_obj, this);}
  void swap(tree_type& rhs);

  typename associative_tree_type::iterator insert( const stored_type& parent_obj, const stored_type& value);
#if !defined(_MSC_VER) || _MSC_VER >= 1300 // insert range not available for VC6
  template<typename iterator_type> void insert(iterator_type it_beg, iterator_type it_end) { while (it_beg != it_end) insert(*it_beg++);}
#endif
  typename associative_tree_type::iterator find_deep(const stored_type& value);
  typename associative_tree_type::const_iterator find_deep(const stored_type& value) const;

  const_ordered_iterator ordered_begin() const { return const_ordered_iterator(ordered_children.begin());}
  const_ordered_iterator ordered_end() const { return const_ordered_iterator(ordered_children.end());}
  ordered_iterator ordered_begin() { return ordered_iterator(ordered_children.begin());}
  ordered_iterator ordered_end() { return ordered_iterator(ordered_children.end());}
  ordered_iterator find_ordered(const stored_type& value);
  const_ordered_iterator find_ordered(const stored_type& value) const;
  bool erase(const stored_type& value);
  void erase(associative_iterator_type it);
  void erase(associative_iterator_type it_beg, associative_iterator_type it_end);
  void clear();
  bool allow_orphans() const { return get_root()->allowing_orphans;}
  void allow_orphans(const bool allow) const { get_root()->allowing_orphans = allow;}
  const tree_type* get_orphans() const { return get_root()->pOrphans;}
  bool is_orphan() const { const tree_type* const root = get_root(); return(!root->empty() && root->ordered_children.empty());}
  void reindex_children();
  void reindex_descendants();

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

private:
  void set(const stored_type& value) { basic_tree_type::set(value);}
  void set(const tree_type& tree_obj);
  void inform_grandparents( tree_type* pNew_child, tree_type* pParent );
  bool check_for_duplicate(const stored_type& value, const tree_type* pParent) const;
  const tree_type* get_root() const;

private:
  // data
  mutable std::set<tree_type*, key_compare > descendents;
  std::multiset<tree_type*, deref_ordered_compare<tree_type, node_order_compare_type> > ordered_children;
  mutable tree_type* pOrphans;
  mutable bool allowing_orphans;
  mutable bool destroy_root;

  // friends
  #if defined(_MSC_VER) && _MSC_VER < 1300
    friend class basic_tree<stored_type, tree_type, container_type>;
  #else
    template<typename T, typename U, typename V> friend class basic_tree;
  #endif
};

#include "unique_tree.inl"


