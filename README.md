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

