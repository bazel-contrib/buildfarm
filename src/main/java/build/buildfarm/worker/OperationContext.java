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

package build.buildfarm.worker;

import com.google.devtools.remoteexecution.v1test.Digest;
import com.google.devtools.remoteexecution.v1test.Action;
import com.google.devtools.remoteexecution.v1test.ExecuteOperationMetadata;
import com.google.longrunning.Operation;
import java.nio.file.Path;
import java.util.List;

final class OperationContext {
  final Operation operation;
  final Path execDir;
  final ExecuteOperationMetadata metadata;
  final Action action;
  final List<String> inputFiles;

  public OperationContext(
      Operation operation,
      Path execDir,
      ExecuteOperationMetadata metadata,
      Action action,
      List<String> inputFiles) {
    this.operation = operation;
    this.execDir = execDir;
    this.metadata = metadata;
    this.action = action;
    this.inputFiles = inputFiles;
  }
}
