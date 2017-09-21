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

package build.buildfarm.instance;

import build.buildfarm.common.Digests;
import build.buildfarm.common.ContentAddressableStorage;
import build.buildfarm.common.ContentAddressableStorage.Blob;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.devtools.remoteexecution.v1test.Action;
import com.google.devtools.remoteexecution.v1test.ActionResult;
import com.google.devtools.remoteexecution.v1test.Command;
import com.google.devtools.remoteexecution.v1test.Digest;
import com.google.devtools.remoteexecution.v1test.Directory;
import com.google.devtools.remoteexecution.v1test.DirectoryNode;
import com.google.devtools.remoteexecution.v1test.ExecuteOperationMetadata;
import com.google.devtools.remoteexecution.v1test.ExecutePreconditionViolationType;
import com.google.devtools.remoteexecution.v1test.ExecuteResponse;
import com.google.devtools.remoteexecution.v1test.FileNode;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public abstract class AbstractServerInstance implements Instance {
  private final String name;
  protected final ContentAddressableStorage contentAddressableStorage;
  protected final Map<Digest, ActionResult> actionCache;
  protected final Map<String, Operation> outstandingOperations;
  protected final Map<String, Operation> completedOperations;

  public AbstractServerInstance(
      String name,
      ContentAddressableStorage contentAddressableStorage,
      Map<Digest, ActionResult> actionCache,
      Map<String, Operation> outstandingOperations,
      Map<String, Operation> completedOperations) {
    this.name = name;
    this.contentAddressableStorage = contentAddressableStorage;
    this.actionCache = actionCache;
    this.outstandingOperations = outstandingOperations;
    this.completedOperations = completedOperations;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ActionResult getActionResult(Digest actionDigest) {
    return actionCache.get(actionDigest);
  }

  @Override
  public void putActionResult(Digest actionDigest, ActionResult actionResult) {
    actionCache.put(actionDigest, actionResult);
  }

  @Override
  public String getBlobName(Digest blobDigest) {
    return String.format(
        "%s/blobs/%s",
        getName(),
        Digests.toString(blobDigest));
  }

  @Override
  public ByteString getBlob(Digest blobDigest) {
    return getBlob(blobDigest, 0, 0);
  }

  @Override
  public ByteString getBlob(Digest blobDigest, long offset, long limit)
      throws IndexOutOfBoundsException {
    Blob blob = contentAddressableStorage.get(blobDigest);

    if (blob == null) {
      return null;
    }

    if (offset < 0
        || (blob.isEmpty() && offset > 0)
        || (!blob.isEmpty() && offset >= blob.size())
        || limit < 0) {
      throw new IndexOutOfBoundsException();
    }

    long endIndex = offset + (limit > 0 ? limit : (blob.size() - offset));

    return blob.getData().substring(
        (int) offset, (int) (endIndex > blob.size() ? blob.size() : endIndex));
  }

  @Override
  public Digest putBlob(ByteString content) throws IllegalArgumentException {
    Blob blob = new Blob(content);
    contentAddressableStorage.put(blob);
    return blob.getDigest();
  }

  protected abstract int getTreeDefaultPageSize();
  protected abstract int getTreeMaxPageSize();
  protected abstract TokenizableIterator<Directory> createTreeIterator(
      Digest rootDigest, String pageToken);

  @Override
  public String getTree(
      Digest rootDigest, int pageSize, String pageToken,
      ImmutableList.Builder<Directory> directories) {
    if (pageSize == 0) {
      pageSize = getTreeDefaultPageSize();
    }
    if (pageSize >= 0 && pageSize > getTreeMaxPageSize()) {
      pageSize = getTreeMaxPageSize();
    }

    TokenizableIterator<Directory> iter =
      createTreeIterator(rootDigest, pageToken);

    while (iter.hasNext() && pageSize != 0) {
      Directory directory = iter.next();
      // If part of the tree is missing from the CAS, the server will return the
      // portion present and omit the rest.
      if (directory != null) {
        directories.add(directory);
        if (pageSize > 0) {
          pageSize--;
        }
      }
    }
    return iter.toNextPageToken();
  }

  protected String createOperationName(String id) {
    return getName() + "/operations/" + id;
  }

  abstract protected Operation createOperation(Action action);

  // called when an operation will be queued for execution
  protected void onQueue(Operation operation, Action action) {
  }

  private void stringsUniqueAndSortedPrecondition(
      Iterable<String> strings,
      ExecutePreconditionViolationType duplicateViolationType,
      ExecutePreconditionViolationType unsortedViolationType) {
    String lastString = "";
    for (String string : strings) {
      int direction = lastString.compareTo(string);
      Preconditions.checkState(direction != 0, duplicateViolationType);
      Preconditions.checkState(direction < 0, string + " >= " + lastString, unsortedViolationType);
    }
  }

  private void filesUniqueAndSortedPrecondition(Iterable<String> files) {
    stringsUniqueAndSortedPrecondition(
        files,
        ExecutePreconditionViolationType.DUPLICATE_FILE_NODE,
        ExecutePreconditionViolationType.DIRECTORY_NOT_SORTED);
  }

  private void environmentVariablesUniqueAndSortedPrecondition(
      Iterable<Command.EnvironmentVariable> environmentVariables) {
    stringsUniqueAndSortedPrecondition(
        Iterables.transform(
            environmentVariables,
            environmentVariable -> environmentVariable.getName()),
        ExecutePreconditionViolationType.DUPLICATE_ENVIRONMENT_VARIABLE,
        ExecutePreconditionViolationType.ENVIRONMENT_VARIABLES_NOT_SORTED);
  }

  private void validateActionInputDirectory(
      Directory directory,
      Stack<Digest> path,
      Set<Digest> visited,
      ImmutableSet.Builder<Digest> inputDigests) {
    Preconditions.checkState(
        directory != null,
        ExecutePreconditionViolationType.MISSING_INPUT);

    Set<String> entryNames = new HashSet<>();

    String lastFileName = "";
    for (FileNode fileNode : directory.getFilesList()) {
      String fileName = fileNode.getName();
      Preconditions.checkState(
          !entryNames.contains(fileName),
          ExecutePreconditionViolationType.DUPLICATE_FILE_NODE);
      /* FIXME serverside validity check? regex?
      Preconditions.checkState(
          fileName.isValidFilename(),
          ExecutePreconditionViolationType.INVALID_FILE_NAME);
      */
      Preconditions.checkState(
          lastFileName.compareTo(fileName) < 0,
          ExecutePreconditionViolationType.DIRECTORY_NOT_SORTED);
      lastFileName = fileName;
      entryNames.add(fileName);

      inputDigests.add(fileNode.getDigest());
    }
    String lastDirectoryName = "";
    for (DirectoryNode directoryNode : directory.getDirectoriesList()) {
      String directoryName = directoryNode.getName();

      Preconditions.checkState(
          !entryNames.contains(directoryName),
          ExecutePreconditionViolationType.DUPLICATE_FILE_NODE);
      /* FIXME serverside validity check? regex?
      Preconditions.checkState(
          directoryName.isValidFilename(),
          ExecutePreconditionViolationType.INVALID_FILE_NAME);
      */
      Preconditions.checkState(
          lastDirectoryName.compareTo(directoryName) < 0,
          ExecutePreconditionViolationType.DIRECTORY_NOT_SORTED);
      lastDirectoryName = directoryName;
      entryNames.add(directoryName);

      Preconditions.checkState(
          !path.contains(directoryNode.getDigest()),
          ExecutePreconditionViolationType.DIRECTORY_CYCLE_DETECTED);

      Digest directoryDigest = directoryNode.getDigest();
      if (!visited.contains(directoryDigest)) {
        path.push(directoryDigest);
        validateActionInputDirectory(expectDirectory(directoryDigest), path, visited, inputDigests);
        path.pop();
        visited.add(directoryDigest);
      }
    }
  }

  private void validateActionInputs(Digest inputRootDigest, ImmutableSet.Builder<Digest> inputDigests) {
    Stack<Digest> path = new Stack<>();
    path.push(inputRootDigest);

    Directory root = expectDirectory(inputRootDigest);
    validateActionInputDirectory(root, path, new HashSet<>(), inputDigests);
  }

  private void validateAction(Action action) {
    Digest commandDigest = action.getCommandDigest();
    ImmutableSet.Builder<Digest> inputDigests = new ImmutableSet.Builder<>();
    inputDigests.add(commandDigest);

    validateActionInputs(action.getInputRootDigest(), inputDigests);

    // A requested input (or the [Command][] of the [Action][]) was not found in
    // the [ContentAddressableStorage][].
    Iterable<Digest> missingBlobDigests = findMissingBlobs(inputDigests.build());
    if (!Iterables.isEmpty(missingBlobDigests)) {
      Preconditions.checkState(
          Iterables.isEmpty(missingBlobDigests),
          ExecutePreconditionViolationType.MISSING_INPUT);
    }

    // FIXME should input/output collisions (through directories) be another
    // invalid action?
    filesUniqueAndSortedPrecondition(action.getOutputFilesList());
    filesUniqueAndSortedPrecondition(action.getOutputDirectoriesList());
    Command command;
    try {
      command = Command.parseFrom(getBlob(commandDigest));
    } catch (InvalidProtocolBufferException ex) {
      Preconditions.checkState(
          false,
          ExecutePreconditionViolationType.INVALID_DIGEST);
      return;
    }
    environmentVariablesUniqueAndSortedPrecondition(
        command.getEnvironmentVariablesList());
  }

  @Override
  public void execute(
      Action action,
      boolean skipCacheLookup,
      int totalInputFileCount,
      long totalInputFileBytes,
      boolean waitForCompletion,
      Consumer<Operation> onOperation) {
    validateAction(action);

    Operation operation = createOperation(action);
    ExecuteOperationMetadata metadata =
      expectExecuteOperationMetadata(operation);

    putOperation(operation);

    if (!waitForCompletion) {
      onOperation.accept(operation);
    }

    Operation.Builder operationBuilder = operation.toBuilder();
    ActionResult actionResult = null;
    if (!skipCacheLookup) {
      metadata = metadata.toBuilder()
          .setStage(ExecuteOperationMetadata.Stage.CACHE_CHECK)
          .build();
      putOperation(operationBuilder
          .setMetadata(Any.pack(metadata))
          .build());
      actionResult = getActionResult(Digests.computeDigest(action));
    }

    if (actionResult != null) {
      metadata = metadata.toBuilder()
          .setStage(ExecuteOperationMetadata.Stage.COMPLETED)
          .build();
      operationBuilder
          .setDone(true)
          .setResponse(Any.pack(ExecuteResponse.newBuilder()
              .setResult(actionResult)
              .setCachedResult(actionResult != null)
              .build()));
    } else {
      onQueue(operation, action);
      metadata = metadata.toBuilder()
          .setStage(ExecuteOperationMetadata.Stage.QUEUED)
          .build();
    }

    operation = operationBuilder
        .setMetadata(Any.pack(metadata))
        .build();
    /* TODO record file count/size for matching purposes? */

    if (!operation.getDone()) {
      updateOperationWatchers(operation); // updates watchers initially for queued stage
      if (waitForCompletion) {
        watchOperation(operation.getName(), /*watchInitialState=*/ false, o -> {
          if (o.getDone()) {
            onOperation.accept(o);
          }
          return true;
        });
      }
    }
    putOperation(operation);
  }

  protected ExecuteOperationMetadata expectExecuteOperationMetadata(
      Operation operation) {
    Preconditions.checkState(
        operation.getMetadata().is(ExecuteOperationMetadata.class));
    try {
      return operation.getMetadata().unpack(ExecuteOperationMetadata.class);
    } catch(InvalidProtocolBufferException ex) {
      return null;
    }
  }

  protected Action expectAction(Operation operation) {
    try {
      return Action.parseFrom(getBlob(
          expectExecuteOperationMetadata(operation).getActionDigest()));
    } catch(InvalidProtocolBufferException ex) {
      return null;
    }
  }

  protected Directory expectDirectory(Digest directoryBlobDigest) {
    try {
      ByteString directoryBlob = getBlob(directoryBlobDigest);
      if (directoryBlob != null) {
        return Directory.parseFrom(directoryBlob);
      }
    } catch(InvalidProtocolBufferException ex) {
    }
    return null;
  }

  protected boolean isCancelled(Operation operation) {
    return operation.getDone() &&
        operation.getResultCase() == Operation.ResultCase.ERROR &&
        operation.getError().getCode() == Status.Code.CANCELLED.value();
  }

  protected boolean isQueued(Operation operation) {
    return expectExecuteOperationMetadata(operation).getStage() ==
        ExecuteOperationMetadata.Stage.QUEUED;
  }

  protected boolean isExecuting(Operation operation) {
    return expectExecuteOperationMetadata(operation).getStage() ==
        ExecuteOperationMetadata.Stage.EXECUTING;
  }

  protected boolean isComplete(Operation operation) {
    return expectExecuteOperationMetadata(operation).getStage() ==
        ExecuteOperationMetadata.Stage.COMPLETED;
  }

  abstract protected boolean matchOperation(Operation operation);
  abstract protected void enqueueOperation(Operation operation);

  @Override
  public boolean putOperation(Operation operation) {
    if (isCancelled(operation)) {
      throw new IllegalStateException();
    }
    if (isExecuting(operation) &&
        !outstandingOperations.containsKey(operation.getName())) {
      return false;
    }
    if (isQueued(operation)) {
      if (!matchOperation(operation)) {
        enqueueOperation(operation);
      }
    } else {
      updateOperationWatchers(operation);
    }
    return true;
  }

  /**
   * per-operation lock factory/indexer method
   *
   * the lock retrieved for an operation will guard against races
   * during transfers/retrievals/removals
   */
  protected abstract Object operationLock(String operationName);

  protected void updateOperationWatchers(Operation operation) {
    if (operation.getDone()) {
      synchronized(operationLock(operation.getName())) {
        completedOperations.put(operation.getName(), operation);
        outstandingOperations.remove(operation.getName());
      }
    } else {
      outstandingOperations.put(operation.getName(), operation);
    }
  }

  @Override
  public Operation getOperation(String name) {
    synchronized(operationLock(name)) {
      Operation operation = completedOperations.get(name);
      if (operation == null) {
        operation = outstandingOperations.get(name);
      }
      return operation;
    }
  }

  protected abstract int getListOperationsDefaultPageSize();
  protected abstract int getListOperationsMaxPageSize();
  protected abstract TokenizableIterator<Operation> createOperationsIterator(String pageToken);

  @Override
  public String listOperations(
      int pageSize, String pageToken, String filter,
      ImmutableList.Builder<Operation> operations) {
    if (pageSize == 0) {
      pageSize = getListOperationsDefaultPageSize();
    } else if (getListOperationsMaxPageSize() > 0 &&
        pageSize > getListOperationsMaxPageSize()) {
      pageSize = getListOperationsMaxPageSize();
    }

    // FIXME filter?
    TokenizableIterator<Operation> iter = createOperationsIterator(pageToken);

    while (iter.hasNext() && pageSize != 0) {
      Operation operation = iter.next();
      operations.add(operation);
      if (pageSize > 0) {
        pageSize--;
      }
    }
    return iter.toNextPageToken();
  }

  @Override
  public void deleteOperation(String name) {
    synchronized(operationLock(name)) {
      Operation deletedOperation = completedOperations.remove(name);
      if (deletedOperation == null &&
          outstandingOperations.containsKey(name)) {
        throw new IllegalStateException();
      }
    }
  }

  @Override
  public void cancelOperation(String name) {
    Operation operation = getOperation(name);
    putOperation(operation.toBuilder()
        .setDone(true)
        .setError(com.google.rpc.Status.newBuilder()
            .setCode(com.google.rpc.Code.CANCELLED.getNumber())
            .build())
        .build());
  }

  protected void expireOperation(Operation operation) {
    Action action = expectAction(operation);
    Digest actionDigest = Digests.computeDigest(action);
    // one last chance to get partial information from worker
    ActionResult actionResult = action.getDoNotCache()
        ? null
        : getActionResult(actionDigest);
    boolean cachedResult = actionResult != null;
    if (!cachedResult) {
      actionResult = ActionResult.newBuilder()
          .setExitCode(-1)
          .setStderrRaw(ByteString.copyFromUtf8(
              "[BUILDFARM]: Action timed out with no response from worker"))
          .build();
      if (!action.getDoNotCache()) {
        putActionResult(actionDigest, actionResult);
      }
    }
    putOperation(operation.newBuilder()
        .setDone(true)
        .setMetadata(Any.pack(ExecuteOperationMetadata.newBuilder()
            .setStage(ExecuteOperationMetadata.Stage.COMPLETED)
            .build()))
        .setResponse(Any.pack(ExecuteResponse.newBuilder()
            .setResult(actionResult)
            .setCachedResult(cachedResult)
            .build()))
        .build());
  }

  @Override
  public boolean pollOperation(
      String operationName,
      ExecuteOperationMetadata.Stage stage) {
    if (stage != ExecuteOperationMetadata.Stage.QUEUED
        && stage != ExecuteOperationMetadata.Stage.EXECUTING) {
      return false;
    }
    Operation operation = getOperation(operationName);
    if (operation == null) {
      return false;
    }
    ExecuteOperationMetadata metadata = expectExecuteOperationMetadata(operation);
    if (metadata == null) {
      return false;
    }
    // stage limitation to {QUEUED, EXECUTING} above is required
    if (metadata.getStage() != stage) {
      return false;
    }
    return true;
  }
}
