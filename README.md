time-hz-executor
=======
    
My pet project.  
This is a prototype of multithreaded and distributed service executes incoming tasks on a schedule time. Powered by: `Java SE`, `Guice` and `Hazelcast`. Project uses SEDA like highly customizable thread model: independent tasks for watching, executing data and queue, distributed map between them.
  
  
## Rules:  

  * Choose the directory that needs to be watch. 
  * Successfully processed XML files move to `<dir>/success/` directory.
  * Files processed with fail should move to `<dir>/fail/` directory.
  * XML files should have similiar content:  
```xml  
<?xml version="1.0" encoding="utf-8" ?>
<Entry>
    <!-- length of string 1024 characters -->
    <content>Text</content>
    <!-- local date -->
    <creationDate>2014-01-01 00:00:00</creationDate>
</Entry>
```   

  
## Requirements:

  * Java SE Development Kit 8 (or newer)  
  * Gradle 2.x (or you could use Gradle wrapper)  

