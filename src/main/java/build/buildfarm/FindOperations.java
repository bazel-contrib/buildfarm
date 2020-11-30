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

package build.buildfarm;

import build.buildfarm.common.DigestUtil;
import build.buildfarm.instance.Instance;
import build.buildfarm.instance.stub.StubInstance;
import build.buildfarm.operations.FindOperationsResults;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

// This tool can be used to find Operations based on their particular properties.
// For example, it could find all of the operations executed by a particular user or particular
// program.
// ./tool <URL> shard SHA256 <user>
// The operations that match the query will be printed.
class FindOperations {

  private static ManagedChannel createChannel(String target) {
    NettyChannelBuilder builder =
        NettyChannelBuilder.forTarget(target).negotiationType(NegotiationType.PLAINTEXT);
    return builder.build();
  }

  public static void main(String[] args) throws Exception {
    String host = args[0];
    String instanceName = args[1];
    DigestUtil digestUtil = DigestUtil.forHash(args[2]);
    String filterPredicate = args[3];
    ManagedChannel channel = createChannel(host);
    Instance instance = new StubInstance(instanceName, digestUtil, channel);
    FindOperationsResults results = instance.findOperations(filterPredicate);
    System.out.println(results.toMessage());
    instance.stop();
  }
}
