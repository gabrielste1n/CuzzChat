package cuzzchat;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.filechooser.FileSystemView;

/**
 *
 * @author gabri
 */
public class Server
{

    // VECTOR TO STORE ACTIVE CLIENTS
    static Vector<ClientHandler> clientList = new Vector<>();
    static File home = FileSystemView.getFileSystemView().getHomeDirectory();
    static String desktop = home.getAbsolutePath();

    // COUNTER FOR CLIENTS
    static int i = 1;

    public static void main(String[] args) throws IOException
    {
        System.out.println("Server is runnning...");
        // SERVER IS LISTENING ON PORT 1234 
        ServerSocket serverSocket = new ServerSocket(1234);

        Socket socket;
        boolean server = (new File(desktop + "\\Server")).mkdir();
        // RUNNING INFINITE LOOP TO LISTEN FOR CLIENT REQUESTS
        while (true)
        {
            // ACCEPT THE INCOMING REQUEST
            socket = serverSocket.accept();
            System.out.println("New Cuzzy request received : " + socket);

            // OBTAIN INPUT AND OUTPUT STREAMS 
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            File cuzzyFolder = (new File(desktop + "\\Server\\" + "Cuzzy " + i));
            cuzzyFolder.mkdir();

            // CREATE A NEW CLIENT HANDLER OBJECT TO MANAGE THE CLIENT
            System.out.println("Creating a new handler for this Cuzzy...");
            ClientHandler activeClient = new ClientHandler(socket, "Cuzzy " + i, "", inputStream, outputStream, clientList);

            // CREATE A NEW THREAD FOR THIS OBJECT 
            Thread thread = new Thread(activeClient);

            // ADD CLIENT TO ACTIVE CLIENT LIST
            System.out.println("Adding this client to active Cuzzy list");
            clientList.add(activeClient);
            // START THE THREAD
            thread.start();

            // INCREMENT COUNTER FOR NEXT CLIENT
            i++;

        }
    }
}

class ClientHandler implements Runnable
{

    private String name;
    String filename;
    long fileSize;
    private String chattingPartnerName;
    final DataInputStream inputStream;
    final DataOutputStream outputStream;
    Socket socket;
    boolean online;
    File home = FileSystemView.getFileSystemView().getHomeDirectory();
    String desktop = home.getAbsolutePath();
    static Vector<ClientHandler> clientList;

