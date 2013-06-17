/*
 * Metric.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef MONITOR_METRIC_METRIC_HPP
#define MONITOR_METRIC_METRIC_HPP

#include <string>

#include <boost/function.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/json/Json.hpp>

namespace core {
   class Error;
}

namespace monitor {
namespace metrics {

enum MetricType
{
   GaugeMetric = 0,
   TimerMetric = 1,
   CounterMetric = 2
};

class Metric
{
public:
   Metric() {}

   Metric(const std::string& scope,
          const std::string& name,
          double value,
          MetricType type = GaugeMetric,
          const std::string& unit = std::string(),
          boost::posix_time::ptime timestamp =
                     boost::posix_time::microsec_clock::universal_time())
      : scope_(scope),
        name_(name),
        value_(value),
        type_(type),
        unit_(unit),
        timestamp_(timestamp)
   {
   }

public:
   // check for empty
   bool isEmpty() const { return !name_.empty(); }

   // property accessors
   const std::string& scope() const { return scope_; }
   const std::string& name() const { return name_; }
   double value() const { return value_; }
   MetricType type() const { return type_; }
   const std::string& unit() const { return unit_; }
   const boost::posix_time::ptime& timestamp() const { return timestamp_; }

private:
   std::string scope_;
   std::string name_;
   double value_;
   MetricType type_;
   std::string unit_;
   boost::posix_time::ptime timestamp_;
};

// metric handler
typedef boost::function<void(const Metric&)> MetricHandler;

// json serialization
std::string metricToJson(const Metric& metric);
core::Error metricFromJson(const std::string& metricJson, Metric* pMetric);


} // namespace metrics
} // namespace monitor

#endif // MONITOR_METRIC_METRIC_HPP

