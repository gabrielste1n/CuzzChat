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
public class Client
{

    final static int SERVER_PORT = 1234;
    static DataInputStream inputStream;
    static DataOutputStream outputStream;
    String username;

    public Client(String args[]) throws IOException
    {
        Client.main(args);
    }

    public static void main(String args[]) throws UnknownHostException, IOException
    {
        // GET LOCALHOST IP 
        InetAddress IP = InetAddress.getByName("localhost");

        // ESTABLISH CONNECTION 
        Socket socket = new Socket(IP, SERVER_PORT);

        // OBTAIN INPUT AND OUTPUT STREAMS
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void setUsername(String username)
    {
        this.username = username;
    }
    
    public void sendMessage(String message)
    {
        try
        {
            // WRITE TO OUTPUT STREAM
            outputStream.writeUTF(message);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }    
    
    public String readMessage()
    {
        try
        {
            // READ THE RECIEVED MESSAGE
            return inputStream.readUTF();

        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        return "null";
    }
    
    public void sendFile(File file) throws IOException
    {
        Socket socket = new Socket(InetAddress.getByName("localhost"), SERVER_PORT);
        //Specify the file
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fileInputStream); 
          
        //Get socket's output stream
        OutputStream outStream = socket.getOutputStream();
                
        //Read File Contents into contents array 
        byte[] contents;
        long fileLength = file.length(); 
        long current = 0;
         
        long start = System.nanoTime();
        while(current!=fileLength){ 
            int size = 10000;
            if(fileLength - current >= size)
                current += size;    
            else{ 
                size = (int)(fileLength - current); 
                current = fileLength;
            } 
            contents = new byte[size]; 
            bis.read(contents, 0, size); 
            outStream.write(contents);
            System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!\n");
        }   
        
        outStream.flush(); 
        //File transfer done. Close the socket connection!
        socket.close();
        System.out.println("File sent succesfully!");
    }

    public void receiveFile() throws UnknownHostException, IOException
    {
        //Initialize socket
        Socket socket = new Socket(InetAddress.getByName("localhost"), SERVER_PORT);
        byte[] contents = new byte[10000];
        
        //Initialize the FileOutputStream to the output file's full path.
        FileOutputStream fos = new FileOutputStream("C:\\Users\\gabri\\Desktop");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        
        //No of bytes read in one read() call
        int bytesRead = 0; 
        
        while((bytesRead=is.read(contents))!=-1)
            bos.write(contents, 0, bytesRead); 
        
        bos.flush(); 
        socket.close(); 
        
        System.out.println("File saved successfully!");
    }

}
