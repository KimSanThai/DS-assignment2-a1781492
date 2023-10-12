import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

public class ContentServer
{
    public static void main(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            System.out.println("Usage: java ContentServer <server_name:port_number> <file_path>");
            return;
        }

        String serverURL = args[0];
        String filepath = args[1];
        String LC_filepath = "Lamport-Clocks/CS1.txt";

        //Initialize timeout counter and lamport clock and response Code
        int timeout = 0;
        int responseCode = 0;
        int Lamport_Clock = 0;

        //Check if lamport clock is saved before
        File f = new File(LC_filepath);
        
        if(!f.createNewFile())
        {
            String tmp = read(LC_filepath);
            //Checks if tmp is empty
            if(tmp.isEmpty())
            {
                FileWriter writer = new FileWriter(LC_filepath);
                writer.write(Integer.toString(Lamport_Clock));
                writer.close();
                tmp = Integer.toString(Lamport_Clock);
            }
            Lamport_Clock = Integer.parseInt(tmp);
        }
        else
        {
            FileWriter writer = new FileWriter(LC_filepath);
            writer.write(Integer.toString(Lamport_Clock));
            writer.close();
        }

        try
        {
            Vector<String> data = JSONreader(filepath, 1);

            while(true)
            {
                System.out.println("Number of JSON objects to send: " + data.size());

                for (int i = 0; i < data.size(); i++)
                {
                    while(true)
                    {
                        //Create a socket connection to the AggregationServer
                        try (Socket socket = argumentParser(serverURL))
                        {
                            //Get the output stream of the socket
                            OutputStream os = socket.getOutputStream();

                            //Set a timeout for the socket (15 seconds)
                            socket.setSoTimeout(15000);
                            
                            //Read data to be sent from file
                            String requestBody = data.get(i);

                            //Construct the PUT request
                            String request = "PUT / HTTP/1.1\r\n"
                                    + "Content-Length: " + requestBody.length() + "\r\n"
                                    + "Lamport-Clock: " + Lamport_Clock + "\r\n"
                                    + "\r\n"
                                    + requestBody;

                            //Convert the request to bytes and send it
                            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
                            os.write(requestBytes);
                            os.flush();

                            //Increment Lamport Clock
                            Lamport_Clock = Lamport_Clock + 1;

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

                            //If recieve No content error message
                            if(responseCode == 204)
                            {
                                System.out.println(responseBody);
                                timeout++;
                                System.out.println("Retrying in 5 seconds... Retry Number: " + timeout);
                                Thread.sleep(5000);
                                break;
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

                            //Save updated lamport
                            FileWriter writer = new FileWriter(LC_filepath);
                            writer.write(Integer.toString(Lamport_Clock));
                            writer.close();

                            //If response code is 200, wait for 20 seconds before sending the next PUT request
                            if (responseCode == 200 || responseCode == 201)
                            {
                                //Reset the timeout counter
                                timeout = 0;
                                System.out.println("Success!");
                                //Short sleep before sending other content from file
                                Thread.sleep(1000);
                                break;
                            }
                            else
                            {
                                System.out.println("Response Code: " + responseCode);
                                break;
                            }
                        }
                        catch (java.net.SocketTimeoutException | java.net.SocketException e)
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
                //Break 1 more time if response Code 204 - No content
                if(responseCode == 204)
                {
                    break;
                }

                //Wait 20 seconds before sending the next message
                System.out.println("Waiting 20 seconds before sending the next batch of messages.");
                Thread.sleep(20000);
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Read data and turn into JSON format
    public static Vector<String> JSONreader(String filepath, int CSID) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath)))
        {
            Vector<String> d = new Vector<>();
            StringBuilder jsonString = new StringBuilder();

            String line;

            //Check if file is empty and returns nothing if it is empty
            if((line = reader.readLine()) == null)
            {
                return null;
            }
            else
            {
                String[] parts = line.split(":");
                if (parts.length == 2)
                {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    jsonString.append("\"").append(key).append("\":").append("\"").append(value).append("\",");
                }
            }

            //Continue reading past the first line
            while ((line = reader.readLine()) != null) 
            {
                String[] parts = line.split(":");
                if (parts.length == 2)
                {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    jsonString.append("\"").append(key).append("\":").append("\"").append(value).append("\",");

                    if(key.equals("wind_spd_kt"))
                    {
                        //piggy-back Content Server ID onto JSON data
                        jsonString.insert(0,"\"CSID\":" + "\"" + CSID + "\",");
                        
                        //Remove the trailing comma and wrap the JSON object
                        jsonString.deleteCharAt(jsonString.length() - 1);
                        jsonString.insert(0, "{");
                        jsonString.append("}");

                        //Append string to vector
                        d.add(jsonString.toString());

                        //Reset jsonString
                        jsonString.setLength(0);
                    }
                }
            }

            return d;
        }
    }

    //Function to read in files
    public static String read(String filepath) throws IOException
    {
        String file = new String(Files.readAllBytes(Paths.get(filepath)));
        return file;
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
