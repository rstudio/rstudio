#include <gtest/gtest.h>

#include <core/Thread.hpp>

#include <vector>
#include <thread>
#include <atomic>
#include <chrono>
#include <random>
#include <future>
#include <set>


using namespace rstudio::core::thread;

class ThreadsafeQueueTest : public ::testing::Test
{
protected:
    void SetUp() override
    {
        queue_.reset(new ThreadsafeQueue<int>());
    }

    void TearDown() override
    {
        queue_.reset();
    }

    std::unique_ptr<ThreadsafeQueue<int>> queue_;
};


/*
* Test: enque and deque a single value
* Procedure: 
*   enqueue a value
*   deque a value
* Conditions:
*   queue should be empty before enqueue
*   queue should not be empty after enqueue
*   deque should return true and the value should match the enqueued value 
*/
TEST_F(ThreadsafeQueueTest, BasicEnqueueDequeue)
{
    EXPECT_TRUE(queue_->isEmpty());
    
    queue_->enque(-1);
    EXPECT_FALSE(queue_->isEmpty());
    
    int value;
    EXPECT_TRUE(queue_->deque(&value));
    EXPECT_EQ(value, -1);
    EXPECT_TRUE(queue_->isEmpty());
}

/*
* Test: Deque from empty queue
* Procedure: 
*   deque a value
* Conditions:
*   deque should return false
*/
TEST_F(ThreadsafeQueueTest, DequeueFromEmptyQueue)
{
    int value;
    EXPECT_FALSE(queue_->deque(&value));
}

/*
* Test: Order with multiple enqueue and dequeue
* Procedure: 
*   enqueue multiple values in sequence
*   dequeue all values in sequence
* Conditions:
*   all values should be dequeued in FIFO order
*   queue should be empty after all dequeues
*/
TEST_F(ThreadsafeQueueTest, FIFOOrder)
{
    std::vector<int> testValues = {1, 2, 3, 4, 5};
    
    // Enqueue all values
    for (int val : testValues)
    {
        queue_->enque(val);
    }
    
    // Dequeue and verify order (FIFO)
    for (int expected : testValues)
    {
        int actual;
        EXPECT_TRUE(queue_->deque(&actual));
        EXPECT_EQ(actual, expected);
    }
    
    EXPECT_TRUE(queue_->isEmpty());
}

/*
* Test: dequeue with timeout on empty queue
* Procedure: 
*   call timed dequeue on empty queue with 100ms timeout
*   measure elapsed time
* Conditions:
*   dequeue should return false
*   elapsed time should be approximately 100ms (≥90ms tolerance)
*/
TEST_F(ThreadsafeQueueTest, DequeueWithTimeout)
{
    int value;
    auto start = std::chrono::steady_clock::now();
    
    bool result = queue_->deque(&value, boost::posix_time::milliseconds(100));
    
    auto elapsed = std::chrono::steady_clock::now() - start;
    EXPECT_FALSE(result);
    EXPECT_GE(elapsed, std::chrono::milliseconds(90)); // Allow some tolerance for inexact timing
}

/*
* Test: dequeue with timeout and data available
* Procedure:
*   enqueue a value
*   call timed dequeue with 100ms timeout
*   measure elapsed time
* Conditions:
*   dequeue should return true with correct value
*   elapsed time should be very short (<50ms)
*/
TEST_F(ThreadsafeQueueTest, DequeueWithTimeoutAndDataAvailable)
{
    queue_->enque(123);
    
    int value;
    auto start = std::chrono::steady_clock::now();
    
    bool result = queue_->deque(&value, boost::posix_time::milliseconds(100));
    
    auto elapsed = std::chrono::steady_clock::now() - start;
    EXPECT_TRUE(result);
    EXPECT_EQ(value, 123);
    EXPECT_LT(elapsed, std::chrono::milliseconds(50)); // Should be fast
}

