### E2E

- `lib` bazel java_library targets 
- `mvn-build-lib` bazelizer target that depends on `lib (bazel)` and `mvn-build-lib-one (bazelizer)`, 
also it inherit `mvn-parent-pom` that allows to resolve build plugin version  
- `mvn-build-lib-one` single bazelizer target 
- `mvn-build-lib-with-profile` single bazelizer target that use personal maven profile
- `mvn-parent-pom` maven's pom parent module