def run_tests(name, data, srcs, package, deps):
  for src in srcs:
    src_name = src[:-5]
    native.java_test(name=src_name, test_class=package + "." + src_name, srcs=srcs, deps=deps, size="small", data=data)
