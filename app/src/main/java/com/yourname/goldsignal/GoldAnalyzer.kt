package com.yourname.goldsignal

import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class GoldAnalyzer {
    
    private var currentPrice = 1950.0
    private val emaShort = mutableListOf<Double>()
    private val emaLong = mutableListOf<Double>()
    
    suspend fun checkGoldSignal(): GoldSignal {
        simulatePriceChange()
        
        val signalType = calculateTechnicalSignals()
        
        return GoldSignal(
            hasSignal = signalType != "NONE",
            signalType = signalType,
            currentPrice = currentPrice,
            stopLoss = currentPrice * 0.99,
            takeProfit = currentPrice * 1.015,
            timestamp = getCurrentTime()
        )
    }
    
    private fun calculateTechnicalSignals(): String {
        if (emaShort.size < 2 || emaLong.size < 2) {
            return "NONE"
        }
        
        val currentEmaShort = emaShort.last()
        val currentEmaLong = emaLong.last()
        val previousEmaShort = emaShort[emaShort.size - 2]
        val previousEmaLong = emaLong[emaLong.size - 2]
        
        if (previousEmaShort <= previousEmaLong && currentEmaShort > currentEmaLong) {
            return "BUY"
        }
        
        if (previousEmaShort >= previousEmaLong && currentEmaShort < currentEmaLong) {
            return "SELL"
        }
        
        return "NONE"
    }
    
    private fun simulatePriceChange() {
        val change = Random.nextDouble(-5.0, 5.0)
        currentPrice += change
        
        updateEMAs(currentPrice)
    }
    
    private fun updateEMAs(price: Double) {
        if (emaShort.isEmpty()) {
            emaShort.add(price)
        } else {
            val shortEma = (price * 2.0 / (5 + 1)) + (emaShort.last() * (5 - 1) / (5 + 1))
            emaShort.add(shortEma)
        }
        
        if (emaLong.isEmpty()) {
            emaLong.add(price)
        } else {
            val longEma = (price * 2.0 / (13 + 1)) + (emaLong.last() * (13 - 1) / (13 + 1))
            emaLong.add(longEma)
        }
        
        if (emaShort.size > 50) emaShort.removeAt(0)
        if (emaLong.size > 50) emaLong.removeAt(0)
    }
    
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}

data class GoldSignal(
    val hasSignal: Boolean,
    val signalType: String,
    val currentPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val timestamp: String
)
