package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserDataRepository
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

enum class LegalCategory(val title: String) {
    PRIVACY("Privacidad y Datos"),
    TERMS("Términos y Licencias"),
    COPYRIGHT("Derechos de Autor & DMCA"),
    ACCOUNT("Gestión de Cuenta y Derechos")
}

enum class LegalDocType {
    PRIVACY_POLICY,
    TERMS_AND_CONDITIONS,
    LEGAL_NOTICE,
    COOKIES_POLICY,
    COPYRIGHT,
    DMCA_POLICY,
    INTELLECTUAL_PROPERTY,
    ACCOUNT_DELETION,
    DATA_PURGE,
    LICENSES,
    EULA,
    LEGAL_CONTACT
}

data class LegalSection(
    val title: String,
    val content: String,
    val bullets: List<String> = emptyList()
)

data class LegalDocumentModel(
    val type: LegalDocType,
    val title: String,
    val category: LegalCategory,
    val icon: ImageVector,
    val version: String = "2.4.0-LEGAL",
    val lastUpdated: String = "29 de Julio de 2026",
    val summary: String,
    val sections: List<LegalSection>
)

object LegalDocumentProvider {

    fun getAllDocuments(): List<LegalDocumentModel> = listOf(
        LegalDocumentModel(
            type = LegalDocType.PRIVACY_POLICY,
            title = "Política de Privacidad",
            category = LegalCategory.PRIVACY,
            icon = Icons.Default.Security,
            summary = "Información detallada sobre cómo recopilamos, protegemos y procesamos sus datos en Firebase Authentication y Realtime Database.",
            sections = listOf(
                LegalSection(
                    title = "1. Responsable del Tratamiento de Datos",
                    content = "La presente Política de Privacidad regula el tratamiento de datos personales realizado por la plataforma CinéSync en su aplicación móvil Android. Garantizamos la confidencialidad y protección de los datos de nuestros usuarios de acuerdo con el Reglamento General de Protección de Datos (RGPD) y regulaciones locales e internacionales de privacidad.",
                    bullets = listOf(
                        "Plataforma: CinéSync App",
                        "Infraestructura Servidor: Google Firebase Auth & Realtime Database",
                        "Servidor de Base de Datos: https://abby-cdb30-default-rtdb.firebaseio.com",
                        "Delegado de Protección de Datos: privacy@cinesync.app"
                    )
                ),
                LegalSection(
                    title = "2. Datos Personales Recopilados",
                    content = "Recopilamos únicamente la información técnica y de uso estrictamente necesaria para brindar el servicio de streaming y sincronización de catálogo:",
                    bullets = listOf(
                        "Datos de Identificación: Correo electrónico, nombre de perfil público y UID generado por Firebase Authentication.",
                        "Autenticación con Google y Correo: Token de sesión seguro cifrado mediante JWT.",
                        "Datos de Acceso Invitado: Identificador anónimo temporal generado en Firebase Auth sin datos personales identificables.",
                        "Historial de Reproducción: Título del contenido, identificador (contentId), timestamp exacto y posición de reproducción en milisegundos (positionMs).",
                        "Lista de Favoritos: Colección de películas y series guardadas por el usuario sincronizadas en la ruta /favoritos/{uid}.",
                        "Estado de Continuar Viendo: Progreso guardado automáticamente en /continuar_viendo/{uid} para reanudar desde cualquier dispositivo.",
                        "Preferencias de Notificaciones: Identificador de WorkManager para verificar nuevos lanzamientos y capítulos de contenidos agregados a Favoritos."
                    )
                ),
                LegalSection(
                    title = "3. Uso y Finalidades del Tratamiento",
                    content = "Los datos recogidos se utilizan exclusivamente para las siguientes finalidades funcionales:",
                    bullets = listOf(
                        "Sincronizar su progreso de reproducción, favoritos e historial entre sesiones.",
                        "Personalizar las recomendaciones algorítmicas en la sección 'Para Ti' en base a sus contenidos vistos.",
                        "Procesar credenciales de inicio de sesión de forma segura sin almacenar contraseñas en texto plano.",
                        "Ejecutar tareas programadas en segundo plano (WorkManager) para notificar nuevos estrenos globales y nuevos capítulos de sus contenidos guardados en Favoritos.",
                        "Gestionar permisos de acceso administrativos para usuarios autorizados en el Panel de Administración."
                    )
                ),
                LegalSection(
                    title = "4. Almacenamiento y Seguridad de Datos",
                    content = "Todos los datos transmitidos entre la aplicación y nuestros servidores utilizan cifrado HTTPS/TLS 1.3 de extremo a extremo. La información persistente reside en clusters seguros de Firebase Realtime Database protegidos mediante Reglas de Seguridad (Security Rules) que garantizan que únicamente el propietario del UID puede leer o escribir en su nodo de favoritos e historial."
                ),
                LegalSection(
                    title = "5. Conservación y Supresión de Datos",
                    content = "Sus datos personales se mantendrán almacenados de manera indefinida mientras su cuenta permanezca activa. En cualquier momento puede ejercer su derecho de eliminación de datos o baja definitiva de cuenta a través de las secciones correspondientes en este Módulo Legal."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.TERMS_AND_CONDITIONS,
            title = "Términos y Condiciones",
            category = LegalCategory.TERMS,
            icon = Icons.Default.Gavel,
            summary = "Reglas, obligaciones y términos de uso del servicio de streaming multimedia CinéSync.",
            sections = listOf(
                LegalSection(
                    title = "1. Aceptación del Acuerdo",
                    content = "Al descargar, instalar o utilizar la aplicación CinéSync, usted acepta plenamente el presente contrato de Términos y Condiciones. Si no está de acuerdo con alguno de los términos, debe abstenerse de utilizar el servicio y desinstalar la aplicación."
                ),
                LegalSection(
                    title = "2. Requisitos de Uso y Cuentas",
                    content = "Para acceder a las funciones avanzadas como sincronización de favoritos e historial, el usuario debe crear una cuenta válida utilizando correo y contraseña o inicio de sesión con Google. El usuario es responsable de mantener la confidencialidad de su contraseña."
                ),
                LegalSection(
                    title = "3. Funcionalidades del Reproductor Multimedia",
                    content = "CinéSync incluye un reproductor de video de alta tecnología impulsado por Android Media3 ExoPlayer y soporte para Web Embeds (WebView):",
                    bullets = listOf(
                        "Control de velocidad de reproducción (0.5x a 2.0x).",
                        "Soporte multilenguaje para pistas de audio (Español, Inglés, etc.) y subtítulos (VTT/SRT).",
                        "Servidores dobles (Servidor Principal y Servidor Secundario) para garantizar alta disponibilidad de transmisión.",
                        "Navegación por temporadas y capítulos para series de televisión y anime.",
                        "Modo Inmersivo de pantalla completa con soporte de rotación automática y ajuste de aspecto."
                    )
                ),
                LegalSection(
                    title = "4. Uso Prohibido y Conducta del Usuario",
                    content = "Queda estrictamente prohibido realizar cualquier intento de ingeniería inversa, modificación del paquete APK, ataques de denegación de servicio a la Realtime Database o uso no autorizado del Panel de Administración reservado para administradores."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.LEGAL_NOTICE,
            title = "Aviso Legal",
            category = LegalCategory.TERMS,
            icon = Icons.Default.Info,
            summary = "Información legal del proveedor, exención de responsabilidad e información técnica del sistema.",
            sections = listOf(
                LegalSection(
                    title = "1. Datos Informativos",
                    content = "En cumplimiento de las normativas de comercio electrónico y servicios de la sociedad de la información, se informa que la aplicación CinéSync es una plataforma de software desarrollada para la gestión, catalogación y visualización de contenidos audiovisuales interactivos."
                ),
                LegalSection(
                    title = "2. Exención de Responsabilidad sobre Enlaces Externos",
                    content = "La aplicación utiliza reproductores web embebidos y enlaces de transmisión (HLS/MP4/Dailymotion/Web). CinéSync no aloja directamente archivos de video sujetos a derechos de autor en sus servidores propios, actuando únicamente como un organizador e interfaz de reproducción."
                ),
                LegalSection(
                    title = "3. Disponibilidad del Servicio",
                    content = "Nos esforzamos por mantener la aplicación operativa las 24 horas del día. Sin embargo, no garantizamos la disponibilidad ininterrumpida debido a mantenimientos del servidor de Firebase, fallos en la red del usuario o interrupciones en proveedores de video externos."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.COOKIES_POLICY,
            title = "Política de Cookies y Caché",
            category = LegalCategory.PRIVACY,
            icon = Icons.Default.Cookie,
            summary = "Detalle del almacenamiento local, tokens de sesión y caché de imágenes en el dispositivo.",
            sections = listOf(
                LegalSection(
                    title = "1. Uso de Almacenamiento Local en Android",
                    content = "Aunque las aplicaciones nativas de Android no utilizan cookies tradicionales de navegador HTTP en el sentido web clásico, empleamos tecnologías equivalentes de almacenamiento en el dispositivo:",
                    bullets = listOf(
                        "Firebase Auth Local Storage: Token JWT cifrado para mantener su sesión iniciada de manera persistente.",
                        "Coil Disk & Memory Cache: Caché de imágenes para portadas y banners, reduciendo el consumo de datos móviles.",
                        "SharedPreferences & DataStore: Guarda sus preferencias de notificaciones y marca de tiempo de última verificación.",
                        "Caché de WebView: Almacenamiento temporal para reproductores web como Dailymotion o reproductores HTML5."
                    )
                ),
                LegalSection(
                    title = "2. Gestión y Limpieza de Caché",
                    content = "El usuario puede limpiar en cualquier momento el almacenamiento en caché de la aplicación directamente desde los Ajustes del Sistema de Android -> Aplicaciones -> CinéSync -> Almacenamiento -> Borrar Caché."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.COPYRIGHT,
            title = "Derechos de Autor",
            category = LegalCategory.COPYRIGHT,
            icon = Icons.Default.Copyright,
            summary = "Aviso de propiedad sobre marcas, títulos, afiches e imágenes del catálogo audiovisual.",
            sections = listOf(
                LegalSection(
                    title = "1. Marcas y Propiedad de Terceros",
                    content = "Todos los nombres de películas, series, marcas registradas, afiches (posters), imágenes de fondo y sinopsis mostrados en CinéSync son propiedad intelectual exclusiva de sus respectivos creadores, estudios de producción y distribuidores oficiales."
                ),
                LegalSection(
                    title = "2. Integración de Datos de TMDB",
                    content = "La información de catálogo, calificaciones, géneros y sinopsis se obtiene y complementa mediante las APIs públicas de The Movie Database (TMDB). Este producto utiliza la API de TMDB pero no está endosado ni certificado por TMDB."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.DMCA_POLICY,
            title = "Política DMCA",
            category = LegalCategory.COPYRIGHT,
            icon = Icons.Default.Report,
            summary = "Procedimiento formal para la notificación y retirada inmediata de contenido infractor (Takedown Notice).",
            sections = listOf(
                LegalSection(
                    title = "1. Compromiso de Cumplimiento DMCA",
                    content = "CinéSync respeta los derechos de propiedad intelectual de terceros y cumple rigurosamente con la Digital Millennium Copyright Act (DMCA). Ante cualquier notificación legítima de infracción, tomamos medidas inmediatas."
                ),
                LegalSection(
                    title = "2. Requisitos para enviar una Notificación DMCA",
                    content = "Si usted es el titular de los derechos de autor o un agente autorizado y considera que un enlace o contenido en la aplicación infringe sus derechos, debe enviar una comunicación por escrito a nuestro Departamento Legal (dmca@cinesync.app) incluyendo:",
                    bullets = listOf(
                        "Firma física o electrónica del titular de los derechos.",
                        "Identificación de la obra protegida por derechos de autor que se alega infringida.",
                        "Identificación del enlace de video o identificador (contentId) exacto en la aplicación.",
                        "Información de contacto (correo electrónico, teléfono y dirección física).",
                        "Declaración de buena fe afirmando que el uso del material no está autorizado.",
                        "Declaración bajo pena de perjurio de que la información proporcionada es precisa."
                    )
                ),
                LegalSection(
                    title = "3. Retirada Inmediata desde el Panel de Administración",
                    content = "Una vez recibida una solicitud DMCA válida, los administradores de CinéSync procederán a eliminar inmediatamente la entrada del catálogo o enlace del reproductor a través del Panel de Administración dentro de un plazo máximo de 24 horas hábiles."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.INTELLECTUAL_PROPERTY,
            title = "Propiedad Intelectual",
            category = LegalCategory.COPYRIGHT,
            icon = Icons.Default.Code,
            summary = "Titularidad del código fuente Kotlin, arquitectura Jetpack Compose y diseño de la interfaz.",
            sections = listOf(
                LegalSection(
                    title = "1. Licencia sobre el Software CinéSync",
                    content = "Todo el código fuente en Kotlin, componentes de interfaz de usuario en Jetpack Compose, gráficos vectoriales personalizados, base de datos interna y arquitectura de la aplicación CinéSync son propiedad intelectual reservada de sus desarrolladores."
                ),
                LegalSection(
                    title = "2. Restricciones",
                    content = "Se concede al usuario una licencia limitada, personal, no exclusiva, revocable e intransferible para usar la aplicación estrictamente para entretenimiento personal y no comercial."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.ACCOUNT_DELETION,
            title = "Eliminación de Cuenta",
            category = LegalCategory.ACCOUNT,
            icon = Icons.Default.DeleteForever,
            summary = "Procedimiento completo y garantizado para dar de baja su cuenta y borrar su registro de Firebase.",
            sections = listOf(
                LegalSection(
                    title = "1. Procedimiento de Baja Definitiva",
                    content = "Usted tiene derecho a solicitar la eliminación completa e irreversible de su cuenta en cualquier momento. Al ejecutar la baja, el sistema realizará las siguientes acciones en tiempo real:",
                    bullets = listOf(
                        "Eliminación de su perfil de usuario en /usuarios/{uid} de Firebase Realtime Database.",
                        "Supresión de su lista de Favoritos en /favoritos/{uid}.",
                        "Borrado completo de su Historial de reproducción en /historial/{uid}.",
                        "Eliminación de sus marcas de Continuar Viendo en /continuar_viendo/{uid}.",
                        "Baja y revocación del usuario en el servicio Firebase Authentication."
                    )
                ),
                LegalSection(
                    title = "2. Acción Directa de Eliminación",
                    content = "Haga clic en el botón inferior para iniciar el proceso de eliminación definitiva de su cuenta de forma automática."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.DATA_PURGE,
            title = "Eliminación de Datos",
            category = LegalCategory.ACCOUNT,
            icon = Icons.Default.CleaningServices,
            summary = "Derecho a purgar únicamente sus datos de actividad (Favoritos, Historial y Progreso) sin cerrar su cuenta.",
            sections = listOf(
                LegalSection(
                    title = "1. Purga Parcial de Actividad",
                    content = "Si desea conservar su cuenta de usuario y correo registrado, pero quiere reiniciar su actividad desde cero, puede ejecutar la Purga de Datos. Esto eliminará únicamente sus listas de Favoritos, Historial y Continuar Viendo de los servidores sin dar de baja su perfil."
                ),
                LegalSection(
                    title = "2. Impacto de la Purga",
                    content = "Esta acción es irreversible. Sus recomendaciones en 'Para Ti' se reiniciarán a su estado por defecto hasta que vuelva a interactuar con nuevos contenidos."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.LICENSES,
            title = "Licencias de Código Abierto",
            category = LegalCategory.TERMS,
            icon = Icons.Default.Terminal,
            summary = "Atribuciones y licencias Apache 2.0 / MIT de las bibliotecas de software utilizadas.",
            sections = listOf(
                LegalSection(
                    title = "1. Bibliotecas y Frameworks",
                    content = "Agradecemos a la comunidad de código abierto. CinéSync utiliza las siguientes bibliotecas bajo licencias abiertas:",
                    bullets = listOf(
                        "Android Jetpack Compose (Apache License 2.0)",
                        "Google Firebase SDK - Auth & Realtime Database (Apache License 2.0)",
                        "AndroidX Media3 ExoPlayer (Apache License 2.0)",
                        "Coil Image Loader (Apache License 2.0)",
                        "Kotlin Coroutines & StateFlow (Apache License 2.0)",
                        "AndroidX WorkManager (Apache License 2.0)",
                        "Google Material Design 3 Components (Apache License 2.0)"
                    )
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.EULA,
            title = "Acuerdo del Usuario (EULA)",
            category = LegalCategory.TERMS,
            icon = Icons.Default.Assignment,
            summary = "Contrato de licencia de usuario final relativo al acceso a la aplicación y uso multimedia.",
            sections = listOf(
                LegalSection(
                    title = "1. Otorgamiento de Licencia EULA",
                    content = "Este Acuerdo de Licencia de Usuario Final ('EULA') es un acuerdo legal vinculante entre usted y CinéSync para el uso de la aplicación móvil y servicios asociados."
                ),
                LegalSection(
                    title = "2. Restricciones de Edad",
                    content = "La aplicación está diseñada para usuarios generales. Si es menor de edad en su jurisdicción, debe contar con la supervisión de un padre o tutor legal para utilizar el servicio de transmisión."
                )
            )
        ),
        LegalDocumentModel(
            type = LegalDocType.LEGAL_CONTACT,
            title = "Contacto Legal",
            category = LegalCategory.ACCOUNT,
            icon = Icons.Default.ContactMail,
            summary = "Canales oficiales para consultas sobre privacidad, requerimientos judiciales y soporte normativo.",
            sections = listOf(
                LegalSection(
                    title = "1. Canales Oficiales de Atención Legal",
                    content = "Para cualquier duda, sugerencia o requerimiento formal en materia de privacidad, protección de datos personales o derechos de autor, puede comunicarse directamente con nuestro equipo:",
                    bullets = listOf(
                        "Correo de Asuntos Legales: legal@cinesync.app",
                        "Atención de Privacidad RGPD: privacy@cinesync.app",
                        "Notificaciones DMCA: dmca@cinesync.app",
                        "Horario de Atención: Lunes a Viernes de 09:00 a 18:00 (UTC-5)",
                        "Respuesta Garantizada: Máximo 48 horas hábiles"
                    )
                )
            )
        )
    )

    fun getDocument(type: LegalDocType): LegalDocumentModel {
        return getAllDocuments().first { it.type == type }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalHubScreen(
    authViewModel: AuthViewModel,
    onSelectDoc: (LegalDocType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<LegalCategory?>(null) }

    val allDocs = remember { LegalDocumentProvider.getAllDocuments() }

    val filteredDocs = remember(searchQuery, selectedCategory) {
        allDocs.filter { doc ->
            val matchesCategory = selectedCategory == null || doc.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.summary.contains(searchQuery, ignoreCase = true) ||
                    doc.sections.any { sec -> sec.title.contains(searchQuery, ignoreCase = true) || sec.content.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Información Legal",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141418))
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar términos, privacidad, DMCA...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFE50914)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE50914),
                    unfocusedBorderColor = Color(0xFF2E2E38),
                    focusedContainerColor = Color(0xFF181820),
                    unfocusedContainerColor = Color(0xFF181820),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("legal_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE50914),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1E28),
                            labelColor = Color.Gray
                        )
                    )
                }
                items(LegalCategory.entries.toTypedArray()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE50914),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1E28),
                            labelColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Version info banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Centro de Cumplimiento Normativo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Versión de documentos: 2.4.0-LEGAL • Actualizado: 29 Julio 2026", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Document List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDocs, key = { it.type.name }) { doc ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDoc(doc.type) }
                            .testTag("legal_doc_card_${doc.type.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE50914).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(doc.icon, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = doc.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = doc.summary,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(Icons.Default.ChevronRight, contentDescription = "Ver document", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentDetailScreen(
    docType: LegalDocType,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val doc = remember(docType) { LegalDocumentProvider.getDocument(docType) }

    var expandedSectionIndices by remember { mutableStateOf(setOf(0)) }
    var selectedTocIndex by remember { mutableIntStateOf(-1) }

    // Dialogs for Account / Data Deletion
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showPurgeDataDialog by remember { mutableStateOf(false) }
    var isProcessingAction by remember { mutableStateOf(false) }

    val userProfile by authViewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
    ) {
        // Top Header
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = doc.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Versión ${doc.version} • ${doc.lastUpdated}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141418))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Category & Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFE50914).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = doc.category.title,
                                color = Color(0xFFE50914),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Documento Oficial",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = doc.summary,
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automatic Table of Contents / Índice Interactivo
            Text(
                text = "Índice de Secciones",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    doc.sections.forEachIndexed { index, section ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTocIndex = index
                                    expandedSectionIndices = expandedSectionIndices + index
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (expandedSectionIndices.contains(index)) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                fontSize = 12.sp,
                                color = if (selectedTocIndex == index) Color(0xFFE50914) else Color.LightGray,
                                fontWeight = if (selectedTocIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Expand All / Collapse All Controls
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Contenido Legal (${doc.sections.size} cláusulas)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        expandedSectionIndices = if (expandedSectionIndices.size == doc.sections.size) {
                            emptySet()
                        } else {
                            doc.sections.indices.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (expandedSectionIndices.size == doc.sections.size) "Plegar Todo" else "Desplegar Todo",
                        color = Color(0xFFE50914),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Render Accordion Sections
            doc.sections.forEachIndexed { index, section ->
                val isExpanded = expandedSectionIndices.contains(index)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTocIndex == index) Color(0xFF221A20) else Color(0xFF181820)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .animateContentSize()
                        .border(
                            width = if (selectedTocIndex == index) 1.dp else 0.dp,
                            color = if (selectedTocIndex == index) Color(0xFFE50914) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSectionIndices = if (isExpanded) {
                                        expandedSectionIndices - index
                                    } else {
                                        expandedSectionIndices + index
                                    }
                                }
                        ) {
                            Text(
                                text = section.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = section.content,
                                    fontSize = 13.sp,
                                    color = Color(0xFFD0D0D8),
                                    lineHeight = 19.sp
                                )

                                if (section.bullets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    section.bullets.forEach { bullet ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("• ", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = bullet,
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Actions depending on Document Type
            if (docType == LegalDocType.ACCOUNT_DELETION) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1518)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Zona de Baja Definitiva de Cuenta",
                            color = Color(0xFFFF5252),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Usuario actual: ${userProfile?.email ?: "Sesión activa"}\nEsta acción borrará de inmediato toda su información personal en Firebase Auth y Realtime Database (Favoritos, Historial, Continuar Viendo y Registro de Perfil).",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_trigger_delete_account")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar Mi Cuenta y Datos Ahora", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (docType == LegalDocType.DATA_PURGE) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Purga de Actividad e Historial",
                            color = Color(0xFF64B5F6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Elimina únicamente sus listas de Favoritos, Historial y Continuar Viendo guardadas en la Realtime Database sin cerrar su sesión.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showPurgeDataDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_trigger_purge_data")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Purgar Mi Historial y Favoritos", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (docType == LegalDocType.LEGAL_CONTACT) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C221E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Formulario de Contacto Legal",
                            color = Color(0xFF81C784),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Haga clic para enviar una solicitud formal por correo electrónico a nuestro Delegado de Protección de Datos (DPO) o Departamento Legal.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Correo de contacto legal: legal@cinesync.app", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar Correo a legal@cinesync.app", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) showDeleteAccountDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE50914)) },
            title = { Text("¿Eliminar cuenta definitivamente?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Esta acción no se puede deshacer. Se borrarán permanentemente sus datos en Firebase Realtime Database (Favoritos, Historial, Continuar Viendo y Perfil) y se dará de baja su cuenta en Firebase Auth.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        authViewModel.deleteAccountAndData { result ->
                            isProcessingAction = false
                            showDeleteAccountDialog = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Cuenta y datos eliminados con éxito", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Error al eliminar la cuenta", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    enabled = !isProcessingAction
                ) {
                    if (isProcessingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Sí, Eliminar Todo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E28)
        )
    }

    // Purge Data Confirmation Dialog
    if (showPurgeDataDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) showPurgeDataDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF64B5F6)) },
            title = { Text("¿Purgar historial y favoritos?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Se eliminarán sus listas de Favoritos, Historial y Continuar Viendo en la base de datos de Firebase. Su cuenta permanecerá activa.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        val uid = userProfile?.uid ?: ""
                        coroutineScope.launch {
                            val res = UserDataRepository().purgeAllUserData(uid)
                            isProcessingAction = false
                            showPurgeDataDialog = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Historial y favoritos eliminados con éxito", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al purgar los datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    enabled = !isProcessingAction
                ) {
                    if (isProcessingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Sí, Purgar Actividad", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurgeDataDialog = false },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E28)
        )
    }
}
