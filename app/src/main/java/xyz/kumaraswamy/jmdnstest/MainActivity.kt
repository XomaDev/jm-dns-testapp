package xyz.kumaraswamy.jmdnstest

import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import xyz.kumaraswamy.jmdnstest.JmDnsCreator.Companion.localPort
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
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

  private lateinit var input: InputStream
  private lateinit var output: OutputStream

  private lateinit var textView: TextView

  private var connected = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    textView = findViewById(R.id.textview)

    val wifi = getSystemService(WIFI_SERVICE) as WifiManager
    val lock = wifi.createMulticastLock("jmdns-multicast-lock")
    lock.setReferenceCounted(true)
    lock.acquire()


    thread {
      jmDns = JmDnsCreator.getInstance()
      // the service is already registered on the port 1234
      // we just need to discover devices

      // jus tries to accept any incoming request

      initializeServerSocket()
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
          if (resolved || connected)
            return
          val serviceInfo = jmDns.getServiceInfo(event.type, event.name)
          if (serviceInfo.name != Build.MODEL) {
            serviceInfo?.let {
              Log.d(TAG, "Service resolved: " + it.name)

              jmDns.removeServiceListener(SERVICE_TYPE, this)
              resolved = true

              val port = serviceInfo.port
              val host = serviceInfo.hostAddresses[0]

              val socket = Socket(InetAddress.getByName(host), port)
              socket.keepAlive = true

              input = socket.getInputStream()
              output = socket.getOutputStream()

              output.write(Build.MODEL.encodeToByteArray())

              thread {
                Thread.sleep(1000)
                val name = ByteArray(input.available())
                input.read(name)

                runOnUiThread {
                  textView.text = "Connected to ${String(name)}"
                }
                Log.d(TAG, "serviceResolved: bytes = " + String(name))
              }
              Log.d(TAG, "Connected and was resolved! $input")
            }
          }
        }
      })
    }
  }

  private fun initializeServerSocket() {
    // this basically allows any incoming request
    // to connect to the server

    Thread {
      try {
        val serverSocket = ServerSocket(localPort)
        val socket = serverSocket.accept()

        socket.keepAlive = true

        input = socket.getInputStream()
        output = socket.getOutputStream()

        connected = true
        output.write(Build.MODEL.encodeToByteArray())

        thread {
          Thread.sleep(1000)
          val name = ByteArray(input.available())
          input.read(name)
          Log.d(TAG, "serviceResolved: bytes1111 = " + String(name))

          runOnUiThread {
            textView.text = "Connected to ${String(name)}"
          }
        }

        Log.d(TAG, "###### Connection Was Accepted")
      } catch (e: IOException) {
        Log.d(TAG, "initializeServerSocket: Failed to Accept")
        throw RuntimeException(e)
      }
    }.start()
  }

  override fun onPause() {
    super.onPause()
    jmDns.close()
  }
}
