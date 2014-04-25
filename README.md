Assignment3
============



>Agenda 



* Write the whole file to the server in chunks. 


* Read the whole file back using the previous APIs 


>Entities:

there are 3 entities as follows: 


*  Registry server: This will keep track of all instances of the FileServer 


*  File server:  runs multiple instances of the FileServer per node. For e.g
   there could be 3 servers per node. It will register itself with the RMI Registry    and Registry Server.. 

* Client:  The client will first contact the registry server to know the Binding
 name of FileServers.
  
  For writes, it willsend the FileWrite64K RPCs to the master.

  For reads,  to perform parallel reads, The client will create 
  multiple threads as many as the FileServers or as many as chunks whichever is      smaller. Now every thread will contact onefileserver’s Read64K api and make it     read the  chunk data eg : 


Example : 

[suppose you had 2 FileServers and 5 chunks] 

FileServer 0 reads Chunk 0, Chunk 2, Chunk 4 whereas FileServer 1 reads Chunk 1 & Chunk 3. 


>Error Handling:

 The client handles errors/exceptions while reading from a FileServer.

* The registry and all instances of FileServer can be first started. Once all FileServers are started, kill 1 or more (not all). 


* the multi-threaded FileClient can be launched to read a large file. The threads that are reading from the FileServer instances that are down will get errors. The client recovers from these errors by stopping that thread.

* The other threads will continue reading the file completely by reading different chunks from different FileServers.


Interfaces:


RegistryServer  provide the following interfaces: 
      
      boolean RegisterServer(String name)

      Each file server uses this interface to register to the registry server. 
      Return value is as follows: true means caller has been made the master, and false means caller is the replica 

Only master file server allows FileWrite64() APIs. The other file servers will return an error or throw an exception when they receive a write request. 

RegistryServer also provides another interface as follows: 

        String[] GetFileServers() 

        Client uses this to find the names of all instances of file server. 

        Return value is as follows: 

        - null on error 

        An array of file servers. Each file server is identified by the name used in the call to RegisterServer(). The first entry is the master file server. 
        

The RegistryServer first registers with the RMI registry, and waits for calls to RegisterServer() or to GetFileServers(). 

When it gets a call to RegisterServer(), it maintains the list of servers in a list. 

Each FileServer instance has to do the following: 

- Generate a unique name. This will be Server_(time since epoch in seconds). While
  starting multiple instances of Servers, ensure that there is a gap of 2 seconds    to ensure uniqueness in the name. 

- Register with RMI 

- Register with RegistryServer by calling RegisterServer() with the above name. 

- Maintain a boolean that tracks if this is the master- Only master handles writes 

Client program does the following: 

- Looks up our registry from RMI registry 

- Calls GetFileServers() to obtain all fileservers and the master 

- Calls Naming.lookup(fileServerName) to obtain handles to each of the FileServer 

  objects- Do writes to the master, and reads to any replica. 


>Example Usage:

Client/run.sh <Name of the File> <IP Address of RMI registry> <Port No of the RMI 

Registry/run.sh <IP Address of RMI registry> <Port No of the RMI Registry> 

FileServer/run.sh <IP Address of RMI registry> <Port No of the RMI Registry> 

 

Keep the folders under the folder with your roll no. 
