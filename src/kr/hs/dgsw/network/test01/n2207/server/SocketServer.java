package kr.hs.dgsw.network.test01.n2207.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SocketServer {
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    File dir = new File("/Users/kimjunho/Documents/receive");
    String id = "ID를 입력해주세요";
    String pass = "PASSWORD를 입력해주세요";
    String loginFaild = "로그인 실패\n\n";
    String loginSuccess = "** 서버에 접속하였습니다 ** ";
    String wrongCommand = "명령어가 잘못 입력되었습니다";
    String clientStop = "서버 접속을 종료합니다";
    String storage = "/Users/kimjunho/Documents/receive";
    String dupFile = "파일이 이미 존재합니다. 덮어씌울까요? (Yes: 덮어씌우기 / No: 취소)";


    public static void main(String[] args){
        SocketServer socketServer = new SocketServer();
        socketServer.run();
    }

    public void run(){
        try {
            ServerSocket server = new ServerSocket(2000);

            while(true){
                Socket socket = server.accept();
                System.out.println(socket.getInetAddress() + "로 부터 연결요청이 들어옴");

                login(socket);
                boolean check = true;
                while(check){
                    check = checkCommand(socket);
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkCommand(Socket socket) throws IOException {
        byte[] bytes = new byte[1024];
        readData(bytes, socket);
        String command = new String(bytes).trim();

        System.out.println(command + "을 실행");
        String[] cmd = command.split(" ");
        if(cmd[0].charAt(0) == '/') {
            switch (cmd[0]) {
                case "/파일목록":
                    fileList(socket);
                    return true;
                case "/업로드":
                    uploadFile(socket, command);
                    return true;
                case "/다운로드":
                    downloadFile(socket, cmd[1]);
                    return true;
                case "/접속종료":
                    sendData(clientStop.getBytes(StandardCharsets.UTF_8),socket);
                    return false;
                default:
                    sendData(wrongCommand.getBytes(StandardCharsets.UTF_8), socket);
                    return true;
            }
        }
        return true;
    }

    public void sendData(byte[] bytes, Socket socket){
        try {
            OutputStream os = socket.getOutputStream();
            os.write(bytes);
            os.flush();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public int readData(byte[] bytes, Socket socket){
        try {
            int maxBuffer = 1024;
            byte[] recBuffer = new byte[maxBuffer];
            InputStream is = socket.getInputStream();
            int readBytes = is.read(bytes);
            return readBytes;
        } catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public void login(Socket socket) throws UnsupportedEncodingException {
        byte[] bytes = new byte[1024];

        while (true) {
            sendData(id.getBytes("UTF-8"), socket);

            readData(bytes, socket);
            String readId = new String(bytes).trim();

            sendData(pass.getBytes("UTF-8"), socket);
            bytes = new byte[1024];
            readData(bytes, socket);
            String readPass = new String(bytes).trim();

            if(checkLogin(readId, readPass)){
                sendData(loginSuccess.getBytes(StandardCharsets.UTF_8), socket);
                return;
            }else{
                sendData(loginFaild.getBytes(StandardCharsets.UTF_8), socket);
            }
        }
    }

    public boolean checkLogin(String id,String pass){
        return id.equals("admin") && pass.equals("1234");
    }

    public void fileList(Socket socket) throws UnsupportedEncodingException {
        File directory = new File(storage);

        String fileCnt = directory.listFiles().length + " ";
        sendData(fileCnt.getBytes("UTF-8"),socket);

        int fileInt = 0;
        for(File file : directory.listFiles()) {
            String size = getSize(file.length());

            String list = "** " + file.getName() + "  " + size + " **";

            sendData(list.getBytes("UTF-8"), socket);
            fileInt++;
        }
        String fileNum = "** " + fileInt + "개 파일 **";

        sendData(fileNum.getBytes("UTF-8"),socket);
    }

    public String getSize(long bytes) {
        long kilo = bytes / 1024;
        long mega = kilo / 1024;
        long giga = mega / 1024;

        if (giga > 0) {
            return giga + "GB";
        }
        else if (mega > 0) {
            return mega + "MB";
        }
        else if (kilo > 0) {
            return kilo + "KB";
        }
        else {
            return bytes + "B";
        }
    }

    public void uploadFile(Socket socket, String command) throws IOException {
        String cmd = command.replace("/업로드 ", "");
        String cmdName = cmd;

        if(cmd.split(" ").length == 2){
            File file = new File(cmdName).getParentFile();
            cmdName = file + "/" + cmd.split(" ")[1];
        }

        File file = new File(cmdName);
        if(!file.exists()){
            System.out.println("파일이 없습니다");
            return;
        }
        boolean dupfileBool = false;

        for(File currentFile : Objects.requireNonNull(dir.listFiles())){
            if(currentFile.getName().equals(file.getName())) {

                sendData(dupFile.getBytes(StandardCharsets.UTF_8), socket);
                dupfileBool = true;

                byte[] bytes = new byte[1024];
                readData(bytes, socket);
                String answer = new String(bytes).trim();
                if (answer.equalsIgnoreCase("no")) {
                    String dupFileFailed = "업로드가 취소 되었습니다";
                    sendData(dupFileFailed.getBytes("UTF-8"), socket);
                    return;
                }
                break;
            }
        }
        //테스트
        ///업로드 /Users/kimjunho/Documents/send/시간표.png
        if(!dupfileBool) {
            sendData("업로드".getBytes(StandardCharsets.UTF_8), socket);
        }

        InputStream is = socket.getInputStream();
        BufferedInputStream bir = new BufferedInputStream(is);
        DataInputStream dis = new DataInputStream(bir);

        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        FileOutputStream fos = new FileOutputStream(dir + "/" + file.getName());

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
        fos.flush();

        String uploadSuccess = "** " + file.getName() + "파일을 업로드하였습니다. **";
        sendData(uploadSuccess.getBytes("UTF-8"),socket);
    }

    public void downloadFile(Socket socket, String command) throws IOException {
        OutputStream os = socket.getOutputStream();
        BufferedOutputStream bor = new BufferedOutputStream(os);
        DataOutputStream dos = new DataOutputStream(bor);

        byte[] bytes = new byte[1024];

        File commandFile = null;
        for(File currentFile : Objects.requireNonNull(dir.listFiles())){
            if(currentFile.getName().equals(command)) {
                commandFile = new File(currentFile.getAbsolutePath());
            }
        }
        if(commandFile == null){
            String nonData = "해당 파일이 없습니다";
            sendData(nonData.getBytes(StandardCharsets.UTF_8),socket);
            return;
        }else{
            sendData("데이터 전송중".getBytes(StandardCharsets.UTF_8),socket);
        }


        File fl = new File(commandFile.getAbsolutePath());
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
    }
}