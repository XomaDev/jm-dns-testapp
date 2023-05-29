package xyz.kumaraswamy.jmdnstest

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo


class JmDnsCreator {
  companion object {
    private var jmDNS: JmDNS? = null

    fun init(context: Context): JmDNS {
      val threadpool = Executors.newCachedThreadPool()
      val futureTask: Future<JmDNS> = threadpool.submit(Callable {
        val jmDNS = JmDNS.create(
          InetAddress.getByAddress(
            getIpv4(context)
          )
        )
        this.jmDNS = jmDNS

        // this will speed up the process
        jmDNS.registerService(
          ServiceInfo.create(
            "_http._tcp.local.",
            Build.MODEL,
            1234,
            0,
            0,
            "path=index.html"
          )
        )
        return@Callable jmDNS
      })
      return futureTask.get()
    }

    fun getInstance(): JmDNS {
      jmDNS?.let { return it }
      throw Error("Jm Dns Not initialized")
    }

    private fun getIpv4(context: Context): ByteArray {
      val connector = context.getSystemService(ConnectivityManager::class.java)

      val linkProperties = connector.getLinkProperties(connector.activeNetwork) as LinkProperties
      for (linkAddress in linkProperties.linkAddresses) {
        val address = linkAddress.address
        val hostAddress = address.hostAddress

        // we have to look for Ip4 address here
        if (isValidIpv4(hostAddress))
          return address.address
      }
      throw Error("Could not find Ipv4 address")
    }

    private fun isValidIpv4(ip: String?): Boolean {
      return try {
        if (ip.isNullOrEmpty()) return false
        val parts = ip.split("\\.".toRegex())
          .dropLastWhile { it.isEmpty() }
          .toTypedArray()
        if (parts.size != 4) return false
        for (s in parts) {
          val i = s.toInt()
          if (i < 0 || i > 255) {
            return false
          }
        }
        !ip.endsWith(".")
      } catch (nfe: NumberFormatException) {
        false
      }
    }
  }
}