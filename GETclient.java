import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;

public class GETclient {
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: java GETclient <server_name:port_number>");
            return;
        }

        String serverURL = args[0];
        int timeout = 0;
        int Lamport_Clock = 0;

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

                    //Get the input stream to read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    boolean headerCheck = true;
                    boolean Lamport_check = false;
                    StringBuilder content = new StringBuilder();
                    int tmp_LC;

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
                                    //Code to get Lamport Clock
                                    if(Lamport_check == false)
                                    {
                                        line = reader.readLine();
                                        tmp_LC = Integer.parseInt(line.split(":")[1]);
                                        Lamport_check = true;

                                        //Compare with current lamport clock and update it
                                        if(Lamport_Clock > tmp_LC)
                                        {
                                            Lamport_Clock++;
                                        }
                                        else
                                        {
                                            Lamport_Clock = tmp_LC + 1;
                                        }
                                        System.out.println(Lamport_Clock);
                                    }

                                    //Gets the rest of the content
                                    else
                                    {
                                        content.append(line).append("\n");
                                    }
                                }
                            }

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

    //Parse the argument 0 from the terminal into host and port number then returns socket with those parameters
    private static Socket arguementParser(String serverURL) throws IOException
    {
        String[] parts = serverURL.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new Socket(host, port);
    }
}
