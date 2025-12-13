package com.qzone.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Coordinate system converter
 * Handles conversion between WGS-84 (GPS) and GCJ-02 (Chinese Mars Coordinate System)
 */
object CoordinateConverter {
    
    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0 // Major semi-axis
    private const val EE = 0.00669342162296594323 // Eccentricity squared
    
    /**
     * Check if coordinates are in China (roughly)
     */
    fun isInChina(latitude: Double, longitude: Double): Boolean {
        return longitude in 72.004..137.8347 && latitude in 0.8293..55.8271
    }
    
    /**
     * Convert WGS-84 (GPS coordinates) to GCJ-02 (Mars coordinates used in China)
     * @param wgsLat WGS-84 latitude
     * @param wgsLng WGS-84 longitude
     * @return Pair of (GCJ-02 latitude, GCJ-02 longitude)
     */
    fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        if (!isInChina(wgsLat, wgsLng)) {
            return Pair(wgsLat, wgsLng)
        }
        
        var dLat = transformLat(wgsLng - 105.0, wgsLat - 35.0)
        var dLng = transformLng(wgsLng - 105.0, wgsLat - 35.0)
        val radLat = wgsLat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        val gcjLat = wgsLat + dLat
        val gcjLng = wgsLng + dLng
        
        return Pair(gcjLat, gcjLng)
    }
    
    /**
     * Convert GCJ-02 to WGS-84 (approximate, for reference)
     */
    fun gcj02ToWgs84(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        if (!isInChina(gcjLat, gcjLng)) {
            return Pair(gcjLat, gcjLng)
        }
        
        val (tmpLat, tmpLng) = wgs84ToGcj02(gcjLat, gcjLng)
        val wgsLat = gcjLat * 2 - tmpLat
        val wgsLng = gcjLng * 2 - tmpLng
        
        return Pair(wgsLat, wgsLng)
    }
    
    private fun transformLat(lng: Double, lat: Double): Double {
        var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 
                  0.1 * lng * lat + 0.2 * sqrt(abs(lng))
        ret += (20.0 * sin(6.0 * lng * PI) + 20.0 * sin(2.0 * lng * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(lat * PI) + 40.0 * sin(lat / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(lat / 12.0 * PI) + 320.0 * sin(lat * PI / 30.0)) * 2.0 / 3.0
        return ret
    }
    
    private fun transformLng(lng: Double, lat: Double): Double {
        var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 
                  0.1 * lng * lat + 0.1 * sqrt(abs(lng))
        ret += (20.0 * sin(6.0 * lng * PI) + 20.0 * sin(2.0 * lng * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(lng * PI) + 40.0 * sin(lng / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(lng / 12.0 * PI) + 300.0 * sin(lng / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
    
    /**
     * Calculate distance between two coordinates in meters
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
