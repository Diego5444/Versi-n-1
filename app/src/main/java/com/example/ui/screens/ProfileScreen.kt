package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.NotificationHelper
import com.example.notification.NotificationScheduler
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FavoritesViewModel
import com.example.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    onOpenAdminPanel: () -> Unit,
    onOpenLegal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by authViewModel.userProfile.collectAsState()
    val favorites by favoritesViewModel.favoritesList.collectAsState()
    val watchHistory by historyViewModel.watchHistory.collectAsState()

    var isEditingName by remember { mutableStateOf(false) }
    var displayNameInput by remember { mutableStateOf(userProfile?.displayName ?: "") }

    var isNotificationsEnabled by remember { mutableStateOf(NotificationScheduler.isNotificationsEnabled(context)) }
    var lastCheckTime by remember { mutableLongStateOf(NotificationScheduler.getLastCheckTime(context)) }

    val isAdmin = userProfile?.role == "admin"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Perfil de Usuario",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                        .border(3.dp, if (isAdmin) Color(0xFFFFD700) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userProfile?.displayName?.ifBlank { "Usuario" } ?: "Usuario",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = userProfile?.email?.ifBlank { "Invitado" } ?: "Invitado",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (isAdmin) {
                    Surface(
                        color = Color(0xFF332000),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "ADMINISTRADOR",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1E2D20),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Sincronizado",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sincronizado con Firebase Realtime DB",
                            color = Color(0xFF81C784),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Panel Shortcut Button (If Admin)
        if (isAdmin) {
            Button(
                onClick = onOpenAdminPanel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir Panel de Administración", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Scheduled Task & Notifications Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificaciones de Estrenos y Capítulos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Alertas automáticas en segundo plano (WorkManager)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            isNotificationsEnabled = enabled
                            if (enabled) {
                                NotificationScheduler.schedulePeriodicCheck(context)
                            } else {
                                NotificationScheduler.cancelPeriodicCheck(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE50914)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Te notificaremos automáticamente cuando se publiquen nuevas películas o animes en el catálogo, y cuando haya un nuevo capítulo disponible de los contenidos que agregaste a tus Favoritos.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (lastCheckTime > 0) {
                            "Última verificación: ${DateFormat.format("HH:mm - dd/MM", Date(lastCheckTime))}"
                        } else {
                            "Estado: Monitoreando nuevos estrenos y capítulos"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Cards Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(28.dp))
                    Text(
                        text = "${watchHistory.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(text = "Historial", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    Text(
                        text = "${favorites.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(text = "Favoritos", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ajustes de Perfil",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isEditingName) {
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Nombre de perfil") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(onClick = { isEditingName = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                authViewModel.updateProfile(displayNameInput)
                                isEditingName = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                        ) {
                            Text("Guardar", color = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            displayNameInput = userProfile?.displayName ?: ""
                            isEditingName = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar Nombre de Perfil", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOpenLegal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262632)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_legal_button")
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFE50914))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Información Legal", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { authViewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1416)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFE50914))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
