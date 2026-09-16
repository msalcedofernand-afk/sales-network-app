package com.salesnetwork.avon.app.inspector

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.salesnetwork.avon.app.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class InspectorTarget(
    val id: String,
    val type: String,
    val bounds: ComposeRect,
    val content: String = ""
)

object DevInspectorState {
    private val bounds = mutableMapOf<String, ComposeRect>()
    var active by mutableStateOf(false)
        private set
    var selected by mutableStateOf<InspectorTarget?>(null)
        private set

    fun select(target: InspectorTarget) {
        if (!active) return
        selected = target
    }

    fun clear() {
        selected = null
    }

    fun toggle() {
        active = !active
        if (!active) selected = null
    }

    fun recordBounds(id: String, value: ComposeRect) {
        bounds[id] = value
    }

    fun boundsFor(id: String): ComposeRect = bounds[id] ?: ComposeRect.Zero
}

fun Modifier.inspectable(id: String, type: String, content: String = ""): Modifier =
    onGloballyPositioned { coordinates: LayoutCoordinates ->
        val position = coordinates.positionInRoot()
        val size = coordinates.size
        if (size.width > 0 && size.height > 0) {
            DevInspectorState.recordBounds(id, ComposeRect(position.x, position.y, position.x + size.width, position.y + size.height))
        }
    }.pointerInput(DevInspectorState.active, id) {
        if (DevInspectorState.active) {
            detectTapGestures(onLongPress = {
                DevInspectorState.select(InspectorTarget(id, type, DevInspectorState.boundsFor(id), content))
            })
        }
    }

@Composable
fun DevInspectorOverlay() {
    if (!BuildConfig.DEBUG) return
    val context = LocalContext.current
    val view = LocalView.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        if (!DevInspectorState.active) {
            Button(onClick = DevInspectorState::toggle, modifier = Modifier.padding(16.dp)) { Text("Inspector") }
        } else if (DevInspectorState.selected == null) {
            Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Inspector activo · mantén pulsado un componente")
                    Button(onClick = DevInspectorState::toggle) { Text("Salir") }
                }
            }
        } else {
            val target = DevInspectorState.selected!!
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0xFFE39A26),
                    topLeft = Offset(target.bounds.left, target.bounds.top),
                    size = androidx.compose.ui.geometry.Size(target.bounds.width, target.bounds.height),
                    style = Stroke(width = 4f)
                )
            }
            Surface(
            modifier = Modifier.padding(16.dp).widthIn(max = 340.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            shadowElevation = 12.dp
            ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dev Inspector", style = MaterialTheme.typography.titleMedium)
                Text("${target.type} · ${target.id}", style = MaterialTheme.typography.bodyMedium)
                if (target.content.isNotBlank()) Text(target.content.take(180), style = MaterialTheme.typography.bodySmall)
                var title by androidx.compose.runtime.remember { mutableStateOf("Revisión de ${target.type}") }
                var description by androidx.compose.runtime.remember { mutableStateOf("") }
                var issueType by androidx.compose.runtime.remember { mutableStateOf("layout") }
                var severity by androidx.compose.runtime.remember { mutableStateOf("P2") }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = issueType, onValueChange = { issueType = it }, label = { Text("Tipo") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = severity, onValueChange = { severity = it }, label = { Text("Prioridad") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { saveEvidence(context, view, target, title, description, issueType, severity) }) { Text("Guardar") }
                    Button(onClick = { exportReport(context) }) { Text("Exportar") }
                    Button(onClick = DevInspectorState::clear) { Text("Cerrar") }
                }
            }
        }
        }
    }
}

private fun inspectorDirectory(context: Context): File = File(context.filesDir, "inspector").apply { mkdirs() }

private fun saveEvidence(context: Context, view: View, target: InspectorTarget, title: String, description: String, issueType: String, severity: String) {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val safeId = target.id.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(80)
    val imageName = "$safeId-${System.currentTimeMillis()}.png"
    val crop = target.bounds.let { bounds ->
        val left = bounds.left.toInt().coerceIn(0, bitmap.width)
        val top = bounds.top.toInt().coerceIn(0, bitmap.height)
        val right = bounds.right.toInt().coerceIn((left + 1).coerceAtMost(bitmap.width), bitmap.width)
        val bottom = bounds.bottom.toInt().coerceIn((top + 1).coerceAtMost(bitmap.height), bitmap.height)
        Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }
    val imageFile = File(inspectorDirectory(context), imageName)
    imageFile.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
    crop.recycle()
    bitmap.recycle()
    val issue = JSONObject().apply {
        put("id", UUID.randomUUID().toString())
        put("schemaVersion", 1)
        put("platform", "android")
        put("screen", target.id.substringBefore(':'))
        put("elementId", target.id)
        put("issueType", issueType.take(40))
        put("severity", severity.take(10))
        put("status", "open")
        put("title", title.take(160))
        put("description", description.take(2000))
        put("screenshotFile", "screenshots/android/$imageName")
        put("appVersion", BuildConfig.VERSION_NAME)
        put("buildRevision", BuildConfig.BUILD_REVISION)
        put("createdAt", System.currentTimeMillis())
    }
    File(inspectorDirectory(context), "issues.jsonl").appendText(issue.toString() + "\n")
}

private fun exportReport(context: Context) {
    val source = inspectorDirectory(context)
    val zipFile = File(context.cacheDir, "inspector").apply { mkdirs() }.resolve("sales-network-inspector-${System.currentTimeMillis()}.zip")
    ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
        source.listFiles()?.forEach { file ->
            val entryName = if (file.extension.equals("png", ignoreCase = true)) {
                "screenshots/android/${file.name}"
            } else {
                file.name
            }
            zip.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        zip.putNextEntry(ZipEntry("manifest.json"))
        zip.write(JSONObject().apply {
            put("schemaVersion", 1)
            put("platform", "android")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("buildRevision", BuildConfig.BUILD_REVISION)
            put("exportedAt", System.currentTimeMillis())
        }.toString(2).toByteArray())
        zip.closeEntry()
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", zipFile)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Compartir reporte del inspector").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
