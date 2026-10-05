package com.example.sqlitevsroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Fondo = Color(0xFFF6F7FB)
private val NaranjaA = Color(0xFFF59E0B)
private val NaranjaB = Color(0xFFEA580C)
private val IndigoA = Color(0xFF6366F1)
private val IndigoB = Color(0xFF4338CA)
private val VerdeA = Color(0xFF14B8A6)
private val VerdeB = Color(0xFF0F766E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val helper = NotasDbHelper(applicationContext)
        val dao = AppDatabase.get(applicationContext).notaDao()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = IndigoB, background = Fondo)
            ) {
                App(helper, dao)
            }
        }
    }
}

@Composable
fun App(helper: NotasDbHelper, dao: NotaDao) {
    var pestana by remember { mutableIntStateOf(0) }
    val items = listOf(
        Triple("SQLite", "🗄️", NaranjaB),
        Triple("Room", "🚀", IndigoB),
        Triple("Conclusión", "💡", VerdeB)
    )

    Scaffold(
        containerColor = Fondo,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                items.forEachIndexed { i, (nombre, icono, color) ->
                    NavigationBarItem(
                        selected = pestana == i,
                        onClick = { pestana = i },
                        icon = { Text(icono, fontSize = 20.sp) },
                        label = { Text(nombre) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = color,
                            indicatorColor = color.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (pestana) {
                0 -> PantallaSqlite(helper)
                1 -> PantallaRoom(dao)
                else -> PantallaConclusion()
            }
        }
    }
}

/** SQLite nativo: la lista NO se actualiza sola, hay que recargarla a mano. */
@Composable
fun PantallaSqlite(helper: NotasDbHelper) {
    var texto by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    suspend fun recargar() {
        notas = withContext(Dispatchers.IO) { helper.listar() }
    }

    LaunchedEffect(Unit) { recargar() }

    PantallaNotas(
        titulo = "SQLite nativo",
        subtitulo = "SQLiteOpenHelper + ContentValues + Cursor",
        colores = listOf(NaranjaA, NaranjaB),
        acento = NaranjaB,
        aviso = "La lista se recarga a mano: después de cada insert se vuelve a llamar a listar().",
        texto = texto,
        onTextoChange = { texto = it },
        notas = notas,
        onAgregar = {
            val t = texto.trim()
            texto = ""
            scope.launch {
                withContext(Dispatchers.IO) { helper.insertar(t) }
                recargar()
            }
        }
    )
}

/** Room: la lista SE actualiza sola gracias al Flow. */
@Composable
fun PantallaRoom(dao: NotaDao) {
    var texto by remember { mutableStateOf("") }
    val flow = remember { dao.todas() }
    val notas by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    PantallaNotas(
        titulo = "Room",
        subtitulo = "@Entity + @Dao + Flow",
        colores = listOf(IndigoA, IndigoB),
        acento = IndigoB,
        aviso = "La lista se actualiza sola: el Flow del DAO avisa a la UI cada vez que cambia la tabla.",
        texto = texto,
        onTextoChange = { texto = it },
        notas = notas.map { it.titulo },
        onAgregar = {
            val t = texto.trim()
            texto = ""
            scope.launch { dao.insertar(Nota(titulo = t)) }
        }
    )
}

@Composable
fun Encabezado(titulo: String, subtitulo: String, colores: List<Color>, cantidad: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(colores),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                titulo,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(subtitulo, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            if (cantidad != null) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                    Text(
                        "$cantidad notas guardadas",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaNotas(
    titulo: String,
    subtitulo: String,
    colores: List<Color>,
    acento: Color,
    aviso: String,
    texto: String,
    onTextoChange: (String) -> Unit,
    notas: List<String>,
    onAgregar: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Encabezado(titulo, subtitulo, colores, notas.size)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                label = { Text("Nueva nota") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = acento,
                    focusedLabelColor = acento,
                    cursorColor = acento,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            FilledIconButton(
                onClick = { if (texto.isNotBlank()) onAgregar() },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = acento)
            ) {
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = acento.copy(alpha = 0.10f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                aviso,
                color = acento,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (notas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📝", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text("Aún no hay notas", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Escribe algo arriba y toca +",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(notas) { index, nota ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(acento.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${notas.size - index}",
                                    color = acento,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(nota, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaConclusion() {
    Column(Modifier.fillMaxSize()) {
        Encabezado(
            titulo = "Conclusión",
            subtitulo = "¿Cuándo usar SQLite directo y cuándo Room?",
            colores = listOf(VerdeA, VerdeB),
            cantidad = null
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaInfo(
                "Room (recomendado casi siempre)", IndigoB,
                listOf(
                    "Queries verificadas en compilación.",
                    "Menos código: sin ContentValues ni Cursor.",
                    "Flow actualiza la UI automáticamente.",
                    "Migraciones más seguras."
                )
            )
            TarjetaInfo(
                "SQLite directo (casos puntuales)", NaranjaB,
                listOf(
                    "Código legado que ya lo usa.",
                    "Funciones muy específicas de SQLite.",
                    "Control total del SQL.",
                    "Ejercicio didáctico para entender qué hay debajo de Room."
                )
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Comparación rápida",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    FilaComparacion("", "SQLite", "Room", true)
                    HorizontalDivider()
                    FilaComparacion("SQL", "En texto", "Verificado al compilar")
                    FilaComparacion("Mapeo", "Manual (Cursor)", "Automático")
                    FilaComparacion("Actualizar lista", "A mano", "Sola (Flow)")
                    FilaComparacion("Hilos", "Tú decides", "Fuera del principal")
                    FilaComparacion("Migraciones", "Manuales", "Asistidas")
                }
            }
        }
    }
}

@Composable
fun TarjetaInfo(titulo: String, color: Color, puntos: List<String>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            }
            puntos.forEach { Text("•  $it", fontSize = 14.sp) }
        }
    }
}

@Composable
fun FilaComparacion(a: String, b: String, c: String, encabezado: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            a,
            modifier = Modifier.weight(1.1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Text(
            b,
            modifier = Modifier.weight(1f),
            color = NaranjaB,
            fontSize = 13.sp,
            fontWeight = if (encabezado) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            c,
            modifier = Modifier.weight(1f),
            color = IndigoB,
            fontSize = 13.sp,
            fontWeight = if (encabezado) FontWeight.Bold else FontWeight.Normal
        )
    }
}