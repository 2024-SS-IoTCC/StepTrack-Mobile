package com.example.steptrack

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.util.UUID

class MqttManager {
    private var mqttClient: Mqtt3AsyncClient? = null

    fun connect() {
        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier(UUID.randomUUID().toString())
            .serverHost("164.90.179.217")
            .serverPort(1883)
            .buildAsync()

        mqttClient?.connectWith()
            ?.send()
            ?.whenComplete { ack, throwable ->
                if (throwable != null) {
                    // Handle connection failure
                    Log.i("MqttManager", "could not connect")
                } else {
                    // Successfully connected
                    Log.i("MqttManager", "connection established")
                }
            }
    }

    fun publishMessage(topic: String, message: String) {
        mqttClient?.publishWith()
            ?.topic(topic)
            ?.payload(message.toByteArray())
            ?.qos(MqttQos.EXACTLY_ONCE)
            ?.send()
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }

}