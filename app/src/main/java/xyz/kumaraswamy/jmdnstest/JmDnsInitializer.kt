package xyz.kumaraswamy.jmdnstest

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.jmdns.JmDNS


class JmDnsInitializer : Initializer<JmDNS> {

  companion object {
    private const val TAG = "JmDnsInitializer"
  }

  override fun create(context: Context): JmDNS {
    Log.d(TAG, "create()")

    return JmDnsCreator.init(context)
  }

  override fun dependencies(): List<Class<out Initializer<*>?>> {
    return emptyList()
  }
}