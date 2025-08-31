package client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkClient:
 * - 서버와의 소켓 연결, 수신 스레드
 * - 서버로부터 받은 메시지를 MessageListener들에게 전달
 */
public class NetworkClient {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private List<MessageListener> listeners;

    /**
     * 생성자
     */
    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.listeners = new ArrayList<>();
    }

    /**
     * 서버 연결
     * @return true/false
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
            startReceiverThread();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 서버 수신 스레드
     */
    private void startReceiverThread() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while((line = in.readLine())!=null) {
                    notifyListeners(line);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * 메시지 전송
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
     * 소켓 닫기
     */
    public void close() {
        try {
            if(socket!=null && !socket.isClosed()) {
                socket.close();
            }
        } catch(Exception e){}
    }

    /**
     * MessageListener 등록
     */
    public void addMessageListener(MessageListener l) {
        listeners.add(l);
    }

    /**
     * 수신 -> 리스너 알림
     */
    private void notifyListeners(String msg) {
        for(MessageListener l : listeners) {
            l.onMessageReceived(msg);
        }
    }
}
