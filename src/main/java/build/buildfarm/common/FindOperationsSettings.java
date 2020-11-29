// Copyright 2020 The Bazel Authors. All rights reserved.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//    http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package build.buildfarm.common;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.List;
import build.buildfarm.common.redis.BalancedRedisQueue;
import redis.clients.jedis.JedisCluster;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.stream.Collectors;

///
/// @class   FindOperationsSettings
/// @brief   Settings used to find operations.
/// @details These are used to discover the state of operations given a
///          particular context (such as who spawned the operation).
///
public class FindOperationsSettings {

    ///
    /// @field   user
    /// @brief   The name of the user who spawned the operation.
    /// @details This correlates to the env variable in the operation.
    ///
    public  String user;

    ///
    /// @field   scanAmount
    /// @brief   The number of redis entries to scan at a time.
    /// @details Larger amounts will be faster but require more memory.
    ///
    public  int scanAmount;

    ///
    /// @field   operationQuery
    /// @brief   How to query all of the operation entries in redis.
    /// @details The operation key is a global buildfarm config.
    ///
    public  String operationQuery;



}


