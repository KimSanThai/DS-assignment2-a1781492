import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ContentServer
{
    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.out.println("Usage: java ContentServer <server_name:port_number> <file_path>");
            return;
        }

        String serverURL = args[0];
        String filepath = args[1];

        //Initialize timeout counter and lamport clock and response Code
        int timeout = 0;
        int Lamport_Clock = 0;
        int responseCode = 0;

        try 
        {
            while (true)
            {
                //Create a socket connection to the AggregationServer
                try (Socket socket = argumentParser(serverURL))
                {
                    //Get the output stream of the socket
                    OutputStream os = socket.getOutputStream();

                    //Set a timeout for the socket (15 seconds)
                    socket.setSoTimeout(15000);

                    //Read data to be sent from file
                    String requestBody = JSONreader(filepath);

                    //Construct the PUT request
                    String request = "PUT / HTTP/1.1\r\n"
                            + "Content-Length: " + requestBody.length() + "\r\n"
                            + "Lamport-Clock: " + Lamport_Clock + "\r\n"
                            + "\r\n"
                            +"\n"
                            + requestBody;

                    //Convert the request to bytes and send it
                    Lamport_Clock = Lamport_Clock + 1;

                    byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
                    os.write(requestBytes);
                    os.flush();

                    //Get the input stream to read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String responseLine = reader.readLine();
                    StringBuilder responseBody = new StringBuilder();
                    if (responseLine != null)
                    {
                        responseCode = Integer.parseInt(responseLine.split(" ")[1]);
                        System.out.println("Response Code: " + responseCode);

                        if (responseLine.isEmpty())
                        {
                            //Empty line indicates the end of headers
                            break;
                        }   
                    }

                    //Reads rest of body
                    while ((responseLine = reader.readLine()) != null)
                    {
                        responseBody.append(responseLine);
                    }
                    //Get Lamport Clock
                    int tmp_LC = Integer.parseInt(responseBody.toString().trim());

                    //Lamport comparison and update CS LC
                    if(Lamport_Clock > tmp_LC)
                    {
                        Lamport_Clock = Lamport_Clock + 1;
                    }
                    else
                    {
                        Lamport_Clock = tmp_LC + 1;
                    }

                    //If response code is 200, wait for 20 seconds before sending the next PUT request
                    if (responseCode == 200 || responseCode == 201)
                    {
                        //Reset the timeout counter
                        timeout = 0;

                        Thread.sleep(20000);
                    }
                    else
                    {
                        System.out.println("Response Code: " + responseCode);
                        break;
                    }
                }

                catch (java.net.SocketTimeoutException | ConnectException e)
                {
                    //If no response or connection issue, retry every 5 seconds
                    System.out.println("No response received or connection issue. Retrying in 5 seconds...");
                    timeout++;
                    System.out.println("Retry number: " + timeout);

                    //If 3 consecutive timeouts occur, exit
                    if (timeout == 3)
                    {
                        System.out.println("Unable to connect to server");
                        return;
                    }

                    Thread.sleep(5000);
                }
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Read data and turn into JSON format
    public static String JSONreader(String filepath) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath)))
        {
            StringBuilder jsonString = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) 
            {
                String[] parts = line.split(":");
                if (parts.length == 2)
                {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    jsonString.append("\"").append(key).append("\":").append("\"").append(value).append("\",");
                }
            }
            
            //Remove the trailing comma and wrap the JSON object
            jsonString.deleteCharAt(jsonString.length() - 1);
            jsonString.insert(0, "{");
            jsonString.append("}");

            return jsonString.toString();
        }
    }

    //Parse the argument 0 from the terminal into host and port number then returns socket with those parameters
    private static Socket argumentParser(String serverURL) throws IOException
    {
        String[] parts = serverURL.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new Socket(host, port);
    }
}