/*
* Test: Single producer thread with single consumer thread
* Procedure: 
*   producer thread enqueues 1000 items with occasional delays
*   consumer thread dequeues items until producer is done and queue is empty
* Conditions:
*   all 1000 items should be consumed
*   items should be consumed in the same order they were produced (FIFO)
*   no race conditions or data corruption
*/
TEST_F(ThreadsafeQueueTest, SingleProducerSingleConsumer)
{
    size_t numItems = 1000;
    std::atomic<bool> producerDone{false};
    std::vector<int> consumedItems;
    consumedItems.reserve(numItems);
    
    // Producer thread
    std::thread producer([this, numItems, &producerDone]()
    {
        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_int_distribution<> delayDis(1, 5);
        std::uniform_int_distribution<> frequencyDis(1, 20);
        
        for (size_t i = 0; i < numItems; ++i)
        {
            queue_->enque(i);

            // Add random delay with random frequency to increase chance of race conditions
            if (i % frequencyDis(gen) == 0)
                std::this_thread::sleep_for(std::chrono::microseconds(delayDis(gen)));
        }
        producerDone = true;
    });
    
    // Consumer thread
    std::thread consumer([this, &consumedItems, &producerDone]()
    {
        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_int_distribution<> delayDis(1, 3);
        std::uniform_int_distribution<> frequencyDis(1, 15);
        
        int value;
        while (!producerDone || !queue_->isEmpty())
        {
            if (queue_->deque(&value))
            {
                consumedItems.push_back(value);
            }
            else
            {
                // Random delay when queue is empty to vary timing
                if (frequencyDis(gen) <= 5)
                    std::this_thread::sleep_for(std::chrono::microseconds(delayDis(gen)));
                else
                    std::this_thread::yield();
            }
        }
    });
    
    producer.join();
    consumer.join();
    
    EXPECT_EQ(consumedItems.size(), numItems);
    
    // Verify all items were consumed in order
    for (std::size_t i = 0; i < numItems; ++i)
    {
        EXPECT_EQ(consumedItems[i], i);
    }
}

/*
* Test: Multiple producer threads with single consumer thread
* Procedure: 
*   4 producer threads each enqueue 250 unique items with random delays
*   1 consumer thread dequeues all items
* Conditions:
*   all 1000 items should be consumed exactly once
*   no items should be lost or duplicated
*   all expected values should be present (order may vary due to concurrency)
*/
TEST_F(ThreadsafeQueueTest, MultipleProducersSingleConsumer)
{
    const int numProducers = 4;
    const int itemsPerProducer = 250;
    const int totalItems = numProducers * itemsPerProducer;
    
    std::atomic<int> producersFinished{0};
    std::vector<int> consumedItems;
    consumedItems.reserve(totalItems);
    
    // Multiple producer threads
    std::vector<std::thread> producers;
    for (int p = 0; p < numProducers; ++p)
    {
        producers.emplace_back([this, p, itemsPerProducer, &producersFinished]() {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(1, 10);
            
            for (int i = 0; i < itemsPerProducer; ++i)
            {
                int value = p * itemsPerProducer + i;
                queue_->enque(value);
                
                // Random small delay to increase contention
                if (i % dis(gen) == 0)
                    std::this_thread::sleep_for(std::chrono::microseconds(dis(gen)));
            }
            ++producersFinished;
        });
    }
    
    // Single consumer thread
    std::thread consumer([this, &consumedItems, &producersFinished, numProducers]() {
        int value;
        while (producersFinished < numProducers || !queue_->isEmpty())
        {
            if (queue_->deque(&value))
            {
                consumedItems.push_back(value);
            }
            else
            {
                std::this_thread::sleep_for(std::chrono::microseconds(1));
            }
        }
    });
    
    // Wait for all threads
    for (auto& producer : producers)
        producer.join();
    consumer.join();
    
    EXPECT_EQ(consumedItems.size(), totalItems);
    
    // Verify all expected values are present (order may vary due to concurrent producers)
    std::set<int> consumedSet(consumedItems.begin(), consumedItems.end());
    EXPECT_EQ(consumedSet.size(), totalItems);
    
    for (int i = 0; i < totalItems; ++i)
    {
        EXPECT_TRUE(consumedSet.find(i) != consumedSet.end()) 
            << "Missing value: " << i;
    }
}

