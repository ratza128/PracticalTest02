package systems.pdsd.practicaltest02;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Documented;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PracticalTest02MainActivity extends Activity {
    public EditText serverPort;
    public EditText clientAddress,clientPort,word;
    public Button serverButton,clientButton;
    public ServerThread serverThread;
    public TextView ServerReply;

    private GetExplanation getExplanation = new GetExplanation();
    private ConnectButtonClickListener connectButtonClickListener = new ConnectButtonClickListener();


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
                    BufferedReader bufferedReader = Utilities.getReader(socket);
                    PrintWriter    printWriter    = Utilities.getWriter(socket);
                    if (bufferedReader != null && printWriter != null){
                        Log.d("Debug","[COMUNICATION THREAD] WAITING FOR PARAMETERS");
                        String word = bufferedReader.readLine();
                        if (word != null && !word.isEmpty()){
                            HttpClient httpClient = new DefaultHttpClient();
                            HttpPost httpPost = new HttpPost("http://services.aonaware.com/DictService/DictService.asmx/Define?word="+word);
                            List<NameValuePair> params = new ArrayList<NameValuePair>();
                            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                            httpPost.setEntity(urlEncodedFormEntity);
                            ResponseHandler<String> responseHandler = new BasicResponseHandler();
                            String pageSourceCode = httpClient.execute(httpPost, responseHandler);
                            if (pageSourceCode != null) {

                               Log.d("Debug","Primesc raspuns diferit de null" + pageSourceCode);
                            }
                        }
                    }
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

        public void stopThread() {
            if (serverSocket != null) {
                interrupt();
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException ioException) {
                    Log.e("Error", "An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
                }
            }
        }

        public synchronized void setData(String explanation){
            ServerReply.setText(explanation);
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Log.d("Debug", "[SERVER] Waiting for a connection...");
                    Socket socket = serverSocket.accept();
                    Log.d("Debug", "[SERVER] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                    CommunicationThread communicationThread = new CommunicationThread(getApplicationContext(), socket);
                    communicationThread.start();
                }
            }  catch (IOException ioException) {
                Log.e("error", "An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }


    private class ConnectButtonClickListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            String server_port = serverPort.getText().toString();
            if (server_port == null || server_port.isEmpty()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Server port should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            serverThread = new ServerThread(Integer.parseInt(server_port));
            if (serverThread.getServerSocket() != null) {
                serverThread.start();
            } else {
                Log.e("error", "[MAIN ACTIVITY] Could not creat server thread!");
            }
        }
    }

    private class GetExplanation implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            String address = clientAddress.getText().toString();
            String port    = clientPort.getText().toString();
            if (address == null || address.isEmpty() ||
                    port == null || port.isEmpty()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Client connection parameters should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (serverThread == null || !serverThread.isAlive()) {
                Log.e("Error", "[MAIN ACTIVITY] There is no server to connect to!");
                return;
            }
            String wordd = word.getText().toString();
            if (wordd == null || wordd.isEmpty()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Parameters from client (city / information type) should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            ServerReply.setText("");
            ClientThread clientThread = new ClientThread(
                    address,
                    Integer.parseInt(port),
                    wordd);
            clientThread.start();
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

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                if (socket == null) {
                    Log.e("Error", "[CLIENT THREAD] Could not create socket!");
                    return;
                }
                BufferedReader bufferedReader = Utilities.getReader(socket);
                PrintWriter printWriter    = Utilities.getWriter(socket);
                if (bufferedReader != null && printWriter != null) {
                    printWriter.println(word);
                    printWriter.flush();
                    String explanation;
                    while ((explanation = bufferedReader.readLine()) != null) {
                        final String finalizedInformation = explanation;
                        ServerReply.post(new Runnable() {
                            @Override
                            public void run() {
                                ServerReply.append(finalizedInformation + "\n");
                            }
                        });
                    }
                } else {
                    Log.e("Error", "[CLIENT THREAD] BufferedReader / PrintWriter are null!");
                }
                socket.close();
            } catch (IOException ioException) {
                Log.e("Error", "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();

            }
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
        ServerReply = (TextView) findViewById(R.id.explanation_textview);

        serverButton = (Button) findViewById(R.id.connect_button);
        clientButton = (Button) findViewById(R.id.get_explanation_button);


        serverButton.setOnClickListener(connectButtonClickListener);
        clientButton.setOnClickListener(getExplanation);



    }
}
