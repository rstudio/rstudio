/*
 * LruCache.hpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include <core/Error.hpp>
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
            // key already exists - we are updating the value instead of inserting it
            // we need to remove any existing entries in the queue for that key
            // so that this entry's LRU "time" is effectively updated
            removeKey(key);
         }
         else if (map_.size() >= maxSize_)
         {
            // the cache has reached maximum size
            // remove the oldest key from the cache which is at the front of the deque
            // new items are added to the back, meaning the front always contains the oldest items
            KeyType expiredKey = keyQueue_.front();
            keyQueue_.pop_front();

            map_.erase(expiredKey);
         }

         // add new key to the back
         keyQueue_.push_back(key);

         // store value
         map_[key] = value;
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

         *pValue = iter->second;

         // remove key from queue and add new entry to update its LRU "time"
         removeKey(key);
         keyQueue_.push_back(key);

         return true;
      }
      END_LOCK_MUTEX

      return false;
   }

   void remove(const KeyType& key)
   {
      LOCK_MUTEX(mutex_)
      {
         removeKey(key);
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

   void removeKey(const KeyType& key)
   {
      for (size_t i = 0; i < keyQueue_.size(); ++i)
      {
         if (keyQueue_.at(i) == key)
         {
            keyQueue_.erase(keyQueue_.begin() + i);
            return;
         }
      }
   }

   unsigned int maxSize_;

   typedef std::map<KeyType, ValueType> CollectionType;
   CollectionType map_;
   std::deque<KeyType> keyQueue_;

   boost::mutex mutex_;
};

} // namespace collection
} // namespace core
} // namespace rstudio

#endif // CORE_COLLECTION_LRU_CACHE_HPP
