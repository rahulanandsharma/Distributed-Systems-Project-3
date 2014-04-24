/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ds;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import static java.nio.file.AccessMode.READ;
import static java.nio.file.AccessMode.WRITE;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MyChunkDownloader implements Runnable{
    int server_no; String file_name; RegistryServer rs;String server_ip;
    String [] file_servers; int chunk_no;
    public MyChunkDownloader(int server_no, String file_name, int chunk_no,String server_ip) {
                this.server_no=server_no;this.file_name=file_name;this.server_ip=server_ip;this.chunk_no=chunk_no;
    }

    @Override
    public void run() {
        int had_exception=0;
        try{
            this.rs=(RegistryServer)Naming.lookup("//"+server_ip+"/MyRegistry_ay");
            file_servers=rs.GetFileServers();
            
        }catch (NotBoundException | MalformedURLException | RemoteException e){
            //e.printStackTrace();
            had_exception=1;
        }
        if(had_exception==0){
            try{
                System.err.println("I am thread for file "+file_name+". I will get chunk "+Integer.toString(chunk_no)+" from Server "+file_servers[server_no]);
                downloadChunkSafe(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    public void downloadChunkSafe(int depth) throws FileNotFoundException, IOException {
        if(depth==file_servers.length){
            throw new Error("Could not connect to any server");
        }
        try{
            ReadWriteInterface rwc=(ReadWriteInterface)Naming.lookup(file_servers[server_no]);
            RandomAccessFile raf = new RandomAccessFile("output/"+file_name, "rw");
            raf.seek(64*1024*chunk_no);
            byte [] bytes=rwc.FileRead64K(file_name, chunk_no);
            if(bytes.length==64*1024){
		raf.write(bytes);
            }
            else System.err.println("Non standard byte array received in file "+file_name+" for chunk number "+Integer.toString(chunk_no));
            raf.close();
        } catch( NotBoundException | MalformedURLException | RemoteException e){
            //e.printStackTrace();
            server_no+=1;
            server_no%=file_servers.length;
            System.err.println("\r\n Attempting from next server");
            downloadChunkSafe(depth+1);
        }
    }
    
}
