/*
 * LruCache.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
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

} // namespace collection
} // namespace core
} // namespace rstudio

#endif // CORE_COLLECTION_LRU_CACHE_HPP
