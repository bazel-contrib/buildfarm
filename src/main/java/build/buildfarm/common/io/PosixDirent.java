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

package build.buildfarm.common.io;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Objects;
import javax.annotation.Nullable;

/** Directory entry representation returned by {@link Path#readdir}. */
public final class PosixDirent implements Serializable, Comparable<PosixDirent> {

  private final String name;
  @Nullable private final PosixFileAttributes stat;

  /** Creates a new posix dirent with the given name */
  public PosixDirent(String name, PosixFileAttributes stat) {
    this.name = Preconditions.checkNotNull(name);
    this.stat = stat;
  }

  public String getName() {
    return name;
  }

  @Nullable
  public PosixFileAttributes getStat() {
    return stat;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof PosixDirent)) {
      return false;
    }
    if (this == other) {
      return true;
    }
    PosixDirent otherPosixDirent = (PosixDirent) other;
    return name.equals(otherPosixDirent.name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(PosixDirent other) {
    return this.getName().compareTo(other.getName());
  }
}
