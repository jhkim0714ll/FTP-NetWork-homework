package kr.hs.dgsw.network.test01.n2207.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;

public class SocketClient{

    String storage = "/Users/kimjunho/Desktop";

    public static void main(String[] args) throws IOException {
        SocketClient cm = new SocketClient();
        cm.run();
    }

    void run() throws IOException {
        Socket socket = new Socket();
        SocketAddress address = new InetSocketAddress("127.0.0.1", 2000);
        socket.connect(address);

        login(socket);

        while (true) {
            fileCommand(socket);
        }
    }

    public String send(Socket socket) throws IOException{
        Scanner scanner = new Scanner(System.in);
        Message message = new Message(scanner.nextLine());

        String checkMessage = new String(message.msg).trim();
        if (checkMessage.equals("/접속종료")) {
            socket.close();
            System.exit(0);
        }

        byte[] data = message.msg;
        OutputStream os = socket.getOutputStream();
        os.write(data);
        os.flush();

        return checkMessage;
    }

    public String receive(Socket socket) throws IOException{

        int maxBuffer = 1024;
        byte[] recBuffer = new byte[maxBuffer];

        InputStream is = socket.getInputStream();
        int readByte = is.read(recBuffer);
        if(readByte > 0){
            return new String(recBuffer).trim();
        }
        return "메시지가 전달되지 않았습니다";
    }

    public void login(Socket socket) throws IOException {
        byte[] bytes = new byte[1024];

        while (true) {
            String id = receive(socket);
            System.out.println(id);
            send(socket);
            String pw = receive(socket);
            System.out.println(pw);
            send(socket);

            String checkLogin = receive(socket);
            System.out.println(checkLogin);
            if(checkLogin.equals("** 서버에 접속하였습니다 **")){
                break;
            }
        }
    }

    public void fileCommand(Socket socket) throws IOException {
        String sendMessage = send(socket);
        String[] sendCmd = sendMessage.split(" ");
        switch (sendCmd[0]){
            case "/파일목록":
                fileList(socket);
                break;
            case "/업로드":
                uploadFile(socket,sendCmd);
                break;
            case "/다운로드":
                downloadFile(socket,sendCmd[1]);
                break;
        }
    }

    public void fileList(Socket socket) throws IOException {
        String size = receive(socket);
        System.out.println("** [File List] **");
        String fileListMessage = receive(socket);
        System.out.println(fileListMessage);
        String[] fileCnt = size.split(" ");
        int fileInt = Integer.parseInt(fileCnt[0]);

        int cnt = 0;
        while (cnt < fileInt){
            String msg = receive(socket);
            System.out.println(msg);
            cnt++;
        }
    }

    public void uploadFile(Socket socket,String[] command) throws IOException {
        String answer = receive(socket);
        if(!answer.equals("업로드")) {

            System.out.println(answer);
            if (answer.equals("업로드가 취소 되었습니다")) {
                return;
            }
            send(socket);
        }
        sendFile(socket,command);
    }

    public void downloadFile(Socket socket,String command) throws IOException{
        receiveFile(socket,command);
    }

    public void sendFile(Socket socket,String[] command) throws IOException{
        String cmdName = command[1];

        if(command.length == 3){
            File file = new File(cmdName).getParentFile();
            cmdName = file + "/" + command[2];
        }

        OutputStream os = socket.getOutputStream();
        BufferedOutputStream bor = new BufferedOutputStream(os);
        DataOutputStream dos = new DataOutputStream(bor);

        byte[] bytes = new byte[1024];

        File fl = new File(cmdName);
        FileInputStream fis = new FileInputStream(fl);

        dos.writeUTF(fl.getName());
        dos.writeLong(fl.length());
        int readSize = 0;

        while(true){
            readSize = fis.read(bytes);
            if(readSize == -1){
                dos.flush();
                break;
            }
            dos.write(bytes, 0 , readSize);
        }
        fis.close();

        String uploadBool = receive(socket);
        System.out.println(uploadBool);
    }

    public void receiveFile(Socket socket, String command) throws IOException{
        String fileExist = receive(socket);
        if(fileExist.equals("해당 파일이 없습니다")){
            System.out.println(fileExist);
            return;
        }
        File commandFile = new File(command);

        InputStream is = socket.getInputStream();
        BufferedInputStream bir = new BufferedInputStream(is);
        DataInputStream dis = new DataInputStream(bir);

        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        FileOutputStream fos = new FileOutputStream(storage + "/" + fileName);

        byte[] bytes = new byte[1024];
        int readSize = 0;

        while (true) {
            readSize = dis.read(bytes);
            fos.write(bytes, 0, readSize);
            fileSize -= readSize;
            if (fileSize <= 0) {
                break;
            }
        }
        String successDownload = "** "
                + commandFile.getName()
                + "을 "
                + storage
                + "로 다운로드 하였습니다";
        System.out.println(successDownload);
        fos.flush();
    }
}

class Message implements Serializable {
    byte[] msg;

    Message(String msg) {
        this.msg = msg.getBytes();
    }
}