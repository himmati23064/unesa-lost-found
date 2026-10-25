package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.User
import com.example.ui.theme.UnesaBlueDark
import com.example.ui.theme.UnesaBluePrimary
import com.example.ui.theme.UnesaYellow
import com.example.viewmodel.ReportViewModel

@Composable
fun LoginScreen(
    viewModel: ReportViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isRegisterMode by remember { mutableStateOf(false) }

    // Form Login State
    var emailOrNim by remember { mutableStateOf("himmati.23064@mhs.unesa.ac.id") }
    var password by remember { mutableStateOf("unesa2024") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Form Register State
    var regName by remember { mutableStateOf("Himmati") }
    var regNim by remember { mutableStateOf("23064") }
    var regFaculty by remember { mutableStateOf("Fakultas Teknik") }
    var regPhone by remember { mutableStateOf("081234567890") }
    var regCampus by remember { mutableStateOf("Kampus Ketintang") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UnesaBluePrimary,
                        UnesaBlueDark,
                        Color(0xFF071426)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo & Badge UNESA
            Surface(
                shape = CircleShape,
                color = UnesaYellow,
                modifier = Modifier.size(68.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "UNESA",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = UnesaBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "UNESA Lost & Found",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Sistem Pencocokan Cerdas Barang Hilang & Ditemukan\nCivitas Akademika Universitas Negeri Surabaya",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Banner Hero Ilustrasi Kampus UNESA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.unesa_campus_hero),
                    contentDescription = "Kampus UNESA",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card Form Login / Register
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Tab Switcher Masuk vs Daftar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isRegisterMode) UnesaBluePrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isRegisterMode = false }
                                .testTag("tab_login_mode")
                        ) {
                            Text(
                                text = "Masuk Civitas",
                                fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRegisterMode) UnesaBluePrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isRegisterMode = true }
                                .testTag("tab_register_mode")
                        ) {
                            Text(
                                text = "Daftar Akun Baru",
                                fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!isRegisterMode) {
                        // Form Masuk
                        Text(
                            text = "Email Mahasiswa / NIM UNESA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = emailOrNim,
                            onValueChange = { emailOrNim = it },
                            placeholder = { Text("misal: 23064 atau email@mhs.unesa.ac.id") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = UnesaBluePrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_username"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Kata Sandi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = UnesaBluePrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_password"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Tombol Masuk Utama
                        Button(
                            onClick = {
                                viewModel.login(
                                    emailOrNim = emailOrNim,
                                    password = password,
                                    onSuccess = onLoginSuccess,
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_do_login"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UnesaBluePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Masuk ke Sistem",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tombol Quick SSO UNESA
                        OutlinedButton(
                            onClick = {
                                viewModel.loginWithUnesaSso(onSuccess = onLoginSuccess)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_sso_unesa"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = UnesaYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Masuk Cepat via SSO UNESA",
                                fontWeight = FontWeight.SemiBold,
                                color = UnesaBluePrimary
                            )
                        }
                    } else {
                        // Form Registrasi Civitas Baru
                        Text(
                            text = "Nama Lengkap Civitas *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            placeholder = { Text("Nama Mahasiswa / Staff") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_name"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "NIM / NIP *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = regNim,
                            onValueChange = { regNim = it },
                            placeholder = { Text("Contoh: 23064...") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_nim"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Fakultas *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = regFaculty,
                            onValueChange = { regFaculty = it },
                            placeholder = { Text("Fakultas Teknik / FEB / FBS...") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_faculty"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Nomor WhatsApp Aktif *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            placeholder = { Text("081234567890") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_phone"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val newUser = User(
                                    uid = "user_${regNim}",
                                    name = regName.trim(),
                                    email = "${regNim}@mhs.unesa.ac.id",
                                    nim = regNim.trim(),
                                    phone = regPhone.trim(),
                                    faculty = regFaculty.trim(),
                                    primaryCampus = regCampus
                                )
                                viewModel.register(
                                    user = newUser,
                                    onSuccess = onLoginSuccess,
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_do_register"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UnesaBluePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Daftarkan Akun Civitas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Catatan Keamanan Civitas
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = UnesaYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Anda terlindungi oleh sistem keamanan Single Sign-On UNESA dan diawasi Satgas Ketertiban Kampus.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
