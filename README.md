> :warning: **Note**: This project is using modules that are being phased out
in Reactor 3.x. 

If you have questions about Reactor 3, join the
community (see the [reactor README](https://github.com/reactor/reactor/blob/master/README.md)).

We are introducing various microbenchmarks in each project and we are taking a more end-to-end approach for meaningful scenarios such as [Brian's Benchmarks](https://github.com/bclozel/web-benchmarks).

# Reactor Benchmarks

These benchmarks measure the relative performance of various components of the Reactor architecture. There are also benchmarks for standard JDK components in different configurations to try and determine what's the fastest way to do various tasks.

### Running

The benchmarks are [JMH](http://openjdk.java.net/projects/code-tools/jmh/) based. To run them, clone the project, then run the `microbenchmarks.jar` using `java -jar`. For example, to run the `ReactorBenchmarks` which measure the performance of the event publication system, do something like the following:

		> git clone https://github.com/reactor/reactor-benchmark.git
		> cd reactor-benchmark
		> mvn package && java -jar target/microbenchmarks.jar ".*ReactorBenchmarks.*"

Sensible defaults are placed as annotations on the benchmark classes themselves. In many cases, those can be overridden by passing the appropriate CLI parameter. Refer to the JMH documentation for more information on what options are available and what effect they have on the benchmark runs.

Note that we are very sorry to ship this project with a Maven build, assistance welcome for moving to Gradle :) Pull Requests my friends !
