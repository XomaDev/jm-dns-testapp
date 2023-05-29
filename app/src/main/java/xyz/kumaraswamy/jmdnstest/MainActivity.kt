package xyz.kumaraswamy.jmdnstest

import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "MainActivity"
    private const val SERVICE_TYPE = "_http._tcp.local."
  }

  private lateinit var jmDns: JmDNS

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val wifi = getSystemService(WIFI_SERVICE) as WifiManager
    val lock = wifi.createMulticastLock("jmdns-multicast-lock")
    lock.setReferenceCounted(true)
    lock.acquire()


    thread {
      jmDns = JmDnsCreator.getInstance()

      jmDns.addServiceListener(SERVICE_TYPE, object : ServiceListener {
        var resolved = false

        override fun serviceAdded(event: ServiceEvent) {
          // Service added, resolve its details
          val serviceInfo: ServiceInfo? = jmDns.getServiceInfo(event.type, event.name)
          if (serviceInfo != null) {
            Log.d(TAG, "Service added: " + serviceInfo.name)
          }
        }

        override fun serviceRemoved(event: ServiceEvent) {
          // Service removed
          Log.d(TAG, "Service removed: " + event.name)
        }

        override fun serviceResolved(event: ServiceEvent) {
          // Service resolved, send data to the discovered service
          if (resolved)
            return
          val serviceInfo = jmDns.getServiceInfo(event.type, event.name)
          if (serviceInfo.name != Build.MODEL) {
            serviceInfo?.let {
              Log.d(TAG, "Service resolved: " + it.name)

              jmDns.removeServiceListener(SERVICE_TYPE, this)
              resolved = true
            }
          }
        }
      })


    }
  }

  override fun onPause() {
    super.onPause()
    jmDns.close()
  }
}
