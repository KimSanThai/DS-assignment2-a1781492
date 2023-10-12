//Kim San Thai - a1781492

Design Brief:

Implement a REST architecture style system to send weather data in JSON format across server.
Implementation includes 3 main parts which are:
-Aggregation Server - Handles PUT and GET request to either store or send weather data in the JSON format.
-Content Server - Handles sending the aggregation server PUT requests and formatting the data to be sent in a JSON format before sending it off.
-GET client - Sends the GET request to the server and recieves a JSON string back
*The aggregation server, content server and get client all maintain a local lamport clock that gets sent back and forth to be in sync


Implementation:

Aggregation server:
The aggregation server runs off of 3 classes which are the ClientThread, QueueHandler and HeartBeat which all extends Threads.

-The ClientThread class handles reads all of the requests and sends back response messages (including errors) and converts PUT requests into a custom class I made called Tasks.
Tasks stores the clientID, the lamport clock and the content sent from the content server. The class implements Comparable with a compareTo method that compares Lamport clocks.
This allows it to be sorted within the PriorityBlockingQueue task_Queue which the ClientThread stores all tasks in.

-The QueueHandler class processes all the tasks in the PriorityBlockingQueue which is just the PUT requests as the GET request returns a response immediately.
This mainly involves taking tasks off of the queue according to the priority list and writing them down into a file.

-The HeartBeat class involves maintaining a ConcurrentHashMap that stores the content server ID as the key along with the time that the message is recieved.
The storing into the ConcurrentHashMap is done in the ClientThread class as PUT request are accepted.
The HeartBeat class sole purpose is to navigate through this HashMap and identify any Content Server that has not sent a message within 30 seconds and delete all data regarding that content server.


Content server:
The content server reads off a file and parses them into JSON format then sends them over in increments of 1 second before waiting 20 seconds to send the next batch of data.
It will also respond to error codes or failure to connect exceptions by retrying to send the data again every 5 seconds 3 times before shutting off.
The lamport clock is saved in a file to maintain a persistant state when the server is shut down.

GET client:
The GET client just sends the aggregation server a get request and recieves messages back.
It handles errors the same way the content server does including saving the lamport clock in a file.


Usage:
The code uses an extern org.json library, thus must be compiled with it included. (The file is "json-20230618.jar")
-Use command "javac -cp ./* *.java" to compile

Only the aggregation server uses this library
-Use command "java -cp ".;json-20230618.jar" AggregationServer <port_number>" to run aggregation server for windows.
-Use command "java -cp ".:json-20230618.jar" AggregationServer <port_number>" to run aggregation server for linux.
This will start the aggregation server at the specified port

-Use command "java ContentServer <host:port_number> <Data_filepath>" to run the content server
This will start a connection with the host specified on the port specified and send data from the filepath specified.

-Use command "java GETclient <host:port_number>" to run the get client
This will start a connection with the host specified on the port specified and send a get request.


Test:
The tests implemented will test the functionality provided in Appendix B of the assignment sheet.

Test 1: Text sending works
