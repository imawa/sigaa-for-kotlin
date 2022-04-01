package com.imawa.sigaaforkotlin.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class Utils {
    companion object {
        fun isConnectionOn(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val connection = connectivityManager.getNetworkCapabilities(network)
            return connection?.hasTransport(TRANSPORT_WIFI) == true || connection?.hasTransport(
                TRANSPORT_CELLULAR
            ) == true
        }

        fun isInternetAvailable(): Boolean {
            return try {
                val socket = Socket()
                val inetSocketAddress = InetSocketAddress("8.8.8.8", 53)
                socket.connect(inetSocketAddress, 2 * 1000)
                socket.close()
                true
            } catch (e: IOException) {
                false
            }
        }

        fun isValidResponse(response: Response?): Boolean {
            if (response?.isSuccessful == true) {
                return if (response.priorResponse?.isRedirect == true) {
                    // Redirecionado -> conferir se não foi para a página de manutenção
                    response.priorResponse?.headers?.get("Location")
                        ?.contains("manutencao.html") == false
                } else {
                    true
                }
            }
            return false
        }
    }
}
