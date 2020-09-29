package com.example.client_servertcp_pingpong;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ClienteTCP extends AppCompatActivity {
    TextView tvStatus, tvNumPìngsPongs,textViewLocalidadeCliente,textViewLogradouroCliente,textViewDDDCliente,textViewUFCliente,textViewBairroCliente,textViewPontuacaoCliente,textViewPontuacaoDoOponenteCliente,textViewCEPCliente;
    Socket clientSocket;
    DataOutputStream socketOutput;
    BufferedReader socketEntrada;
    DataInputStream socketInput;
    Button btConectar,button2;
    EditText edtIp,edtCEP2;
    long pings,pongs;
    String target_cep,maior_menor,logradouro_target,localidade_target,bairro_target,uf_target,ddd_target,cep_hint;
    int pontuacao=1000;
    int pontuacaoDoOponente=1000;
    boolean cep_enviado=false;
    boolean isOpponentDone=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente_t_c_p);
        textViewLocalidadeCliente=findViewById(R.id.textViewLocalidadeCliente);
        textViewLogradouroCliente=findViewById(R.id.textViewLogradouroCliente);
        textViewDDDCliente=findViewById(R.id.textViewDDDCliente);
        textViewUFCliente=findViewById(R.id.textViewUFCliente);
        textViewBairroCliente=findViewById(R.id.textViewBairroCliente);
        textViewPontuacaoCliente=findViewById(R.id.textViewPontuacaoCliente);
        textViewPontuacaoDoOponenteCliente=findViewById(R.id.textViewPontuacaoDoOponenteCliente);
        textViewCEPCliente=findViewById(R.id.textViewCEPCliente);

        tvStatus=findViewById(R.id.tvStatusClient);
        cep_hint="?";

        btConectar=findViewById(R.id.btConectar);
        button2=findViewById(R.id.button2);
        edtIp=findViewById(R.id.edtIP);
        edtCEP2=findViewById(R.id.edtCEP2);

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
    public void conectar_old(View v) {
        final String ip=edtIp.getText().toString();
        tvStatus.setText("Conectando em "+ip+":9090");


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket (ip,9090);

                    tvStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Conectado com "+ip+":9090");
                        }
                    });
                    socketOutput =
                            new DataOutputStream(clientSocket.getOutputStream());
                    socketInput=
                            new DataInputStream (clientSocket.getInputStream());
                    while (socketInput!=null) {
                        String result = socketInput.readUTF();
                        if (result.compareTo("PING") == 0) {
                            //enviar Pong
                            pongs++;
                            socketOutput.writeUTF("PONG");
                            socketOutput.flush();
                            atualizarStatus();
                        }
                    }


                } catch (Exception e) {

                    tvStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Erro na conexão com "+ip+":9090");
                        }
                    });

                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void conectar(View v) {
        final String ip=edtIp.getText().toString();
        tvStatus.setText("Conectando em "+ip+":9090");


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket (ip,9090);

                    tvStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Conectado com "+ip+":9090");
                        }
                    });
                    socketOutput =
                            new DataOutputStream(clientSocket.getOutputStream());
                    socketInput=
                            new DataInputStream (clientSocket.getInputStream());
                    while (socketInput!=null) {
                        String result = socketInput.readUTF();
                        /*if (result.compareTo("PING") == 0) {
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

                                Intent intent = new Intent(ClienteTCP.this, Victory.class);
                                intent.putExtra("pontuacao", pontuacao);
                                intent.putExtra("pontuacaoDoOponente",pontuacaoDoOponente);

                                startActivity(intent);
                            }
                        }
                        else onOpponentScoreChanged(result);
                    }


                } catch (Exception e) {

                    tvStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Erro na conexão com "+ip+":9090");
                        }
                    });

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
                        socketOutput.writeUTF(String.valueOf(edtCEP2.getText()));
                        socketOutput.flush();
                        pings++;
                    }else{
                        tvStatus.setText("Servidor Desconectado");
                        btConectar.setEnabled(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();



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
                        btConectar.setEnabled(true);
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
                        tvStatus.setText("Servidor Desconectado");
                        btConectar.setEnabled(true);
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

            URL url = new URL ("https://viacep.com.br/ws/"+String.valueOf(edtCEP2.getText())+"/json/");
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
                    cep = String.valueOf(edtCEP2.getText());
                }




                Log.v ("PDM","Esse é o CEP da rua "+logradouro+" da cidade "+localidade);

                if(target_cep.equals(cep))
                {
                    Looper.prepare();

                    if(!isOpponentDone) {
                        maior_menor = "=";

                        Toast.makeText(ClienteTCP.this,"Login e Senha Incorretos", Toast.LENGTH_LONG).show();
                        mandarDone();
                    }
                    else {
                        mandarDone();

                        maior_menor = "=";

                        Intent intent = new Intent(ClienteTCP.this, Victory.class);
                        intent.putExtra("pontuacao", pontuacao);
                        intent.putExtra("pontuacaoDoOponente",pontuacaoDoOponente);

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
                    textViewPontuacaoCliente.post(new Runnable() {
                        @Override
                        public void run() {
                            textViewPontuacaoCliente.setText("Pontuação: "+String.valueOf(pontuacao));
                        }
                    });
                    mandarPontuacao();
                }

                textViewLogradouroCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLogradouroCliente.setText("Logradouro: "+logradouro_target+" / "+logradouro);
                    }
                });

                textViewLocalidadeCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLocalidadeCliente.setText("Localidade: "+localidade_target+" / "+localidade);
                    }
                });

                textViewBairroCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewBairroCliente.setText("Bairro: "+bairro_target+" / "+bairro);
                    }
                });

                textViewUFCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewUFCliente.setText("UF: "+uf_target+" / "+uf);
                    }
                });

                textViewDDDCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDDDCliente.setText("DDD: "+ddd_target+" / "+ddd);
                    }
                });

                textViewCEPCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(maior_menor.equals("="))textViewCEPCliente.setText("Você encontrou o CEP! Aguarde seu oponente.");

                            else textViewCEPCliente.setText("CEP: ???"+cep_hint+" / "+cep+" (CEP alvo "+maior_menor+" CEP atual)");}
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
            textViewPontuacaoDoOponenteCliente.post(new Runnable() {
                @Override
                public void run() {
                    textViewPontuacaoDoOponenteCliente.setText("Pontuação do Oponente: "+s);
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

                textViewLogradouroCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLogradouroCliente.setText("Logradouro: "+logradouro_target);
                    }
                });

                textViewLocalidadeCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLocalidadeCliente.setText("Localidade: "+localidade_target);
                    }
                });

                textViewBairroCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewBairroCliente.setText("Bairro: "+bairro_target);
                    }
                });

                textViewUFCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewUFCliente.setText("UF: "+uf_target);
                    }
                });

                textViewDDDCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDDDCliente.setText("DDD: "+ddd_target);
                    }
                });

                textViewCEPCliente.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewCEPCliente.setText("CEP: ???"+cep_hint);
                    }
                });


            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
