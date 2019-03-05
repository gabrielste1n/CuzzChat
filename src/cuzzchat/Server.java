package cuzzchat;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 *
 * @author gabri
 */
public class Server
{

    // VECTOR TO STORE ACTIVE CLIENTS
    static Vector<ClientHandler> clientList = new Vector<>();

    // COUNTER FOR CLIENTS
    static int i = 1;

    public static void main(String[] args) throws IOException
    {
        System.out.println("Server is runnning...");
        // SERVER IS LISTENING ON PORT 1234 
        ServerSocket serverSocket = new ServerSocket(1234);

        Socket socket;

        // RUNNING INFINITE LOOP TO LISTEN FOR CLIENT REQUESTS
        while (true)
        {
            // ACCEPT THE INCOMING REQUEST
            socket = serverSocket.accept();
            System.out.println("New Cuzzy request received : " + socket);

            // OBTAIN INPUT AND OUTPUT STREAMS 
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            File cuzzyFolder = (new File("C:\\Users\\gabri\\Desktop\\" + "Cuzzy " + i));
            cuzzyFolder.mkdir();

            // CREATE A NEW CLIENT HANDLER OBJECT TO MANAGE THE CLIENT
            System.out.println("Creating a new handler for this Cuzzy...");
            ClientHandler activeClient = new ClientHandler(socket, "Cuzzy " + i, "", inputStream, outputStream);

            // CREATE A NEW THREAD FOR THIS OBJECT 
            Thread thread = new Thread(activeClient);

            // SET CLIENT USERNAME
            outputStream.writeUTF("username#Cuzzy " + i);

            // ADD CLIENT TO ACTIVE CLIENT LIST
            System.out.println("Adding this client to active Cuzzy list");
            clientList.add(activeClient);

            // TELLING THE OTHER CLIENTS TO ADD NEW CLIENT TO THEIR
            // ACTIVE CLIENT LIST
            for (ClientHandler client : clientList)
            {
                client.outputStream.writeUTF("addToActiveList#Cuzzy " + i);
            }

            // START THE THREAD
            thread.start();

            // ADDING EVERYONE TO THIS CLIENTS ACTIVE CLIENT LIST
            for (ClientHandler client : clientList)
            {
                activeClient.outputStream.writeUTF("addToActiveList#" + client.getName());
            }

            // INCREMENT COUNTER FOR NEXT CLIENT
            i++;

        }
    }
}

class ClientHandler implements Runnable
{

    private String name;
    String filename;
    private String chattingPartnerName;
    final DataInputStream inputStream;
    final DataOutputStream outputStream;
    Socket socket;
    boolean online;

    public ClientHandler(Socket socket, String name, String chattingPartnerName,
            DataInputStream inputStream, DataOutputStream outputStream)
    {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.chattingPartnerName = chattingPartnerName;
        this.name = name;
        this.socket = socket;
        this.online = true;
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
                        chattingPartnerName ="";
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
                        receiveFile();
                    }

                    else if (dataType.equals("chatMessage"))
                    {
                        // IF CLIENT IS SENDING PLAIN TEXT MESSAGE TO RECIPIENT                                               
                        recipient.outputStream.writeUTF("chatMessage#" + message + "#" + name);
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
    
    public void receiveFile() throws UnknownHostException, IOException
    {        
        byte[] contents = new byte[10000];
        System.out.println("in server receive file");
        //Initialize the FileOutputStream to the output file's full path.        
        FileOutputStream fos = new FileOutputStream(new File("C:\\Users\\gabri\\Desktop\\" + chattingPartnerName+"\\"+filename));
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        
        //No of bytes read in one read() call
        int bytesRead = 0; 
        
        while((bytesRead=is.read(contents))!=-1)
            bos.write(contents, 0, bytesRead); 
        
        bos.flush(); 
        
        
        System.out.println("File saved successfully!");
    }

//    Thread receiveFile = new Thread(new Runnable()
//    {
//        @Override
//        public void run()
//        {
//            while (true)
//            {
//                try
//                {
//                    //Initialize socket        
//                    byte[] contents = new byte[10000];
//                    System.out.println("in server receive file");
//                    //Initialize the FileOutputStream to the output file's full path.
//                    FileOutputStream fos = new FileOutputStream("C:\\Users\\gabri\\Desktop");
//                    BufferedOutputStream bos = new BufferedOutputStream(fos);
//                    InputStream is = socket.getInputStream();
//
//                    //No of bytes read in one read() call
//                    int bytesRead = 0;
//
//                    while ((bytesRead = is.read(contents)) != -1)
//                    {
//                        bos.write(contents, 0, bytesRead);
//                    }
//
//                    bos.flush();
//                    socket.close();
//
//                    System.out.println("File saved successfully!");
//                }
//                catch (IOException ex)
//                {
//                    System.out.println("Couldn't receive file");
//                }
//            }
//        }
//
//    });

}
