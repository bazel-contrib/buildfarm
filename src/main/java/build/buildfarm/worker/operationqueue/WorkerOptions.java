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

package build.buildfarm.worker.operationqueue;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

/**
 * Command-line options definition for Worker.
 */
public class WorkerOptions extends OptionsBase {

  @Option(
      name = "help",
      abbrev = 'h',
      help = "Prints usage info.",
      defaultValue = "true"
    )
  public boolean help;

  @Option(
      name = "root",
      help = "Root base directory for all work being performed.",
      defaultValue = ""
    )
  public String root;

  @Option(
      name = "cas_cache_directory",
      category = "CAS File Cache",
      help = "(Absolute or relative to root) path to cached files from CAS.",
      defaultValue = ""
    )
  public String casCacheDirectory;

  @Option(
      name = "operation_queue_host_var",
      category = "Operation Queue",
      help = "Name of the environment variable containing the operation_queue service host.",
      defaultValue = ""
    )
  public String operationQueueHostVar;
    
  @Option(
      name = "operation_queue_port_var",
      category = "Operation Queue",
      help = "Name of the environment variable containing the operation_queue service port.",
      defaultValue = ""
    )
  public String operationQueuePortVar;
      
  @Option(
      name = "content_addressable_storage_host_var",
      category = "Content Addressable Storage",
      help = "Name of the environment variable containing the operation_queue service host.",
      defaultValue = ""
    )
  public String contentAddressableStorageHostVar;
    
  @Option(
      name = "content_addressable_storage_port_var",
      category = "Content Addressable Storage",
      help = "Name of the environment variable containing the content_addressable_storage service port.",
      defaultValue = ""
    )
  public String contentAddressableStoragePortVar;
      
  @Option(
      name = "action_cache_host_var",
      category = "Action Cache",
      help = "Name of the environment variable containing the action_cache service host.",
      defaultValue = ""
    )
  public String actionCacheHostVar;
    
  @Option(
      name = "action_cache_port_var",
      category = "Action Cache",
      help = "Name of the environment variable containing the action_cache service port.",
      defaultValue = ""
    )
  public String actionCachePortVar;
      
}
