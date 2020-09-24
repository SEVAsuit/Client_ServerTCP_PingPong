package com.example.client_servertcp_pingpong;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP extends AppCompatActivity {

    TextView tvStatus, tvNumPìngsPongs;
    ServerSocket welcomeSocket;
    DataOutputStream socketOutput;
    BufferedReader socketEntrada;
    DataInputStream fromClient;
    boolean continuarRodando = false;
    Button btLigarServer;

    long pings,pongs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        tvStatus=findViewById(R.id.textView);
        btLigarServer=findViewById(R.id.btLigarServer);
        tvNumPìngsPongs=findViewById(R.id.tvNumPingsPongs);
    }

    public void ligarServidor(View v){
        ConnectivityManager connManager;
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Network[] networks = connManager.getAllNetworks();


        for (Network minhaRede:networks){
            NetworkInfo netInfo= connManager.getNetworkInfo(minhaRede);
            if(netInfo.getState().equals(NetworkInfo.State.CONNECTED)){
                NetworkCapabilities propDaRede = connManager.getNetworkCapabilities(minhaRede);

                if (propDaRede.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){

                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

                    String macAddress = wifiManager.getConnectionInfo().getMacAddress();
                    Log.v ("PDM","Wifi - MAC:"+macAddress);

                    int ip= wifiManager.getConnectionInfo().getIpAddress();
                    String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >>24 & 0xff));

                    Log.v ("PDM","Wifi - IP:"+ipAddress);
                    tvStatus.setText("Ativo em:"+ipAddress);

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                         ligarServerCodigo();
                        }
                    });
                    t.start();
                }

            }
        }


    }

    public void mandarPing(View v){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socketOutput!=null) {
                        socketOutput.writeUTF("PING");
                        socketOutput.flush();
                        pings++;
                        atualizarStatus();
                    }else{
                        tvStatus.setText("Cliente Desconectado");
                        btLigarServer.setEnabled(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();



    }
    public void desconectar(){
        try {
            if(socketOutput!=null) {
                socketOutput.close();
            }
            //Habilitar o Botão de Ligar
            btLigarServer.post(new Runnable() {
                @Override
                public void run() {
                    btLigarServer.setEnabled(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void ligarServerCodigo() {
        //Desabilitar o Botão de Ligar
        btLigarServer.post(new Runnable() {
            @Override
            public void run() {
                btLigarServer.setEnabled(false);
            }
        });

        String result = "";
        try {
            Log.v("SMD", "Ligando o Server");
            welcomeSocket = new ServerSocket(9090);
            Socket connectionSocket = welcomeSocket.accept();
            Log.v("SMD", "Nova conexão");

            //Instanciando os canais de stream
            fromClient = new DataInputStream(connectionSocket.getInputStream());
            socketOutput = new DataOutputStream(connectionSocket.getOutputStream());
            continuarRodando = true;
            while (continuarRodando) {
                result = fromClient.readUTF();
                if (result.compareTo("PING") == 0) {
                    //enviar Pong
                    pongs++;
                    socketOutput.writeUTF("PONG");
                    socketOutput.flush();
                    atualizarStatus();
                }
            }

            Log.v("SMD", result);
            //Enviando dados para o servidor
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

   public void somarNumPongs(){
       pongs++;
       atualizarStatus();

   }

   public void atualizarStatus() {
        //Método que vai atualizar os pings e pongs, usando post para evitar problemas com as threads
       tvNumPìngsPongs.post(new Runnable() {
           @Override
           public void run() {
               tvNumPìngsPongs.setText("Enviados "+pings+" Pings e "+pongs+" Pongs");
           }
       });


   }

}
