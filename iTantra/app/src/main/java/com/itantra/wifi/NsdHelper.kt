package com.itantra.wifi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.ServerSocket

class NsdHelper(context: Context, private val onServiceUpdate: (List<NsdServiceInfo>) -> Unit) {

    private val TAG = "NsdHelper"
    private val SERVICE_TYPE = "_itantra._tcp"
    private val SERVICE_NAME_DEFAULT = "iTantraNode"

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var localPort: Int = -1
    private var serviceName: String = SERVICE_NAME_DEFAULT

    private val discoveredServices = mutableMapOf<String, NsdServiceInfo>()

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            serviceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service registered: $serviceName")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Registration failed: $errorCode")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            Log.d(TAG, "Service unregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Unregistration failed: $errorCode")
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service found: ${service.serviceName}")
            if (service.serviceType == SERVICE_TYPE) {
                if (service.serviceName != serviceName) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.d(TAG, "Resolve Succeeded: $serviceInfo")
                            discoveredServices[serviceInfo.serviceName] = serviceInfo
                            onServiceUpdate(discoveredServices.values.toList())
                        }
                    })
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "Service lost: ${service.serviceName}")
            discoveredServices.remove(service.serviceName)
            onServiceUpdate(discoveredServices.values.toList())
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun registerService() {
        // Initialize a server socket on the next available port.
        val socket = ServerSocket(0).also { 
            localPort = it.localPort
            it.close() // Close it so it can be reused by the actual transceiver later
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME_DEFAULT
            serviceType = SERVICE_TYPE
            setPort(localPort)
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    fun unregisterService() {
        try {
            nsdManager.unregisterService(registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }
    }

    fun tearDown() {
        stopDiscovery()
        unregisterService()
        discoveredServices.clear()
        onServiceUpdate(emptyList())
    }
}