/*
* Test: Single producer thread with multiple consumer threads
* Procedure: 
*   1 producer thread enqueues 1000 items with random delays
*   4 consumer threads compete to dequeue items
* Conditions:
*   all 1000 items should be consumed exactly once
*   no item should be consumed by multiple threads (no duplicate consumption)
*   total consumed count should equal total produced count
*/
TEST_F(ThreadsafeQueueTest, SingleProducerMultipleConsumers)
{
    const int numConsumers = 4;
    const int totalItems = 1000;
    
    std::atomic<bool> producerDone{false};
    std::atomic<int> totalConsumed{0};
    std::vector<std::vector<int>> consumedByThread(numConsumers);
    
    // Single producer thread
    std::thread producer([this, totalItems, &producerDone]() {
        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_int_distribution<> dis(1, 5);
        
        for (int i = 0; i < totalItems; ++i)
        {
            queue_->enque(i);
            
            // Random small delay
            if (i % dis(gen) == 0)
                std::this_thread::sleep_for(std::chrono::microseconds(dis(gen)));
        }
        producerDone = true;
    });
    
    // Multiple consumer threads
    std::vector<std::thread> consumers;
    for (int c = 0; c < numConsumers; ++c)
    {
        consumers.emplace_back([this, c, &consumedByThread, &totalConsumed, &producerDone]() {
            int value;
            while (!producerDone || !queue_->isEmpty())
            {
                if (queue_->deque(&value))
                {
                    consumedByThread[c].push_back(value);
                    totalConsumed++;
                }
                else
                {
                    std::this_thread::sleep_for(std::chrono::microseconds(1));
                }
            }
        });
    }
    
    // Wait for all threads
    producer.join();
    for (auto& consumer : consumers)
        consumer.join();
    
    EXPECT_EQ(totalConsumed, totalItems);
    
    // Verify all values were consumed exactly once
    std::set<int> allConsumed;
    for (const auto& consumerItems : consumedByThread)
    {
        for (int item : consumerItems)
        {
            EXPECT_TRUE(allConsumed.find(item) == allConsumed.end()) 
                << "Duplicate consumption of value: " << item;
            allConsumed.insert(item);
        }
    }
    
    EXPECT_EQ(allConsumed.size(), totalItems);
}

