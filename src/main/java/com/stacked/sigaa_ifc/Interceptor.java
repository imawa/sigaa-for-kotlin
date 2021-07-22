package com.stacked.sigaa_ifc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import okhttp3.Request;
import okhttp3.Response;

import static com.stacked.sigaa_ifc.Sessao.TAG;


public class Interceptor implements okhttp3.Interceptor {
    private Context context;
    private Sessao sessao;

    public Interceptor(Context context, Sessao sessao) {
        this.context = context.getApplicationContext();
        this.sessao = sessao;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        if(!isConnectionOn() || !isInternetAvailable()) throw new ExcecaoSemInternet();

        Response response = chain.proceed(request);

        if(sessao.respostaValida(response)) {
            //Conferir sessão expirada
            if(response.priorResponse() != null && response.priorResponse().headers().get("Location").contains("expirada.jsp")) {
                Log.d(TAG, "intercept: sessão expirada");
                //Relogar
                try {
                    if(sessao.login()) {
                        Log.d(TAG, "intercept: sessão relogada");

                        //Repetir o request anterior com o novo JSESSIONID
                        Request requestNova;
                        if(request.body() != null) {
                            requestNova = new Request.Builder()
                                    .url(request.url())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Cookie", "JSESSIONID=" + sessao.getJSESSIONID())
                                    .post(request.body())
                                    .build();
                        } else {
                            requestNova = new Request.Builder()
                                    .url(request.url())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Cookie", "JSESSIONID=" + sessao.getJSESSIONID())
                                    .build();
                        }
                        response.close();
                        response = chain.proceed(requestNova);
                    }
                } catch (ExcecaoAPI excecaoAPI) {
                    excecaoAPI.printStackTrace();
                } catch (ExcecaoSessaoExpirada excecaoSessaoExpirada) {
                    excecaoSessaoExpirada.printStackTrace();
                } catch (ExcecaoSIGAA excecaoSIGAA) {
                    excecaoSIGAA.printStackTrace();
                }
                
            }
        } else {
            //Manunteção ou resposta inválida -> deslogar
            sessao.deslogar();
        }

        if(!response.isSuccessful()) throw new ExcecaoSemInternet();

        return response;
    }

    /*
    Utilizado para conferir se a internet está ligada (ativada)
     */
    private boolean isConnectionOn() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = cm.getActiveNetwork();
        NetworkCapabilities connection = cm.getNetworkCapabilities(network);

        return (connection != null) && (connection.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || connection.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    /*
    Utilizado para conferir se é possível conectar com a internet (pode estar ativada, mas não conseguir conectar)
     */
    private boolean isInternetAvailable() {
        try {
            int timeout = 1500;
            Socket sock = new Socket();
            InetSocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

            sock.connect(sockaddr, timeout);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
