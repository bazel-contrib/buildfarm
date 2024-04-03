"""
buildfarm dependencies that can be imported into other WORKSPACE files
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def archive_dependencies(third_party):
    return [
        # Needed for @grpc_java//compiler:grpc_java_plugin.
        {
            "name": "io_grpc_grpc_java",
            "sha256": "5d617856c295d863307f4036a1b1e93f9eeaf6da41424d2de7c9b330a810fc3b",
            "strip_prefix": "grpc-java-1.62.2",
            "urls": ["https://github.com/grpc/grpc-java/archive/v1.62.2.zip"],
            # Bzlmod: Waiting for https://github.com/bazelbuild/bazel-central-registry/issues/353
        },
        # The APIs that we implement.
        {
            "name": "googleapis",
            "build_file": "%s:BUILD.googleapis" % third_party,
            "patch_cmds": ["find google -name 'BUILD.bazel' -type f -delete"],
            "patch_cmds_win": ["Remove-Item google -Recurse -Include *.bazel"],
            "sha256": "1980dc4a4d02420d4da588665e3ecbe55e02a1c2e32d8906a2268c67d1085e0b",
            "strip_prefix": "googleapis-5f8a02d6b7e77bd26e0375a00ca20eb2f3ee1ba2",
            "url": "https://github.com/googleapis/googleapis/archive/5f8a02d6b7e77bd26e0375a00ca20eb2f3ee1ba2.zip",
        },
        {
            "name": "remote_apis",
            "build_file": "%s:BUILD.remote_apis" % third_party,
            "sha256": "e9a69cf51df14e20b7d3623ac9580bc8fb9275dda46305788e88eb768926b9c3",
            "strip_prefix": "remote-apis-8f539af4b407a4f649707f9632fc2b715c9aa065",
            "url": "https://github.com/bazelbuild/remote-apis/archive/8f539af4b407a4f649707f9632fc2b715c9aa065.zip",
        },

        # Used to format proto files
        {
            "name": "com_grail_bazel_toolchain",
            "sha256": "b2d168315dd0785f170b2b306b86e577c36e812b8f8b05568f9403141f2c24dd",
            "strip_prefix": "toolchains_llvm-0.9",
            "url": "https://github.com/bazel-contrib/toolchains_llvm/archive/refs/tags/0.9.tar.gz",
            "patch_args": ["-p1"],
            "patches": ["%s:clang_toolchain.patch" % third_party],
        },

        # Bazel is referenced as a dependency so that buildfarm can access the linux-sandbox as a potential execution wrapper.
        {
            "name": "bazel",
            "sha256": "06d3dbcba2286d45fc6479a87ccc649055821fc6da0c3c6801e73da780068397",
            "strip_prefix": "bazel-6.0.0",
            "urls": ["https://github.com/bazelbuild/bazel/archive/refs/tags/6.0.0.tar.gz"],
            "patch_args": ["-p1"],
            "patches": ["%s/bazel:bazel_visibility.patch" % third_party],
        },

        # Optional execution wrappers
        {
            "name": "skip_sleep",
            "build_file": "%s:BUILD.skip_sleep" % third_party,
            "sha256": "03980702e8e9b757df68aa26493ca4e8573770f15dd8a6684de728b9cb8549f1",
            "strip_prefix": "TARDIS-f54fa4743e67763bb1ad77039b3d15be64e2e564",
            "url": "https://github.com/Unilang/TARDIS/archive/f54fa4743e67763bb1ad77039b3d15be64e2e564.zip",
        },
    ]

def buildfarm_dependencies(repository_name = "build_buildfarm"):
    """
    Define all 3rd party archive rules for buildfarm

    Args:
      repository_name: the name of the repository
    """
    third_party = "@%s//third_party" % repository_name
    for dependency in archive_dependencies(third_party):
        params = {}
        params.update(**dependency)
        name = params.pop("name")
        maybe(http_archive, name, **params)

    maybe(
        http_jar,
        "opentelemetry",
        sha256 = "eccd069da36031667e5698705a6838d173d527a5affce6cc514a14da9dbf57d7",
        urls = [
            "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.28.0/opentelemetry-javaagent.jar",
        ],
    )

    http_file(
        name = "tini",
        sha256 = "12d20136605531b09a2c2dac02ccee85e1b874eb322ef6baf7561cd93f93c855",
        urls = ["https://github.com/krallin/tini/releases/download/v0.18.0/tini"],
    )
