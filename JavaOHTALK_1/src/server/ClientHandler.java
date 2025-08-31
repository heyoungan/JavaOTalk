package server;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler:
 * - 각 클라이언트와 소켓 연결
 * - 메시지 수신 -> RequestHandler로 처리 -> 응답 전송
 * - userId 보관(로그인 후)
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private ServerMain server;
    private BufferedReader in;
    private BufferedWriter out;
    private int userId = -1; // 아직 로그인 안했다면 -1

    public ClientHandler(Socket socket, ServerMain server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));

            String line;
            while((line=in.readLine())!=null) {
                // RequestHandler에 처리 맡김
                String response = server.getRequestHandler().handleRequest(this, line);
                sendMessage(response);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            // 연결 종료 -> user offline
            if(userId>0) {
                server.setUserOffline(userId);
            }
            try{socket.close();}catch(Exception ignore){}
        }
    }

    /**
     * 서버 -> 클라이언트 메시지
     */
    public void sendMessage(String msg) {
        try {
            out.write(msg+"\n");
            out.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 로그인 여부
     */
    public boolean isAuthenticated() {
        return userId>0;
    }

    public void setUserId(int uid) {
        this.userId = uid;
    }
    public int getUserId() {
        return userId;
    }
}
