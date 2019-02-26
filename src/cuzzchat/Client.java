package cuzzchat;
import java.awt.Component;
import java.awt.Container;
import java.io.*; 
import java.net.*; 
import java.util.Scanner; 
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 *
 * @author gabri
 */
public class Client {
    final static int SERVER_PORT = 1234;    
    static DataInputStream inputStream;
    static DataOutputStream outputStream;
    String username;   
        
    public Client(String args[]) throws IOException{
        Client.main(args);
    }
   
    public static void main(String args[]) throws UnknownHostException, IOException {         
        // GET LOCALHOST IP 
        InetAddress ip = InetAddress.getByName("localhost"); 
          
        // ESTABLISH CONNECTION 
        Socket socket = new Socket(ip, SERVER_PORT); 
          
        // OBTAIN INPUT AND OUTPUT STREAMS
        inputStream = new DataInputStream(socket.getInputStream()); 
        outputStream = new DataOutputStream(socket.getOutputStream());    
    } 
    
    public void setUsername(String username){
        this.username = username;
    };
    
    public void sendMessage(String message){                      
        try { 
            // WRITE TO OUTPUT STREAM
            outputStream.writeUTF(message); 
            } catch (IOException e) { 
                System.out.println(e);
            } 
    };
    
    public String readMessage(){
        try { 
            // READ THE RECIEVED MESSAGE
            return inputStream.readUTF();  
            
            } catch (IOException e) {   
                System.out.println(e); 
            } 
        
        return "null";
    };
}
