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
import build.buildfarm.instance.Instance;
import build.bazel.remote.execution.v2.ActionCacheGrpc;
import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.GetActionResultRequest;
import build.bazel.remote.execution.v2.UpdateActionResultRequest;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

public class ActionCacheService extends ActionCacheGrpc.ActionCacheImplBase {
  private final Instances instances;

  public ActionCacheService(Instances instances) {
    this.instances = instances;
  }

  @Override
  public void getActionResult(
      GetActionResultRequest request,
      StreamObserver<ActionResult> responseObserver) {
    Instance instance;
    try {
      instance = instances.get(request.getInstanceName());
    } catch (InstanceNotFoundException ex) {
      responseObserver.onError(BuildFarmInstances.toStatusException(ex));
      return;
    }

    ActionResult actionResult = instance.getActionResult(
        DigestUtil.asActionKey(request.getActionDigest()));
    if (actionResult == null) {
      responseObserver.onError(new StatusException(Status.NOT_FOUND));
      return;
    }

    responseObserver.onNext(actionResult);
    responseObserver.onCompleted();
  }

  @Override
  public void updateActionResult(
      UpdateActionResultRequest request,
      StreamObserver<ActionResult> responseObserver) {
    Instance instance;
    try {
      instance = instances.get(request.getInstanceName());
    } catch (InstanceNotFoundException ex) {
      responseObserver.onError(BuildFarmInstances.toStatusException(ex));
      return;
    }

    ActionResult actionResult = request.getActionResult();
    try {
      instance.putActionResult(
          DigestUtil.asActionKey(request.getActionDigest()),
          actionResult);

      responseObserver.onNext(actionResult);
      responseObserver.onCompleted();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
