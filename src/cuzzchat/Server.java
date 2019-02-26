/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuzzchat;
import java.io.*; 
import java.util.*; 
import java.net.*; 

/**
 *
 * @author gabri
 */
public class Server {
    // Vector to store active clients 
    static Vector<ClientHandler> clientList = new Vector<>(); 
      
    // counter for clients 
    static int i = 1; 
  
    public static void main(String[] args) throws IOException  
    { 
        // server is listening on port 1234 
        ServerSocket serverSocket = new ServerSocket(1234); 
          
        Socket socket; 
          
        // running infinite loop for getting 
        // client request 
        while (true)  
        { 
            // Accept the incoming request 
            socket = serverSocket.accept(); 
  
            System.out.println("New Cuzzy request received : " + socket); 
              
            // obtain input and output streams 
            DataInputStream inputStream = new DataInputStream(socket.getInputStream()); 
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream()); 
              
            System.out.println("Creating a new handler for this Cuzzy..."); 
  
            // Create a new handler object for handling this request. 
            ClientHandler activeClient = new ClientHandler(socket,"Cuzzy " + i, inputStream, outputStream); 
  
            // Create a new Thread with this object. 
            Thread t = new Thread(activeClient); 
              
            System.out.println("Adding this client to active Cuzzy list"); 
            
            
            
            // give client username
            outputStream.writeUTF("username#Cuzzy "+i);
            
            // add this client to active clients list 
            clientList.add(activeClient); 
            
            for (ClientHandler client : clientList)  
                {                     
                    client.outputStream.writeUTF("addToActiveList#Cuzzy "+ i);                     
                }
            
            // telling the cuzzies to add the new cuzz to their list
            for (ClientHandler client : clientList)  
                {                     
                    activeClient.outputStream.writeUTF("addToActiveList#"+ client.getName());                     
                }
  
            // start the thread. 
            t.start(); 
  
            // increment i for new cuzz. 
            i++; 
  
        } 
    } 
}

class ClientHandler implements Runnable  
{ 
    Scanner scn = new Scanner(System.in); 
    private String name; 
    final DataInputStream inputStream; 
    final DataOutputStream outputStream; 
    Socket socket; 
    boolean online;       
     
    public ClientHandler(Socket socket, String name, 
                            DataInputStream inputStream, DataOutputStream outputStream) { 
        this.inputStream = inputStream; 
        this.outputStream = outputStream; 
        this.name = name; 
        this.socket = socket; 
        this.online=true; 
    } 
    
    public String getName(){
        return name;
    };
  
    @Override
    public void run() { 
  
        String received; 
        while (true)  
        { 
            try
            { 
                // receive the string 
                received = inputStream.readUTF(); 
                
                //this will go away
                if(received.equals("logout")){ 
                    this.online=false; 
                    this.socket.close(); 
                    break; 
                } 
                
                // break the string into message and recipient part 
                StringTokenizer st = new StringTokenizer(received, "#"); 
                String message = st.nextToken(); 
                String recipient = st.nextToken();   
                
                //ask client if he wants to connect
                if(message.equals("connectionRequest")){ 
                    for (ClientHandler client : Server.clientList)  
                { 
                    if (client.name.equals(recipient) && client.online==true)  
                    { 
                        client.outputStream.writeUTF("connectionRequest#"+name); 
                        break; 
                    } 
                } 
                }else{      
                // search for the recipient in the connected devices list. 
                // clientList is the vector storing client of active users 
                for (ClientHandler client : Server.clientList)  
                { 
                    // if the recipient is found, write on its 
                    // output stream 
                    if (client.name.equals(recipient) && client.online==true)  
                    { 
                        
                        client.outputStream.writeUTF(this.name+" : "+message); 
                        break; 
                    } 
                } 
                }
                
            } catch (IOException e) { 
                  
                e.printStackTrace(); 
            } 
              
        } 
        try
        { 
            // closing resources 
            this.inputStream.close(); 
            this.outputStream.close(); 
              
        }catch(IOException e){ 
            e.printStackTrace(); 
        } 
    } 
} 
