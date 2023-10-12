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
-Use command "javac -cp .:* *.java" to compile
Alternatively, the makefile will compile the code using command "make" does not work on windows.

Only the aggregation server uses this library
-Use command "java -cp ".:json-20230618.jar" AggregationServer <port_number>" to run aggregation server.
This will start the aggregation server at the specified port

-Use command "java ContentServer <host:port_number> <Data_filepath>" to run the content server
This will start a connection with the host specified on the port specified and send data from the filepath specified.

-Use command "java GETclient <host:port_number>" to run the get client
This will start a connection with the host specified on the port specified and send a get request.


Test:
The tests implemented will test the functionality provided in Appendix B of the assignment sheet. All test to be run after compile (See above).

Test 1: Text sending works/ client, server and content serveer processes start up and communicate/ PUT operation works for one content server
This test consists of 3 parts which must be run in order. Test1.part1 and Test1.part2 will have to be run concurrently on different terminals.
But Test1.part3 can be run any time after Test1.part2 is done.
-Script Test1.part1.sh will delete Data.txt first then start the aggregation server on local host on port 8000.
-Script Test1.part2.sh will start the content server on localhost on port 8000 and start sending the data in test1.txt (inside JSON folder).
-Script Test1.part3.sh will start the get client on localhost on port 8000 and send the get request.
Expected outcome:
The aggregation server will start listening on port 8000. The content server will then send data from test1.txt which will get written to Data.txt (inside JSON folder).
Then the get client will send a get request on port 8000 and recieve data from Data.txt (inside JSON folder) sent by the aggregation server.
If left running, occassionally, Test1.part1 will send warning messages as messages from the content server is sent every 20 seconds and the HeartBeat checks every 15 seconds before deleting files at 30 seconds.


Test 2: GET operation works for many read clientSocket
This test consists of 2 parts which must be run in order. Test2.part1 and Test2.part2 will have to be run concurrently on different terminals.
-Script Test2.part1.sh will start the content server in the background then start the aggregation server on local host on port 8000.
-Script Test2.part2.sh will rapidfire send multiple curl GET request to local host on port 8000
Expected outcome:
The content server will start sending content to localhost:8000 before the aggregation server starts on the same port. The content server will then send server to the aggregation server 5 seconds later after (because the content server initially fails to connect to the aggregation server because it has not started yet).
The second script will then concurrently send 4 GET curl request and print out the response. If Test2.part2 is ran after the data is sent properly by the content server, then 4 of the same json string should be recieved back.


Test 3: Aggregation server expunging expired data works (30s)
This code will reuse the script from Test 1.
-Script Test1.part1.sh will delete Data.txt (inside JSON folder) first then start the aggregation server on local host on port 8000.
-Script Test1.part2.sh will start the content server on localhost on port 8000 and start sending the data from text1.txt (inside JSON folder).
*Script Test1.part2.sh should be manually closed after data is sent.
Expected outcome:
The aggregation server will start listening on port 8000. The content server will then send data from test1.txt which will get written to Data.txt (inside JSON folder).
The content server should then be closed and the aggregation server will then empty the Data.txt file after 30 seconds of not recieving any messages.


Test 4: Retry on errors (server not available etc) works
This code will reuse the script from Test 1. Running Test1.part2 or Test1.part3 without running Test1.part1 first will automatically start the retry process.
The connection will proceed properly if Test1.part1 is run before Test1.part2 or Test1.part3.


Test 5: Lamport clocks are implemented
This test consists of 2 parts which must be run in order. Test5.part1 and Test5.part2 will have to be run concurrently on different terminals.
-Script Test5.part1.sh will delete Data.txt (inside JSON folder) and AS1.txt (inside Lamport-Clocks folder) first then start the aggregation server on local host on port 8000.
-Script Test5.part2.sh will delete CS1.txt (inside Lamport-Clocks folder) then start the content server on the same port and start sending data from text1.txt (inside JSON folder).
-Script Test5.part3.sh will delete GC1.txt (inside Lamport-Clocks folder) then start the get client on the same port and send a get request.
This test will delete the saved lamport clocks that accumulated from the previous tests before starting the same test as in Test 1. If the scripts are left running, the AS1.txt, CS1.txt and GC1.txt files (inside Lamport-Clocks folder) can be checked periodically to see the lamport clock get updated in real time as tasks are processed and messages are recieved.


Test 6: All error codes are implemented
This test will reuse the script from Test1
-Script Test1.part1.sh will delete Data.txt (inside JSON folder) first then start the aggregation server on local host on port 8000.
-Script Test6.sh will then send different curl requests to verify the error codes.
the curl request are:
curl -X POST http://localhost:8000 -v (sends a post request - recieve error code 400 bad request)
curl -X PUT http://localhost:8000 -v -H "Lamport-Clock: 0" -d "" (sends a PUT request with an empty body - recieve error code 204 no content)
curl -X PUT http://localhost:8000 -v -H "Lamport-Clock: 0" -d "?" (sends a PUT request with the wrong format - recieve error code 500)


Test 7: Content servers are replicated and fault tolerant
For content server, fault tolerance is shown test 4 (The retry test) (This also applies to the get client). Additionally, the lamport clocks are saved in a local file so that is persistant even through crashes (This applies to all server and client).
