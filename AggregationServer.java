import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AggregationServer
{
    static int port = 4567;
    static AtomicInteger ClientID = new AtomicInteger(1);
    static AtomicInteger Lamport_Clock = new AtomicInteger(0);
    static PriorityBlockingQueue<Tasks> task_Queue = new PriorityBlockingQueue<Tasks>();

    //Parameterized constructor to take in port number
    AggregationServer(int p)
    {
        port = p;
    }

    //Method to start the server
    public void start()
    {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("HTTP Server listening on port " + port);

            QueueHandler QH = new QueueHandler();
            QH.start();

            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                
                //Create a new client thread and start it
                ClientThread CT = new ClientThread(clientSocket);
                CT.start();
            }
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void RequestHandler(BufferedReader in, OutputStream out) throws IOException
    {
        String requestLine = in.readLine();
        System.out.println("Received Request: " + requestLine);

        //Check if it's a GET request
        if (requestLine != null)
        {
            //code for GET request
            if(requestLine.startsWith("GET"))
            {
                GETParser(out);
            }

            //Code for PUT request
            else if(requestLine.startsWith("PUT"))
            {
                PUTParser(in, out);
            }

            //If request isn't GET or PUT
            else
            {
                String response = "HTTP/1.1 400 Bad Request\r\n\r\nThe request could not be understood or was missing required parameters.";
                out.write(response.getBytes());
            }
        }

        out.flush();
    }

    //Function to read in files
    public String read(String filepath) throws IOException
    {
        String file = new String(Files.readAllBytes(Paths.get(filepath)));
        return file;
    }

    //Parses GET request
    private void GETParser(OutputStream out) throws IOException
    {
        try
        {
            Lamport_Clock.getAndIncrement();
            String response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + "Lamport Clock:" + Lamport_Clock + "\n" + read("JSON/Data.json");
            out.write(response.getBytes());
        }

        catch (IOException e)
        {
            e.printStackTrace();
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n\r\nError reading the JSON file.";
            out.write(errorResponse.getBytes());
        }
    }

    //Parse PUT request and turns it into a task and gets added to priority blocking queue
    private void PUTParser(BufferedReader in, OutputStream out) throws IOException
    {
        //Reads the rest of the content, ignoring HTTP header and turns it into a tasks class
        StringBuilder content = new StringBuilder();
        String line;
        int contentLength = 0;
        int messageLamport = 0;
        boolean headerCheck = true;
        boolean contentCheck = false;
        boolean lamportCheck = false;

        while((line = in.readLine()) != null)
        {
            if(line.startsWith("Lamport-Clock:"))
            {
                lamportCheck = true;
                messageLamport = Integer.parseInt(line.split(":")[1].trim());
            }

            if(line.startsWith("Content-Length:"))
            {
                contentCheck = true;
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }

            if(line.isEmpty())
            {
                headerCheck = false;
                break;
            }
        }

        //If the HTTP header doesn't include a Lamport clock
        if(lamportCheck == false)
        {
            String response = "HTTP/1.1 400 Bad Request\r\n\r\nThe request could not be understood or was missing required parameters.";
            out.write(response.getBytes());
            return;
        }
        else if(contentCheck == false)
        {
            String response = "HTTP/1.1 400 Bad Request\r\n\r\nThe request could not be understood or was missing required parameters.";
            out.write(response.getBytes());
            return;
        }

        //Reads the content based on the content length given - Does not add task into queue if no content
        if(contentLength > 0)
        {
            char[] buffer = new char[contentLength];
            int bytesRead = 0;

            if(!headerCheck)
            {
                if(contentLength > 0)
                {
                    while(bytesRead < contentLength)
                    {
                        int charsRead = in.read(buffer, bytesRead, contentLength - bytesRead);
                        if(charsRead == -1)
                        {
                            break;
                        }
                        bytesRead += charsRead;
                    }
                    content.append(buffer, 0, bytesRead);
                }
                content.append("}");
            }

            //Adds tasks to queue and order based on lamport clock
            Tasks tmp = new Tasks(messageLamport, content.toString());
            task_Queue.add(tmp);

            //Updates the lamport clock to send back to Content Server
            LamportUpdate(messageLamport);

            //Sends different response based on if Data.json previously exists
            File f = new File("JSON/Data.json");
            if(f.exists())
            {
                //Respond to ContentServer
                String response = "HTTP/1.1 200 OK\r\n\r\n" + Lamport_Clock.get();
                out.write(response.getBytes());
            }
            else
            {
                f.createNewFile();

                //Respond to ContentServer
                String response = "HTTP/1.1 201 HTTP_CREATED\r\n\r\n" + Lamport_Clock.get();
                out.write(response.getBytes());
            }
        }
        else
        {
            String response = "HTTP/1.1 204 No Content\r\n\r\nNo content recieved";
            out.write(response.getBytes());
        }
    }

    //Creating thread class for each client connection
    public class ClientThread extends Thread
    {
        private Socket clientSocket;

        //Parameterized constructor
        ClientThread(Socket socket)
        {
            this.clientSocket = socket;
        }

        @Override
        public void run()
        {
            try
            (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
            )
            {
                RequestHandler(in, out);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //Class to handle processing of data in queue
    public class QueueHandler extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    if(AggregationServer.task_Queue.peek() != null)
                    {
                        Tasks Prio_task = AggregationServer.task_Queue.poll();

                        write("JSON/Data.json", Prio_task.content);
                    }
                }
            }
            catch (IOException e)
            {
            }
        }
    }

    //Function to write data to file - with crash protection
    public void write(String filepath, String content) throws IOException
    {
        try
        {
            File f = new File(filepath);
            f.createNewFile();

            if(f.exists())
            {
                File temp = new File("temp.json");
                temp.createNewFile();
                FileWriter writer = new FileWriter(temp);
                writer.write(content);
                writer.close();
                f.delete();
                temp.renameTo(new File(filepath));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    //Function to update lamportClock
    public void LamportUpdate(int messageLamport)
    {
        if(Lamport_Clock.get() > messageLamport)
        {
            Lamport_Clock.getAndIncrement();
        }
        else
        {
            Lamport_Clock.set(messageLamport + 1);
        }
    }

    public static void main(String args[]) throws IOException
    {
        int portNumber = 4567;
        if(args.length > 1 || args.length < 0)
        {
            System.out.println("Usage: java AggregationServer <port_number> or java AggregationServer (For default port: 4567)");
        }

        if(args.length == 1)
        {
            portNumber = Integer.parseInt(args[0]);
        }

        AggregationServer AS = new AggregationServer(portNumber);
        AS.start();
    }
}