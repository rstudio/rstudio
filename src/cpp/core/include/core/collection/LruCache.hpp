/*
 * LruCache.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_COLLECTION_LRU_CACHE_HPP
#define CORE_COLLECTION_LRU_CACHE_HPP

#include <map>
#include <deque>
#include <vector>

#include <boost/multi_index_container.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index/sequenced_index.hpp>
#include <boost/multi_index/member.hpp>

#include <shared_core/Error.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {
namespace collection {

template <typename KeyType, typename ValueType>
class LruCache
{
public:
   LruCache(unsigned int maxSize) : maxSize_(maxSize) {}
   virtual ~LruCache() {}

   void insert(const KeyType& key,
               const ValueType& value)
   {
      LOCK_MUTEX(mutex_)
      {
         if (map_.count(key) > 0)
         {
            // entry for this key already exists - we are updating the value instead of inserting it
            // we need to remove it from the chain of nodes and then reinsert it to the front
            // so that this entry's LRU "time" is effectively updated
            auto pNode = map_[key];
            pNode->value = value;
            removeNode(pNode);
            addNode(pNode);
         }
         else
         {
            if (map_.size() >= maxSize_)
            {
               // the cache has reached maximum size
               // remove the oldest item from the cache which is at the back of the node chain
               auto pNode = backNode_;
               removeNode(pNode);

               // erase the node from the map
               // when this block goes out of scope, there are no more references
               // to the expired node pointer, so it is freed
               map_.erase(pNode->key);
            }

            // create a new node and store it
            auto pNode = boost::shared_ptr<Node>(new Node(key, value));
            addNode(pNode);
            map_[key] = pNode;
         }
      }
      END_LOCK_MUTEX
   }

   bool get(const KeyType& key,
            ValueType* pValue)
   {
      LOCK_MUTEX(mutex_)
      {
         typename CollectionType::iterator iter = map_.find(key);
         if (iter == map_.end())
            return false;

         auto pNode = iter->second;

         // remove and reinsert node to update its last access time
         removeNode(pNode);
         addNode(pNode);

         *pValue = pNode->value;
         return true;
      }
      END_LOCK_MUTEX

      return false;
   }

   void remove(const KeyType& key)
   {
      LOCK_MUTEX(mutex_)
      {
         typename CollectionType::iterator iter = map_.find(key);
         if (iter == map_.end())
            return;

         auto pNode = iter->second;
         removeNode(pNode);
         map_.erase(key);
      }
      END_LOCK_MUTEX
   }

   size_t size()
   {
      LOCK_MUTEX(mutex_)
      {
         return map_.size();
      }
      END_LOCK_MUTEX

      return 0;
   }

private:
   struct Node
   {
      Node(const KeyType& key, const ValueType& value) :
         key(key), value(value) {}

      boost::shared_ptr<Node> pLeft;
      boost::shared_ptr<Node> pRight;
      KeyType key;
      ValueType value;
   };

   void removeNode(const boost::shared_ptr<Node>& pNode)
   {
      if (pNode->pLeft)
         pNode->pLeft->pRight = pNode->pRight;
      else
         frontNode_ = pNode->pRight;

      if (pNode->pRight)
         pNode->pRight->pLeft = pNode->pLeft;
      else
         backNode_ = pNode->pLeft;
   }

   void addNode(const boost::shared_ptr<Node>& pNode)
   {
      pNode->pRight = frontNode_;
      pNode->pLeft.reset();

      if (frontNode_)
         frontNode_->pLeft = pNode;
      frontNode_ = pNode;

      if (!backNode_)
         backNode_ = frontNode_;
   }

   unsigned int maxSize_;

   typedef std::map<KeyType, boost::shared_ptr<Node>> CollectionType;
   CollectionType map_;

   boost::shared_ptr<Node> frontNode_;
   boost::shared_ptr<Node> backNode_;

   boost::mutex mutex_;
};

template <typename KeyType, typename ValueType>
class MultiIndexLruCache
{
public:
   MultiIndexLruCache(unsigned int maxSize) : maxSize_(maxSize) {}
   virtual ~MultiIndexLruCache() {}

   void insert(const KeyType& key,
               const ValueType& value)
   {
      LOCK_MUTEX(mutex_)
      {
         auto& keyIndex = cache_.template get<ByKey>();
         auto it = keyIndex.find(key);
         if (it != keyIndex.end())
         {
            // entry for this key already exists - we are updating the value instead of inserting it
            // we need to move it to the front so that this entry's LRU "time" is effectively updated
            keyIndex.modify(it, [&value](Entry& e){ e.value = value; });
            
            auto& seqIndex = cache_.template get<BySeq>();
            seqIndex.relocate(seqIndex.begin(), cache_.template project<BySeq>(it));
         }
         else
         {
            if (cache_.size() >= maxSize_)
            {
               // the cache has reached maximum size
               // remove the oldest item from the cache which is at the back of the node chain
               auto& seqIndex = cache_.template get<BySeq>();
               seqIndex.pop_back();
            }

            // create a new node and store it
            auto& seqIndex = cache_.template get<BySeq>();
            seqIndex.push_front(Entry(key, value));
         }
      }
      END_LOCK_MUTEX
   }

   bool get(const KeyType& key,
            ValueType* pValue)
   {
      LOCK_MUTEX(mutex_)
      {
         auto& keyIndex = cache_.template get<ByKey>();
         auto it = keyIndex.find(key);
         if (it == keyIndex.end())
            return false;

         // remove and reinsert node to update its last access time
         auto& seqIndex = cache_.template get<BySeq>();
         seqIndex.relocate(seqIndex.begin(), cache_.template project<BySeq>(it));

         *pValue = it->value;
         return true;
      }
      END_LOCK_MUTEX

      return false;
   }

   void getByValue(const ValueType& value, std::vector<KeyType>* pKeys)
   {
      LOCK_MUTEX(mutex_)
      {
         auto& valIndex = cache_.template get<ByValue>();
         auto range = valIndex.equal_range(value);
         for (auto it = range.first; it != range.second; ++it)
         {
            pKeys->push_back(it->key);
         }
      }
      END_LOCK_MUTEX
   }

   void remove(const KeyType& key)
   {
      LOCK_MUTEX(mutex_)
      {
         auto& keyIndex = cache_.template get<ByKey>();
         keyIndex.erase(key);
      }
      END_LOCK_MUTEX
   }

   void removeByValue(const ValueType& value)
   {
      LOCK_MUTEX(mutex_)
      {
         auto& valIndex = cache_.template get<ByValue>();
         valIndex.erase(value);
      }
      END_LOCK_MUTEX
   }

   void clear()
   {
      LOCK_MUTEX(mutex_)
      {
         cache_.clear();
      }
      END_LOCK_MUTEX
   }

   size_t size()
   {
      LOCK_MUTEX(mutex_)
      {
         return cache_.size();
      }
      END_LOCK_MUTEX

      return 0;
   }

private:
   struct Entry
   {
      Entry(const KeyType& key, const ValueType& value) :
         key(key), value(value) {}

      KeyType key;
      ValueType value;
   };

   struct BySeq {};
   struct ByKey {};
   struct ByValue {};

   typedef boost::multi_index::multi_index_container<
      Entry,
      boost::multi_index::indexed_by<
         boost::multi_index::sequenced<boost::multi_index::tag<BySeq>>,
         boost::multi_index::ordered_unique<
            boost::multi_index::tag<ByKey>,
            boost::multi_index::member<Entry, KeyType, &Entry::key>
         >,
         boost::multi_index::ordered_non_unique<
            boost::multi_index::tag<ByValue>,
            boost::multi_index::member<Entry, ValueType, &Entry::value>
         >
      >
   > Cache;

   unsigned int maxSize_;
   Cache cache_;

   boost::mutex mutex_;
};

} // namespace collection
} // namespace core
} // namespace rstudio

#endif // CORE_COLLECTION_LRU_CACHE_HPP
