package com.example.client_servertcp_pingpong;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Victory extends AppCompatActivity {

    TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victory);
        tv=findViewById(R.id.textView5);
        //Dados da Intent Anterior
        Intent quemChamou=this.getIntent();
        if (quemChamou!=null) {
            Bundle params = quemChamou.getExtras();
            if (params != null) {
                //Recuperando o Usuario
                int i = (int) params.getSerializable("pontuacao");
                int i2=(int)params.getSerializable("pontuacaoDoOponente");

                Log.v("pdm", String.valueOf(i));

                if(i>i2)tv.setText("Você venceu! Pontuação: " + i+". Pontuação do Oponente: "+i2+".");
                else if(i<i2)tv.setText("Você perdeu! Pontuação: " + i+". Pontuação do Oponente: "+i2+".");
                else if(i==i2)tv.setText("Empate! Pontuação: " + i+". Pontuação do Oponente: "+i2+".");
            }
        }
    }
}