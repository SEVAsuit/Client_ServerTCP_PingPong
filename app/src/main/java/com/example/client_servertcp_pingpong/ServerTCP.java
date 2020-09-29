package com.example.client_servertcp_pingpong;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ServerTCP extends AppCompatActivity {

    TextView tvStatus, tvNumPìngsPongs,textViewBairro,textViewLocalidade,textViewLogradouro,textViewCEP,textViewDDD,textViewUF,textViewPontuacao,textViewPontuacaoDoOponente;
    ServerSocket welcomeSocket;
    DataOutputStream socketOutput;
    BufferedReader socketEntrada;
    DataInputStream fromClient;
    boolean continuarRodando = false;
    boolean cep_enviado=false;
    boolean isOpponentDone=false;
    Button btLigarServer;
    EditText edtCEP;
    String logradouro_target;
    String localidade_target;
    String uf_target;
    String ddd_target;
    String bairro_target,target_cep,cep_hint,maior_menor;
    int pontuacao=1000;
    int pontuacaoDoOponente=1000;
    long pings,pongs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        textViewBairro=findViewById(R.id.textViewBairro);
        textViewCEP=findViewById(R.id.textViewCEP);
        textViewUF=findViewById(R.id.textViewUF);
        textViewDDD=findViewById(R.id.textViewDDD);
        textViewLocalidade=findViewById(R.id.textViewLocalidade);
        textViewLogradouro=findViewById(R.id.textViewLogradouro);
        textViewPontuacao=findViewById(R.id.textViewPontuacao);
        textViewPontuacaoDoOponente=findViewById(R.id.textViewPontuacaoDoOponente);
        tvStatus=findViewById(R.id.textView);
        btLigarServer=findViewById(R.id.btLigarServer);
        edtCEP=findViewById(R.id.edtCEP);
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

    public void mandarCEP(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socketOutput!=null) {
                        socketOutput.writeUTF(String.valueOf(edtCEP.getText()));
                        socketOutput.flush();
                        pings++;
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
               /* if (result.compareTo("PING") == 0) {
                    //enviar Pong
                    pongs++;
                    socketOutput.writeUTF("PONG");
                    socketOutput.flush();
                    atualizarStatus();
                }*/

                if(result.length()>4)transformarCEP(result);
                else if(result.equals("done"))
                {
                    isOpponentDone=true;
                    if(maior_menor.equals("="))
                    {
                        Looper.prepare();
                        maior_menor = "=";

                        Intent intent = new Intent(ServerTCP.this, Victory.class);
                        intent.putExtra("pontuacao", pontuacao);
                        intent.putExtra("pontuacaoDoOponente",pontuacaoDoOponente);
                        startActivity(intent);
                    }
                }
                else onOpponentScoreChanged(result);
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

    public void mandarDone(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socketOutput!=null) {
                        socketOutput.writeUTF("done");
                        socketOutput.flush();
                        pings++;
                    }else{
                        tvStatus.setText("Cliente Desconectado");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();



    }

    public void mandarPontuacao(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socketOutput!=null) {
                        socketOutput.writeUTF(String.valueOf(pontuacao));
                        socketOutput.flush();
                        pings++;
                    }else{
                        tvStatus.setText("Cliente Desconectado");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();



    }

    public void onCEPSent(View v) {
        Thread t = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        //executarACalculadora();
                        onCEPSentThread();
                    }
                }
        );
        t.start();
    }

    public void onCEPSentThread()
    {
        if(!cep_enviado)
        {
            mandarCEP();
            cep_enviado=true;
            return;
        }
        Log.v ("PDM","CEP SENT CHAMADO");

        try {
            Log.v ("PDM","ENTROU NO TRY");

            URL url = new URL ("https://viacep.com.br/ws/"+String.valueOf(edtCEP.getText())+"/json/");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true); //Vou ler dados?
            conn.connect();


            String result[] = new String[1];
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                result[0] = response.toString();
                //  Log.v ("PDM","Resultado:"+result[0]);

                JSONObject respostaJSON = new JSONObject(result[0]);

                final String logradouro,localidade,uf,ddd,bairro,cep;

                if(!respostaJSON.has("erro"))
                {
                    logradouro = respostaJSON.getString("logradouro");
                    localidade = respostaJSON.getString("localidade");
                    uf = respostaJSON.getString("uf");
                    ddd = respostaJSON.getString("ddd");
                    bairro = respostaJSON.getString("bairro");
                    cep = respostaJSON.getString("cep");
                }
                else
                {
                    logradouro = "Não encontrado";
                    localidade = "Não encontrada";
                    uf = "Não encontrada";
                    ddd = "Não encontrado";
                    bairro = "Não encontrado";
                    cep = String.valueOf(edtCEP.getText());
                }




                Log.v ("PDM","Esse é o CEP da rua "+logradouro+" da cidade "+localidade);

                if(target_cep.equals(cep))
                {
                    Looper.prepare();

                    if(!isOpponentDone) {
                        maior_menor = "=";

                        Toast.makeText(this, "Você encontrou o CEP! Aguarde que o seu oponente faça o mesmo.", Toast.LENGTH_LONG).show();
                        mandarDone();
                    }
                    else {
                        mandarDone();

                        maior_menor = "=";

                        Intent intent = new Intent(ServerTCP.this, Victory.class);
                        intent.putExtra("pontuacao", pontuacao);
                        intent.putExtra("pontuacaoDoOponente", pontuacaoDoOponente);

                        startActivity(intent);
                    }
                }
                else
                {
                    String s=String.valueOf(target_cep.charAt(0))+String.valueOf(target_cep.charAt(1))+String.valueOf(target_cep.charAt(2));
                    String s2=String.valueOf(cep.charAt(0))+String.valueOf(cep.charAt(1))+String.valueOf(cep.charAt(2));
                    Log.v("PDM",target_cep);

                    Log.v("PDM",s);
                    Log.v("PDM",cep);

                    Log.v("PDM",s2);


                    if(Integer.valueOf(s)>Integer.valueOf(s2))maior_menor=">";
                    else maior_menor="<";
                    pontuacao=pontuacao/2;
                    textViewPontuacao.post(new Runnable() {
                        @Override
                        public void run() {
                            textViewPontuacao.setText("Pontuação: "+String.valueOf(pontuacao));
                        }
                    });
                    mandarPontuacao();
                }

                textViewLogradouro.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLogradouro.setText("Logradouro: "+logradouro_target+" / "+logradouro);
                    }
                });

                textViewLocalidade.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLocalidade.setText("Localidade: "+localidade_target+" / "+localidade);
                    }
                });

                textViewBairro.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewBairro.setText("Bairro: "+bairro_target+" / "+bairro);
                    }
                });

                textViewUF.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewUF.setText("UF: "+uf_target+" / "+uf);
                    }
                });

                textViewDDD.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDDD.setText("DDD: "+ddd_target+" / "+ddd);
                    }
                });

                textViewCEP.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(maior_menor.equals("="))textViewCEP.setText("Você encontrou o CEP! Aguarde seu oponente.");

                            else textViewCEP.setText("CEP: ???"+cep_hint+" / "+cep+" (CEP alvo "+maior_menor+" CEP atual)");}
                        catch (Exception e)
                        {

                        }
                    }
                });


            }

        } catch (Exception e) {
            Log.v ("PDM","ENTROU NO CATCH");

            e.printStackTrace();
            Log.e("PDM", e.getMessage(), e);

        }


    }

    public void onOpponentScoreChanged(final String s)
    {
        Log.v("PMD","Pontuação do Oponente:"+s);
        try {
            pontuacaoDoOponente=Integer.valueOf(s);
            textViewPontuacaoDoOponente.post(new Runnable() {
                @Override
                public void run() {
                    textViewPontuacaoDoOponente.setText("Pontuação do Oponente: "+s);
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void transformarCEP(String CEP)
    {
        Log.v("PMD","CEP:"+CEP);
        try {
            URL url = new URL ("https://viacep.com.br/ws/"+CEP+"/json/");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true); //Vou ler dados?
            conn.connect();


            String result[] = new String[1];
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                result[0] = response.toString();
                //  Log.v ("PDM","Resultado:"+result[0]);

                JSONObject respostaJSON = new JSONObject(result[0]);

                logradouro_target = respostaJSON.getString("logradouro");
                localidade_target = respostaJSON.getString("localidade");
                uf_target= respostaJSON.getString("uf");
                ddd_target = respostaJSON.getString("ddd");
                bairro_target = respostaJSON.getString("bairro");
                final String cep = respostaJSON.getString("cep");
                target_cep=cep;
                cep_hint= String.valueOf(cep.charAt(3))+String.valueOf(cep.charAt(4))+String.valueOf(cep.charAt(5))+String.valueOf(cep.charAt(6))+String.valueOf(cep.charAt(7))+String.valueOf(cep.charAt(8));




                Log.v ("PDM","Esse é o CEP da rua "+logradouro_target+" da cidade "+localidade_target);

                textViewLogradouro.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLogradouro.setText("Logradouro: "+logradouro_target);
                    }
                });

                textViewLocalidade.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLocalidade.setText("Localidade: "+localidade_target);
                    }
                });

                textViewBairro.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewBairro.setText("Bairro: "+bairro_target);
                    }
                });

                textViewUF.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewUF.setText("UF: "+uf_target);
                    }
                });

                textViewDDD.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDDD.setText("DDD: "+ddd_target);
                    }
                });

                textViewCEP.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewCEP.setText("CEP: ???"+cep_hint);
                    }
                });


            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
