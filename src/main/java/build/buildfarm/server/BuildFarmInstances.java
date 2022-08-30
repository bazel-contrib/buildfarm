// Copyright 2017 The Bazel Authors. All rights reserved.
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

package build.buildfarm.server;

import build.buildfarm.common.DigestUtil;
import build.buildfarm.common.DigestUtil.HashFunction;
import build.buildfarm.common.config.yml.BuildfarmConfigs;
import build.buildfarm.instance.Instance;
import build.buildfarm.instance.memory.MemoryInstance;
import build.buildfarm.instance.shard.ShardInstance;
import javax.naming.ConfigurationException;

public class BuildFarmInstances {
  private static BuildfarmConfigs configs = BuildfarmConfigs.getInstance();
  public static Instance createInstance(
      String session, Runnable onStop)
          throws InterruptedException, ConfigurationException {
    String name = configs.getServer().getName();
    HashFunction hashFunction = getValidHashFunction();
    DigestUtil digestUtil = new DigestUtil(hashFunction);
    Instance instance;
    switch (configs.getServer().getInstanceType()) {
      default: throw new IllegalArgumentException("Instance type not set in config");
      case "MEMORY":
        instance = new MemoryInstance(name, digestUtil);
        break;
      case "SHARD":
        instance =
            new ShardInstance(
                name,
                session + "-" + name,
                digestUtil,
                onStop);
        break;
    }
    return instance;
  }

  private static HashFunction getValidHashFunction() {
    return HashFunction.valueOf(configs.getDigestFunction());
  }
}
