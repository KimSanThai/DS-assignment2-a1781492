import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GETclient {
    public static void main(String[] args) throws IOException
    {
        if (args.length != 1)
        {
            System.out.println("Usage: java GETclient <server_name:port_number>");
            return;
        }

        String serverURL = args[0];
        int timeout = 0;
        int Lamport_Clock = 0;
        String LC_filepath = "Lamport-Clocks/GC1.txt";

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
            while (true)
            {
                try (Socket socket = arguementParser(serverURL))
                {
                    //Constructing the GET request
                    String getRequest = "GET / HTTP/1.1\r\n" +
                                        "Host: UwU" + "\r\n" +
                                        "\r\n";

                    //Get the output stream of the socket
                    OutputStream os = socket.getOutputStream();

                    //Convert the request to bytes and send it
                    byte[] getRequestBytes = getRequest.getBytes();
                    os.write(getRequestBytes);
                    os.flush();
                    Lamport_Clock++;

                    //Get the input stream to read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line = "";
                    boolean headerCheck = true;
                    StringBuilder content = new StringBuilder();
                    int tmp_LC = 0;

                    //Reads the header to parse status code
                    line = reader.readLine();
                    if(line != null)
                    {
                        //Parses the status code
                        int responseCode = Integer.parseInt(line.split(" ")[1]);
                        System.out.println("Response Code: " + responseCode);

                        if(responseCode == 200)
                        {
                            //Reads the rest of the content and prints the response content (also ignore HTTP header)
                            while ((line = reader.readLine()) != null)
                            {
                                if(line.isEmpty())
                                {
                                    headerCheck = false;
                                }
                                if(!headerCheck)
                                {
                                    //Takes Lamport Clock off the JSON data
                                    if(line.startsWith("Lamport Clock:"))
                                    {
                                        tmp_LC = Integer.parseInt(line.split(":")[1]);
                                    }
                                    else
                                    {
                                        content.append(line).append("\n");
                                    }
                                }
                            }

                            //Update lamport clock
                            if(Lamport_Clock > tmp_LC)
                            {
                                Lamport_Clock = Lamport_Clock + 1;
                            }
                            else
                            {
                                Lamport_Clock = tmp_LC + 1;
                            }

                            //Saves lamport clock to a file
                            //Save updated lamport
                            FileWriter writer = new FileWriter(LC_filepath);
                            writer.write(Integer.toString(Lamport_Clock));
                            writer.close();

                            System.out.println("Content:" + content);
                            return;
                        }
                        else
                        {
                            while ((line = reader.readLine()) != null)
                            {
                                content.append(line).append("\n");
                            }
                            System.out.println("Error Message:" + content);
                            return;
                        }
                    }
                }

                catch (java.net.SocketTimeoutException | ConnectException e)
                {
                    //If no response or connection issue, retry every 5 seconds
                    System.out.println("No response received or connection issue. Retrying in 5 seconds...");
                    timeout++;
                    System.out.println("Retry attempt: " + timeout);

                    //If 3 consecutive timeouts occur, exit
                    if(timeout == 3)
                    {
                        System.out.println("Unable to connect to server");
                        return;
                    }

                    Thread.sleep(5000);
                }
            }
        }
        
        catch(IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    //Function to read in files
    public static String read(String filepath) throws IOException
    {
        String file = new String(Files.readAllBytes(Paths.get(filepath)));
        return file;
    }

    //Parse the argument 0 from the terminal into host and port number then returns socket with those parameters
    private static Socket arguementParser(String serverURL) throws IOException
    {
        String[] parts = serverURL.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new Socket(host, port);
    }
}
