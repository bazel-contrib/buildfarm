// Copyright 2018 The Bazel Authors. All rights reserved.
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

package build.buildfarm.worker;

import com.google.longrunning.Operation;

public class PutOperationStage extends PipelineStage.NullStage {
  private final InterruptingConsumer<Operation> onPut;

  @FunctionalInterface
  public interface InterruptingConsumer<T> {
    void accept(T t) throws InterruptedException;
  }

  public PutOperationStage(InterruptingConsumer<Operation> onPut) {
    this.onPut = onPut;
  }

  @Override
  public void put(OperationContext operationContext) throws InterruptedException {
    onPut.accept(operationContext.operation);
  }
}
