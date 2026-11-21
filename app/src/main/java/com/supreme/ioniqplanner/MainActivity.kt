package com.supreme.ioniqplanner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF070B12)) { PlannerScreen() }
            }
        }
    }
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PlannerScreen() {
    val ctx = LocalContext.current
    val prefs = remember { PlannerPrefs(ctx) }
    var arrival by remember { mutableStateOf(prefs.arrivalSoc.toString()) }
    var depart by remember { mutableStateOf(prefs.departSoc.toString()) }
    var minKw by remember { mutableStateOf(prefs.minKw.toString()) }
    var battery by remember { mutableStateOf(prefs.batteryKwh.toString()) }
    var stats by remember { mutableStateOf(prefs.loadStats()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var tripInfo by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            msg = try {
                val text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                val s = TripImporter.parse(text)
                prefs.saveStats(s); stats = s
                "Import OK: ${s.tripCount} trips, ${s.chargeCount} charges"
            } catch (e: Exception) { "Import failed: ${e.message}" }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Supreme Ioniq Planner", style = MaterialTheme.typography.headlineSmall)
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        NumField("Arrival SOC at charger (%)", arrival) { arrival = it }
        NumField("Departure SOC (%)", depart) { depart = it }
        NumField("Minimum charger power (kW)", minKw) { minKw = it }
        NumField("Usable battery (kWh)", battery) { battery = it }
        Button(onClick = {
            arrival.toFloatOrNull()?.let { prefs.arrivalSoc = it }
            depart.toFloatOrNull()?.let { prefs.departSoc = it }
            minKw.toFloatOrNull()?.let { prefs.minKw = it }
            battery.toFloatOrNull()?.let { prefs.batteryKwh = it }
            msg = "Settings saved"
        }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }

        HorizontalDivider()
        Text("Learned from your logs", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import trips.json")
        }
        val s = stats
        if (s == null) {
            Text("No data imported yet.")
        } else {
            fun f(v: Double?, unit: String) = if (v == null) "n/a" else String.format(Locale.US, "%.1f %s", v, unit)
            Text("Trips used: ${s.tripCount} (${f(s.totalKm, "km")})")
            Text("Avg consumption: ${f(s.avgWhKm?.div(10.0), "kWh/100 km")}")
            Text("Charges used: ${s.chargeCount}")
            Text("Avg charge power: ${f(s.avgChargeKw, "kW")}")
            Text("Max charge power: ${f(s.maxChargeKw, "kW")}")
        }

        HorizontalDivider()
        Text("Trip", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(from, { from = it }, label = { Text("From (address or city)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(to, { to = it }, label = { Text("To (address or city)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && from.isNotBlank() && to.isNotBlank(), onClick = {
            busy = true; tripInfo = "Calculating..."
            scope.launch {
                val a = Routing.geocode(from); val b = Routing.geocode(to)
                val r = if (a != null && b != null) Routing.route(a, b) else null
                if (r == null) {
                    tripInfo = "Route failed (address not found or no network)."
                } else {
                    val wh = stats?.avgWhKm ?: 180.0
                    val need = r.km * wh / 1000.0
                    val usable = (prefs.departSoc - prefs.arrivalSoc) / 100.0 * prefs.batteryKwh
                    val stops = if (usable > 0) maxOf(0, Math.ceil(need / usable).toInt() - 1) else 0
                    tripInfo = String.format(Locale.US,
                        "Distance: %.0f km\nDriving time: %.1f h\nConsumption used: %.1f kWh/100 km%s\nEnergy needed: %.0f kWh\nEstimated charging stops: %d",
                        r.km, r.hours, wh / 10.0, if (stats?.avgWhKm == null) " (default)" else "", need, stops)
                }
                busy = false
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate route") }
        if (tripInfo.isNotEmpty()) Text(tripInfo)
        if (msg.isNotEmpty()) Text(msg, color = Color(0xFF7CFC9A))
    }
}