    public ClientHandler(Socket socket, String name, String chattingPartnerName,
            DataInputStream inputStream, DataOutputStream outputStream, Vector<ClientHandler> clientList)
    {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.chattingPartnerName = chattingPartnerName;
        this.name = name;
        this.socket = socket;
        this.online = true;
        this.clientList = clientList;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public void run()
    {

        String received;
        while (true)
        {
            try
            {
                // MESSAGE SENT FROM CLIENT TO SERVER
                received = inputStream.readUTF();

                //this will go away
                if (received.equals("logout"))
                {
                    this.online = false;
                    this.socket.close();
                    System.out.println(name + " has left.");
                    break;
                }
                else if (received.equals("getUsername"))
                {
                    // SET CLIENT USERNAME
                    outputStream.writeUTF("username#" + name);

                    // TELLING THE OTHER CLIENTS TO ADD NEW CLIENT TO THEIR
                    // ACTIVE CLIENT LIST
                    for (ClientHandler client : clientList)
                    {
                        client.outputStream.writeUTF("addToActiveList#" + name);
                    }

                    // ADDING EVERYONE TO THIS CLIENTS ACTIVE CLIENT LIST
                    for (ClientHandler client : clientList)
                    {
                        outputStream.writeUTF("addToActiveList#" + client.getName());
                    }
                }
                else
                {

                    boolean chatMessage = false;

                    String dataType = "";
                    String message;
                    String recipientName;
                    // BREAK RECEIVED MESSAGE INTO TEXT AND RECIPIENT PART
                    StringTokenizer st = new StringTokenizer(received, "#");

                    // HANDLE FILE TRANSFER AND PLAIN TEXT MESSAGE REQUESTS
                    if (st.countTokens() == 2)
                    {
                        message = st.nextToken();
                        recipientName = st.nextToken();
                    }
                    else
                    {
                        //MESSAGE FORMAT: DATATYPE#TEXT#SENDER
                        dataType = st.nextToken();
                        message = st.nextToken();
                        recipientName = st.nextToken();

                    }

                    // SEARCH FOR RECIPIENT CLIENT HANDLER
                    ClientHandler recipient = null;
                    for (ClientHandler client : Server.clientList)
                    {
                        if (client.name.equals(recipientName) && client.online == true)
                        {
                            recipient = client;
                        }
                    }

                    // IF RECIPIENT EXISTS
                    if (recipient != null)
                    {

                    // PROTOCOLS    
                        //MESSAGE FORMAT: PROTOCOL#SENDER
                        // ASK RECIPIENT IF HE WANTS TO CONNECT
                        if (message.equals("connectionRequest"))
                        {
                            // WE ARE TELLING RECIPIENT WHICH CLIENT WANTS TO CONNECT WITH HIM                        
                            recipient.outputStream.writeUTF("connectionRequest#" + name);

                        }
                        else if (message.equals("requestDeclined"))
                        {
                            // WE ARE TELLING RECIPIENT WHICH CLIENT DECLINED TO CONNECT WITH HIM
                            recipient.outputStream.writeUTF("requestDeclined#" + name);

                        }
                        else if (message.equals("requestAccepted"))
                        {
                            // WE ARE TELLING RECIPIENT WHICH CLIENT DECLINED TO CONNECT WITH HIM
                            recipient.outputStream.writeUTF("requestAccepted#" + name);
                            recipient.chattingPartnerName = name;
                            chattingPartnerName = recipientName;
                        }
                        else if (message.equals("messageDelivered"))
                        {
                            // WE ARE TELLING RECIPIENT WHICH CLIENT DECLINED TO CONNECT WITH HIM

                            recipient.outputStream.writeUTF("messageDelivered#" + name);

                        }
                        else if (message.equals("chatTerminated"))
                        {
                            // WE ARE TELLING RECIPIENT WHICH CLIENT DECLINED TO CONNECT WITH HIM
                            chattingPartnerName = "";
                            recipient.chattingPartnerName = "";
                            recipient.outputStream.writeUTF("chatTerminated#" + name);

                        }

                        else if (dataType.equals("fileTransferRequest"))
                        {
                            System.out.println("sent from server to client 2");
                            recipient.outputStream.writeUTF("fileTransferRequest#" + message + "#" + name);
                        }
                        else if (message.equals("fileTransferAccepted"))
                        {
                            recipient.outputStream.writeUTF("fileTransferAccepted#" + name);

                        }
                        else if (message.equals("fileTransferDeclined"))
                        {
                            recipient.outputStream.writeUTF("fileTransferDeclined#" + name);
                        }
                        else if (dataType.equals("fileNameForTransfer"))
                        {
                            System.out.println("server got file name:" + message);
                            filename = message;
                        }
                        else if (dataType.equals("fileSize"))
                        {
                            System.out.println("server got file size:" + message);
                            fileSize = Long.parseLong(message);
                            receiveFile(recipient);
                        }

                        else if (dataType.equals("chatMessage"))
                        {
                            // IF CLIENT IS SENDING PLAIN TEXT MESSAGE TO RECIPIENT                                               
                            recipient.outputStream.writeUTF("chatMessage#" + message + "#" + name);
                        }
                    }
                }
            }
            catch (IOException e)
            {
                System.out.println(e);
            }

        }
        try
        {
            // CLOSE STREAMS
            this.inputStream.close();
            this.outputStream.close();

        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }

    public void receiveFile(ClientHandler recipient) throws UnknownHostException, IOException
    {
        byte[] contents = new byte[10000];
        System.out.println("in server receive file");
        int bytesRead = 0;
        //Initialize the FileOutputStream to the output file's full path.  
        File writtenFile = new File(desktop + "\\Server\\" + name + "\\" + filename);
        FileOutputStream fos = new FileOutputStream(writtenFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        
        while (fileSize > 0 && (bytesRead = inputStream.read(contents, 0, (int) Math.min(contents.length, fileSize))) != -1)
        {
            bos.write(contents, 0, bytesRead);
            fileSize -= bytesRead;
        }       

        bos.flush();
        System.out.println("adding new cuzzy for some reason before sending file");
        System.out.println("File saved successfully!");
        sendFile(writtenFile, recipient, chattingPartnerName);
    }

    public void sendFile(File file, ClientHandler recipient, String chattingPartnerName) throws IOException
    {

        recipient.outputStream.writeUTF("fileNameFromServer#" + file.getName() + "#" + name);
        //Specify the file
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fileInputStream);

        //Get socket's output stream
        OutputStream outStream = (OutputStream) outputStream;

        //Read File Contents into contents array 
        byte[] contents;
        long fileLength = file.length();
        long current = 0;

        long start = System.nanoTime();
        while (current != fileLength)
        {
            int size = 10000;
            if (fileLength - current >= size)
            {
                current += size;
            }
            else
            {
                size = (int) (fileLength - current);
                current = fileLength;
            }
            contents = new byte[size];
            bis.read(contents, 0, size);
            outStream.write(contents);
            System.out.print("Sending file ... " + (current * 100) / fileLength + "% complete!\n");
        }

        outStream.flush();
        //File transfer done. Close the socket connection!
        System.out.println("File sent succesfully!");
    }

}
