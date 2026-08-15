package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SyncState
import com.example.ui.theme.*
import com.example.ui.viewmodel.WatchStudioViewModel
import kotlinx.coroutines.launch

@Composable
fun WearableSyncScreen(
    viewModel: WatchStudioViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val devices by viewModel.syncManager.devices.collectAsState()
    val selectedDeviceId by viewModel.syncManager.selectedDeviceId.collectAsState()
    val syncProgress by viewModel.syncManager.syncProgress.collectAsState()
    val diagnostics by viewModel.syncManager.diagnostics.collectAsState()
    val activeWatchFace by viewModel.editingWatchFace.collectAsState()

    var autoRotateDayNight by remember { mutableStateOf(true) }
    var highFrequencySensors by remember { mutableStateOf(true) }
    var showPairingGuideDialog by remember { mutableStateOf(false) }
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }

    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ecosistema Galaxy Wearable",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Sincronización segura y diagnóstico One UI 6 Watch",
                    style = MaterialTheme.typography.bodySmall,
                    color = GalaxyCyan
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GalaxyCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Watch, contentDescription = null, tint = GalaxyCyan)
            }
        }

        // Active Device Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(if (activeDevice.isConnected) GalaxyEmerald else Color(0xFFFF5252)),
                width = 1.5.dp
            )
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (activeDevice.isConnected) GalaxyEmerald else Color(0xFFFF5252))
                            )
                            Text(
                                text = if (activeDevice.isConnected) "ENLACE BLE 5.3 CIFRADO (ACTIVO)" else "DESCONECTADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeDevice.isConnected) GalaxyEmerald else Color(0xFFFF5252),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = activeDevice.modelName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = activeDevice.edition,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = activeDevice.isConnected,
                        onCheckedChange = { viewModel.syncManager.toggleDeviceConnection(activeDevice.id) },
                        colors = SwitchDefaults.colors(checkedThumbColor = GalaxyEmerald)
                    )
                }

                Divider(color = DarkBorder)

                // Device Specs Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DeviceMetricPill(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Batería Reloj",
                        value = "${activeDevice.batteryPercent}%",
                        color = GalaxyCyan
                    )
                    DeviceMetricPill(
                        icon = Icons.Default.Storage,
                        label = "Almacenamiento",
                        value = "${activeDevice.freeStorageGb} GB Libres",
                        color = GalaxyAmber
                    )
                    DeviceMetricPill(
                        icon = Icons.Default.Layers,
                        label = "Sistema",
                        value = activeDevice.oneUiVersion,
                        color = GalaxyEmerald
                    )
                }

                // Instant Sync button for currently edited face
                if (activeWatchFace != null) {
                    Button(
                        onClick = { viewModel.applyAndSyncActiveWatchFace(activeWatchFace!!) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GalaxyUltraOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = activeDevice.isConnected
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sincronizar '${activeWatchFace!!.title}' ahora",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Sync Progress Feedback (if active)
        if (syncProgress.state != SyncState.IDLE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (syncProgress.state) {
                        SyncState.SUCCESS -> GalaxyEmerald.copy(alpha = 0.15f)
                        SyncState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.15f)
                        else -> GalaxyCyan.copy(alpha = 0.15f)
                    }
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncProgress.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${syncProgress.progressPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GalaxyCyan
                        )
                    }
                    LinearProgressIndicator(
                        progress = { syncProgress.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = when (syncProgress.state) {
                            SyncState.SUCCESS -> GalaxyEmerald
                            SyncState.ERROR -> Color(0xFFFF5252)
                            else -> GalaxyCyan
                        },
                        trackColor = DarkBorder
                    )
                }
            }
        }

        // Security & Advanced Diagnostics Card
        Text(
            text = "Seguridad y Diagnóstico de Enlace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(GalaxyCyan.copy(alpha = 0.5f)),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GalaxyCyan)
                        Text(
                            "Auditoría de Seguridad y Sensores",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Badge(containerColor = GalaxyEmerald.copy(alpha = 0.2f)) {
                        Text("SEGURO", color = GalaxyEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Divider(color = DarkBorder)

                // Security attributes
                SecurityItemRow(
                    icon = Icons.Default.Lock,
                    title = "Cifrado Bluetooth",
                    value = diagnostics.bleEncryption,
                    statusColor = GalaxyEmerald
                )
                SecurityItemRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Handshake Knox & Wear OS",
                    value = diagnostics.handshakeStatus,
                    statusColor = GalaxyCyan
                )
                SecurityItemRow(
                    icon = Icons.Default.Shield,
                    title = "Integridad de Paquetes WFF",
                    value = diagnostics.dataIntegrityProtocol,
                    statusColor = GalaxyEmerald
                )
                SecurityItemRow(
                    icon = Icons.Default.NetworkCheck,
                    title = "Señal RSSI / Latencia",
                    value = "${diagnostics.signalRssi} dBm (Excelente) • ${diagnostics.latencyMs} ms",
                    statusColor = GalaxyAmber
                )
                SecurityItemRow(
                    icon = Icons.Default.ElectricBolt,
                    title = "Consumo Energético Estimado",
                    value = diagnostics.batteryImpactEstimate,
                    statusColor = GalaxyEmerald
                )

                // Diagnostic runner feedback
                if (diagnosticMessage != null) {
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = diagnosticMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = GalaxyCyan,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Diagnostic Action Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.syncManager.runSecurityAndConnectionDiagnostics().collect { msg ->
                                diagnosticMessage = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !diagnostics.isDiagnosticRunning
                ) {
                    if (diagnostics.isDiagnosticRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GalaxyCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = GalaxyCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (diagnostics.isDiagnosticRunning) "Ejecutando Test de Enlace..." else "Ejecutar Diagnóstico de Seguridad y Enlace",
                        color = GalaxyCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Devices Selector
        Text(
            text = "Dispositivos Galaxy Vinculados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        devices.forEach { device ->
            val isSelected = device.id == selectedDeviceId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.syncManager.selectDevice(device.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) GalaxyCyan.copy(alpha = 0.12f) else DarkSurface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GalaxyCyan),
                    width = 1.dp
                ) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = if (device.isConnected) GalaxyEmerald else TextTertiary
                        )
                        Column {
                            Text(device.modelName, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(device.edition, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    if (isSelected) {
                        Text(
                            "SELECCIONADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Automation & Ecosystem Features
        Text(
            text = "Automatización Inteligente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rotación Automática Día / Noche", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Cambia a esfera oscura minimalista tras el ocaso", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = autoRotateDayNight,
                        onCheckedChange = { autoRotateDayNight = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GalaxyCyan)
                    )
                }

                Divider(color = DarkBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sensores de Alta Frecuencia (Galaxy BioActive)", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Frecuencia cardíaca continua y SpO2 calibrada", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = highFrequencySensors,
                        onCheckedChange = { highFrequencySensors = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GalaxyEmerald)
                    )
                }
            }
        }

        // APK & Package Manager Card
        Text(
            text = "Distribución y Paquete APK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(GalaxyEmerald.copy(alpha = 0.5f)),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = GalaxyEmerald)
                        Column {
                            Text("Paquete APK Generado", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("app-debug.apk • Versión 1.0.0 (Build 1)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Badge(containerColor = GalaxyEmerald.copy(alpha = 0.2f)) {
                        Text("LISTO", color = GalaxyEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Divider(color = DarkBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ruta de compilación:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("app/build/outputs/apk/debug/", style = MaterialTheme.typography.bodySmall, color = GalaxyCyan, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compatibilidad:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("Android 9.0+ & Wear OS 5", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_SUBJECT,
                                "Galaxy Watch Studio - Instrucciones de Instalación APK"
                            )
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Galaxy Watch Studio APK compilado exitosamente.\n\n" +
                                "Para descargar el instalador APK directamente en tu dispositivo:\n" +
                                "1. Abre el menú Ajustes (⚙️) en la barra superior de AI Studio.\n" +
                                "2. Haz clic en 'Generate APK' / 'Download APK'.\n" +
                                "3. Instala el archivo en tu teléfono Samsung o Wear OS para sincronizar tus esferas."
                            )
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = android.content.Intent.createChooser(shareIntent, "Compartir detalles del APK").apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GalaxyEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instrucciones y Descarga del APK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Connection Management & Troubleshooting Actions
        Text(
            text = "Herramientas de Conectividad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showPairingGuideDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GalaxyAmber),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GalaxyAmber)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guía de Enlace", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.syncManager.openBluetoothSettings() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GalaxyCyan),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GalaxyCyan)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajustes BLE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Open Samsung Galaxy Wearable native app button
        Button(
            onClick = { viewModel.syncManager.openGalaxyWearableApp() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = GalaxyCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Aplicación Oficial Galaxy Wearable", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }

    // Pairing Guide Dialog
    if (showPairingGuideDialog) {
        AlertDialog(
            onDismissRequest = { showPairingGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Watch, contentDescription = null, tint = GalaxyCyan)
                    Text("Guía de Vinculación Galaxy Watch", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pasos para asegurar la conexión óptima:",
                        fontWeight = FontWeight.Bold,
                        color = GalaxyCyan,
                        fontSize = 13.sp
                    )
                    GuideStep(1, "Activa el Bluetooth y la Ubicación en tu teléfono móvil.")
                    GuideStep(2, "Abre la aplicación Samsung Galaxy Wearable y confirma que tu reloj figure como 'Conectado'.")
                    GuideStep(3, "En Galaxy Watch Studio, pulsa 'Sincronizar ahora' para transferir el paquete seguro Watch Face Format (WFF).")
                    GuideStep(4, "En tu reloj, mantén pulsada la esfera actual y selecciona tu nuevo diseño.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPairingGuideDialog = false }) {
                    Text("Entendido", color = GalaxyCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun GuideStep(stepNumber: Int, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(GalaxyCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$stepNumber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GalaxyCyan)
        }
        Text(description, fontSize = 12.sp, color = TextPrimary)
    }
}

@Composable
private fun SecurityItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}

@Composable
private fun DeviceMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