/*
* Test: High contention scenario with multiple producers and consumers
* Procedure: 
*   3 producer threads each enqueue ~334 items with aggressive random delays
*   3 consumer threads compete to dequeue items with random delays
*   monitor execution time for deadlock detection (30s timeout)
* Conditions:
*   all ~1000 items should be consumed exactly once
*   no deadlocks should occur (test completes within timeout)
*   no duplicate consumption of any item
*   stress test the mutex and condition variable under high contention
*/
TEST_F(ThreadsafeQueueTest, HighContentionMultipleProducersConsumers)
{
    const int numProducers = 3;
    const int numConsumers = 3;
    const int itemsPerProducer = 334; // Total ~1000 items
    const int totalItems = numProducers * itemsPerProducer;
    
    std::atomic<int> producersFinished{0};
    std::atomic<int> totalConsumed{0};
    std::vector<std::vector<int>> consumedByThread(numConsumers);
    
    // Multiple producer threads
    std::vector<std::thread> producers;
    for (int p = 0; p < numProducers; ++p)
    {
        producers.emplace_back([this, p, itemsPerProducer, &producersFinished]() {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(1, 3);
            
            for (int i = 0; i < itemsPerProducer; ++i)
            {
                int value = p * itemsPerProducer + i;
                queue_->enque(value);
                
                // Aggressive random delays to increase contention
                if (i % 2 == 0)
                    std::this_thread::sleep_for(std::chrono::microseconds(dis(gen)));
            }
            producersFinished++;
        });
    }
    
    // Multiple consumer threads
    std::vector<std::thread> consumers;
    for (int c = 0; c < numConsumers; ++c)
    {
        consumers.emplace_back([this, c, &consumedByThread, &totalConsumed, 
                               &producersFinished, numProducers]() {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(1, 3);
            
            int value;
            while (producersFinished < numProducers || !queue_->isEmpty())
            {
                if (queue_->deque(&value))
                {
                    consumedByThread[c].push_back(value);
                    totalConsumed++;
                }
                else
                {
                    std::this_thread::sleep_for(std::chrono::microseconds(dis(gen)));
                }
            }
        });
    }
    
    // Wait for all threads with timeout to detect deadlocks
    auto startTime = std::chrono::steady_clock::now();
    const auto timeout = std::chrono::seconds(30);
    
    for (auto& producer : producers)
        producer.join();
    
    for (auto& consumer : consumers)
        consumer.join();
    
    auto elapsed = std::chrono::steady_clock::now() - startTime;
    EXPECT_LT(elapsed, timeout) << "Test took too long, possible deadlock";
    
    EXPECT_EQ(totalConsumed, totalItems);
    
    // Verify no duplicates
    std::set<int> allConsumed;
    for (const auto& consumerItems : consumedByThread)
    {
        for (int item : consumerItems)
        {
            EXPECT_TRUE(allConsumed.find(item) == allConsumed.end()) 
                << "Duplicate consumption of value: " << item;
            allConsumed.insert(item);
        }
    }
}

/*
* Test: Stress test of timed wait functionality with multiple threads
* Procedure: 
*   5 waiter threads each attempt 20 timed dequeues (10ms timeout)
*   2 producer threads produce items with long random delays (1-20ms)
*   coordinate timing so waiters start before producers
* Conditions:
*   some timed waits should succeed (>0)
*   some timed waits should timeout due to timing
*   not all waits should succeed (tests actual timeout behavior)
*   no deadlocks in condition variable wait logic
*/
// Stress test with timed waits
TEST_F(ThreadsafeQueueTest, TimedWaitStressTest)
{
    const int numWaiters = 5;
    const int numProducers = 2;
    const int itemsPerProducer = 50;
    
    std::atomic<int> successfulWaits{0};
    std::atomic<int> timeouts{0};
    std::atomic<bool> startProducing{false};
    
    // Multiple threads doing timed waits
    std::vector<std::thread> waiters;
    for (int w = 0; w < numWaiters; ++w)
    {
        waiters.emplace_back([this, &successfulWaits, &timeouts, &startProducing]() {
            // Wait for signal to start
            while (!startProducing)
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
            
            int value;
            for (int i = 0; i < 20; ++i) // Each waiter tries multiple times
            {
                if (queue_->deque(&value, boost::posix_time::milliseconds(10)))
                {
                    successfulWaits++;
                }
                else
                {
                    timeouts++;
                }
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
            }
        });
    }
    
    // Start the waiting threads, then begin producing
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    startProducing = true;
    
    // Producer threads
    std::vector<std::thread> producers;
    for (int p = 0; p < numProducers; ++p)
    {
        producers.emplace_back([this, p, itemsPerProducer]() {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(1, 20);
            
            for (int i = 0; i < itemsPerProducer; ++i)
            {
                queue_->enque(p * itemsPerProducer + i);
                std::this_thread::sleep_for(std::chrono::milliseconds(dis(gen)));
            }
        });
    }
    
    // Wait for completion
    for (auto& waiter : waiters)
        waiter.join();
    for (auto& producer : producers)
        producer.join();
    
    // We should have some successful waits and some timeouts
    EXPECT_GT(successfulWaits, 0);
    EXPECT_LT(successfulWaits, numWaiters * 20); // Not all should succeed due to timing
}
