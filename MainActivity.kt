package com.ramiro.f1argentina

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val Bg = Color(0xFF0B0B0D)
private val Card = Color(0xFF17171B)
private val Accent = Color(0xFFE10600)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFFAAAAAF)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { F1App(this) }
    }
}

data class Race(val season: Int, val round: Int, val name: String, val country: String, val dateTimeUtc: Instant,
                val firstPractice: Instant? = null, val secondPractice: Instant? = null, val thirdPractice: Instant? = null,
                val qualifying: Instant? = null, val sprint: Instant? = null, val sprintQualifying: Instant? = null)

private fun parseSession(o: JSONObject?, key: String): Instant? = try {
    o?.optJSONObject(key)?.let { Instant.parse("${it.getString("date")}T${it.getString("time")}") }
} catch (_: Exception) { null }

private suspend fun fetchSeason(year: Int): List<Race> = withContext(Dispatchers.IO) {
    val url = URL("https://api.jolpi.ca/ergast/f1/$year.json?limit=100")
    val c = url.openConnection() as HttpURLConnection
    c.connectTimeout = 10000; c.readTimeout = 15000
    c.setRequestProperty("User-Agent", "F1Argentina/1.0")
    val text = c.inputStream.bufferedReader().use { it.readText() }
    c.disconnect()
    val races = JSONObject(text).getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
    buildList {
        for (i in 0 until races.length()) {
            val r = races.getJSONObject(i)
            val main = Instant.parse("${r.getString("date")}T${r.getString("time")}")
            val circuit = r.getJSONObject("Circuit")
            val country = circuit.getJSONObject("Location").optString("country", "")
            add(Race(year, r.getString("round").toInt(), r.getString("raceName").replace("Grand Prix", "GP"), country, main,
                parseSession(r, "FirstPractice"), parseSession(r, "SecondPractice"), parseSession(r, "ThirdPractice"),
                parseSession(r, "Qualifying"), parseSession(r, "Sprint"), parseSession(r, "SprintQualifying")))
        }
    }
}

private suspend fun loadRaces(): List<Race> {
    val now = Instant.now()
    val y = java.time.ZonedDateTime.now().year
    return try {
        val current = fetchSeason(y)
        val next = if (current.none { it.dateTimeUtc.isAfter(now.plusSeconds(86400L * 30)) }) fetchSeason(y + 1) else emptyList()
        (current + next).filter { it.dateTimeUtc.isAfter(now.minusSeconds(86400L * 2)) }.sortedBy { it.dateTimeUtc }
    } catch (_: Exception) { emptyList() }
}

private fun fmt(instant: Instant, pattern: String = "EEE d MMM • HH:mm"): String =
    DateTimeFormatter.ofPattern(pattern, Locale("es", "AR")).withZone(ZoneId.of("America/Argentina/Buenos_Aires")).format(instant)

@Composable
fun F1App(context: Context) {
    var races by remember { mutableStateOf<List<Race>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var lastUpdate by remember { mutableStateOf("") }

    fun refresh() {
        loading = true
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            val data = loadRaces()
            races = data
            loading = false
            lastUpdate = "Actualizado ${fmt(Instant.now(), "d/MM • HH:mm")}"
            if (data.isNotEmpty()) AlarmScheduler.schedule(context, data)
        }
    }
    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Accent, onBackground = TextPrimary, onSurface = TextPrimary)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("F1 ARGENTINA", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("Calendario en hora argentina", color = TextSecondary, fontSize = 13.sp)
                    }
                    TextButton(onClick = { refresh() }) { Text("↻", color = TextPrimary, fontSize = 28.sp) }
                }
                if (lastUpdate.isNotEmpty()) Text(lastUpdate, color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
                else if (races.isEmpty()) ErrorCard()
                else {
                    val next = races.firstOrNull()
                    if (next != null) NextRaceCard(next)
                    Spacer(Modifier.height(18.dp))
                    Text("CALENDARIO", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(races) { RaceCard(it) }
                    }
                }
            }
        }
    }
}

@Composable private fun NextRaceCard(r: Race) {
    Card(colors = CardDefaults.cardColors(containerColor = Accent), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("PRÓXIMA CARRERA", color = Color.White.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(r.name, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(r.country, color = Color.White.copy(alpha = .85f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text(fmt(r.dateTimeUtc, "EEEE d 'de' MMMM • HH:mm"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Aviso automático: 24 horas antes", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
        }
    }
}

@Composable private fun RaceCard(r: Race) {
    Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${r.round}", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(38.dp))
                Column(Modifier.weight(1f)) { Text(r.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(r.country, color = TextSecondary, fontSize = 12.sp) }
                Text(fmt(r.dateTimeUtc, "d MMM"), color = TextSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = .08f))
            Session("Carrera", r.dateTimeUtc)
            r.sprint?.let { Session("Sprint", it) }
            r.sprintQualifying?.let { Session("Clasificación Sprint", it) }
            r.qualifying?.let { Session("Clasificación", it) }
            r.firstPractice?.let { Session("Práctica 1", it) }
            r.secondPractice?.let { Session("Práctica 2", it) }
            r.thirdPractice?.let { Session("Práctica 3", it) }
        }
    }
}

@Composable private fun Session(label: String, time: Instant) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(fmt(time, "EEE d/MM • HH:mm"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ErrorCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) { Text("No pude actualizar el calendario", color = TextPrimary, fontWeight = FontWeight.Bold); Text("Comprobá tu conexión y tocá ↻ para intentar nuevamente.", color = TextSecondary, fontSize = 13.sp) }
    }
}

object AlarmScheduler {
    fun schedule(context: Context, races: List<Race>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cache = org.json.JSONArray()
        races.forEach { r ->
            cache.put(org.json.JSONObject().apply { put("season", r.season); put("round", r.round); put("name", r.name); put("country", r.country); put("time", r.dateTimeUtc.toEpochMilli()) })
        }
        context.getSharedPreferences("f1", Context.MODE_PRIVATE).edit().putString("races", cache.toString()).apply()
        races.forEach { r ->
            val notifyAt = r.dateTimeUtc.toEpochMilli() - 24L * 60L * 60L * 1000L
            if (notifyAt <= System.currentTimeMillis()) return@forEach
            val intent = Intent(context, RaceAlarmReceiver::class.java).apply { putExtra("name", r.name); putExtra("round", r.round) }
            val pi = PendingIntent.getBroadcast(context, r.season * 100 + r.round, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pi) } catch (_: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pi) }
        }
    }
}

class RaceAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Gran Premio"
        val nm = "f1_race_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(android.app.NotificationChannel(nm, "F1", android.app.NotificationManager.IMPORTANCE_HIGH))
        val n = androidx.core.app.NotificationCompat.Builder(context, nm)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🏁 F1 mañana")
            .setContentText("Mañana es el $name. Revisá el horario de la carrera en F1 Argentina.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).build()
        if (android.os.Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            manager.notify(intent.getIntExtra("round", 1), n)
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("f1", Context.MODE_PRIVATE)
        val raw = prefs.getString("races", null) ?: return
        val arr = try { org.json.JSONArray(raw) } catch (_: Exception) { return }
        val races = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Race(o.getInt("season"), o.getInt("round"), o.getString("name"), o.getString("country"), Instant.ofEpochMilli(o.getLong("time"))))
            }
        }
        AlarmScheduler.schedule(context, races)
    }
}
