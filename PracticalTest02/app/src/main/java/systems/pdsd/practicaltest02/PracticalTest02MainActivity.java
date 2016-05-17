package systems.pdsd.practicaltest02;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PracticalTest02MainActivity extends ActionBarActivity {
    private EditText serverPort;
    private EditText clientAddress,clientPort,word;
    private Button serverButton,clientButton;

    private class CommunicationThread extends Thread {
        private Socket socket;
        Context ctx;

        public CommunicationThread(Context ctx, Socket socket) {
            if (socket != null) {
                this.socket = socket;
                this.ctx = ctx;
            }
        }

        public void run() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e("Error", "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ServerThread extends Thread {
        boolean isRunning = true;
        private ServerSocket serverSocket;

        public ServerThread(int port) {
            try {
                this.serverSocket = new ServerSocket(port);
                isRunning = true;
            }catch (IOException e){
                Log.e("Error", "Eroare serverthread constructor");
            }
        }

        private ServerSocket getServerSocket() {
            return this.serverSocket;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Log.d("Debug", "[SERVER] Waiting for a connection...");
                    Socket socket = serverSocket.accept();
                    Log.d("Debug", "[SERVER] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                    //CommunicationThread communicationThread = new CommunicationThread(getApplicationContext(), socket);
                   // communicationThread.start();
                }
            }  catch (IOException ioException) {
                Log.e("error", "An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread {
        private Socket socket;
        private String address,word;
        private int port;

        public ClientThread(String address,int port,String word){
            this.address = address;
            this.port = port;
            this.word = word;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);
        serverPort = (EditText) findViewById(R.id.server_port_edit_text);
        clientAddress = (EditText) findViewById(R.id.client_address_edit_text);
        clientPort = (EditText) findViewById(R.id.client_port_edit_text);
        word = (EditText) findViewById(R.id.word_edit_text);
        serverButton = (Button) findViewById(R.id.connect_button);
        clientButton = (Button) findViewById(R.id.get_explanation_button);
    }
}
