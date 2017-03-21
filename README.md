time-hz-executor
=======
    
My pet project.  
This is a prototype of multithreaded and distributed service executes incoming tasks on a scheduled time. Powered by: `Java SE`, `Guice` and `Hazelcast`. Project uses SEDA like highly customizable thread model: independent tasks for watching, executing data and queue, distributed map between them.
  
  
## Rules:  
![export-2](https://cloud.githubusercontent.com/assets/4469702/24129401/85e05708-0df3-11e7-8dd0-17f8bbb1e12e.png)

  * The cluster of Hazelcast nodes accepts tasks with `LocalDateTime` and `Callable<?>`. 
  * `LocalDateTime` is a scheduled time and `Callable<?>` is a task for execution on that time.
  * Order of the execution uses scheduled time or number of inbound order.
  * Tasks could comes in a random order
  * 'Hot' tasks should not waste time and executes immediately.

  
## Requirements:

  * Java SE Development Kit 8 (or newer)  
  * Gradle 2.x (or you could use Gradle wrapper)  


## Project configuration:  

  * Java SE should be installed and you need to set path variable for `JAVA_HOME`.
  * Gradle doesn't need to install because you might do this automatically thanks Gradle Wrapper.


## Run
  *  Build project. Go to the root path `/time-hz-executor/` of the project and run:  
```sh
time-hz-executor$ ./gradlew clean build
Version 0.1
:client-cli:clean UP-TO-DATE
:hz-node:clean UP-TO-DATE
:shared:clean UP-TO-DATE
:shared:compileJava
:shared:processResources
:shared:classes
:shared:jar
:client-cli:compileJava
:client-cli:processResources
:client-cli:classes
:client-cli:jar
:client-cli:assemble
:client-cli:compileTestJava NO-SOURCE
:client-cli:processTestResources NO-SOURCE
:client-cli:testClasses UP-TO-DATE
:client-cli:test NO-SOURCE
:client-cli:check UP-TO-DATE
:client-cli:build
:hz-node:compileJava
:hz-node:processResources
:hz-node:classes
:hz-node:jar
:hz-node:assemble
:hz-node:compileTestJava NO-SOURCE
:hz-node:processTestResources NO-SOURCE
:hz-node:testClasses UP-TO-DATE
:hz-node:test NO-SOURCE
:hz-node:check UP-TO-DATE
:hz-node:build
:shared:assemble
:shared:compileTestJava
:shared:processTestResources NO-SOURCE
:shared:testClasses
:shared:test
:shared:check
:shared:build

BUILD SUCCESSFUL

Total time: 5.176 secs

```  
  *  Run Hazelcast cluster:   
```sh
time-hz-executor/scripts$ ./start_hz_cluster.sh

... <cut> ...

05:43:37.025 [main] INFO  r.s.c.ThreadPoolBuilder - create thread pool: node.executor, threads [8..32], idleTime: 60 SECONDS, queue: SynchronousQueue []
05:43:37.094 [main] INFO  r.s.c.Node - ----- // -----    Node: 1 START 2017-03-21T05:43:37.093    ----- // -----
05:43:37.096 [node-hz-1] INFO  r.s.c.Node - Node: 1 starting...
05:43:37.096 [node-hz-1] INFO  r.s.c.ServiceController - Node:1 services starting...
05:43:37.096 [node-main-1] INFO  r.s.c.Node - Node: 1 thread: Thread[node-main-1,5,main] await the state: IDLE to stop itself
05:43:37.097 [node-main-1] DEBUG r.s.c.Node - Thread: Thread[node-main-1,5,main] is alive
05:43:37.118 [node-hz-1] INFO  r.s.h.HzService - Hz server service starting...
05:43:37.120 [node-hz-1] DEBUG r.s.h.HzBuilder - Load HZ server instance...
мар 21, 2017 5:43:37 AM com.hazelcast.config.ClasspathXmlConfig
INFO: Configuring Hazelcast from 'hazelcast.xml'.
05:43:37.197 [main] INFO  r.s.c.ThreadPoolBuilder - create thread pool: node.executor, threads [8..32], idleTime: 60 SECONDS, queue: SynchronousQueue []
05:43:37.284 [main] INFO  r.s.c.Node - ----- // -----    Node: 2 START 2017-03-21T05:43:37.284    ----- // -----
05:43:37.290 [node-hz-2] INFO  r.s.c.Node - Node: 2 starting...
05:43:37.291 [node-hz-2] INFO  r.s.c.ServiceController - Node:2 services starting...
05:43:37.291 [node-main-2] INFO  r.s.c.Node - Node: 2 thread: Thread[node-main-2,5,main] await the state: IDLE to stop itself
05:43:37.291 [node-main-2] DEBUG r.s.c.Node - Thread: Thread[node-main-2,5,main] is alive
05:43:37.312 [node-hz-2] INFO  r.s.h.HzService - Hz server service starting...
05:43:37.315 [node-hz-2] DEBUG r.s.h.HzBuilder - Load HZ server instance...
мар 21, 2017 5:43:37 AM com.hazelcast.config.ClasspathXmlConfig
INFO: Configuring Hazelcast from 'hazelcast.xml'.
05:43:37.412 [main] INFO  r.s.c.ThreadPoolBuilder - create thread pool: node.executor, threads [8..32], idleTime: 60 SECONDS, queue: SynchronousQueue []
05:43:37.542 [main] INFO  r.s.c.Node - ----- // -----    Node: 3 START 2017-03-21T05:43:37.542    ----- // -----
05:43:37.545 [node-hz-3] INFO  r.s.c.Node - Node: 3 starting...
05:43:37.546 [node-hz-3] INFO  r.s.c.ServiceController - Node:3 services starting...
05:43:37.546 [node-main-3] INFO  r.s.c.Node - Node: 3 thread: Thread[node-main-3,5,main] await the state: IDLE to stop itself

... <cut> ...


Members [3] {
	Member [127.0.0.1]:5701
	Member [127.0.0.1]:5702
	Member [127.0.0.1]:5703 this
}

05:43:50.659 [node-hz-1] INFO  c.h.p.InternalPartitionService - [127.0.0.1]:5701 [dev-node-hz] [3.6.7] Initializing cluster partition table arrangement...
05:43:50.670 [node-hz-1] INFO  c.h.i.HazelcastInstanceImpl - [127.0.0.1]:5701 [dev-node-hz] [3.6.7] HazelcastInstance starting after waiting for cluster size of 2
05:43:50.670 [node-hz-1] INFO  c.h.c.LifecycleService - [127.0.0.1]:5701 [dev-node-hz] [3.6.7] Address[127.0.0.1]:5701 is STARTED
05:43:50.670 [node-hz-1] INFO  r.s.h.HzService - Hz server service started
05:43:50.671 [node-hz-1] INFO  r.s.c.TaskTimeService - TaskTimeService Node:1 starting...
05:43:50.675 [node-hz-1] INFO  r.s.c.TaskTimeService - TaskTimeService Node:1 started
05:43:50.676 [node-hz-1] INFO  r.s.c.ServiceController - Listener: Node:1 has started all services  -->
05:43:50.676 [node-hz-1] INFO  r.s.c.ServiceController - Node:1 services started, state: RUN
05:43:50.681 [node-hz-1] INFO  r.s.c.Node - Node: 1 started, state: RUN
...
```  
