package io.github.zama47.asthmahelper.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Публичная ссылка на политику конфиденциальности (GitHub Pages из каталога docs). */
private const val PRIVACY_POLICY_URL = "https://zama47.github.io/asthma_helper/privacy-policy.html"

private const val DEVELOPER_EMAIL = "zama_47@outlook.com"

private const val TELEGRAM_URL = "https://t.me/Zamaa47"

private const val DISCLAIMER =
    "Приложение носит информационно-справочный характер, помогает вести дневник собственного " +
        "состояния и не является медицинским изделием: оно не ставит диагноз, не назначает лечение " +
        "и не заменяет консультацию врача. Решения о терапии принимайте вместе со своим врачом. " +
        "При тяжёлом приступе вызывайте скорую помощь."

/**
 * Экран «О приложении»: версия, дисклеймер и ссылки на политику конфиденциальности
 * и контакты разработчика. Ссылка на политику нужна для публикации в RuStore.
 */
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        // Нет браузера или почтового клиента — просто ничего не делаем.
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Asthma Helper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Версия ${appVersionName(context)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Дневник пикфлоуметрии, лекарств, приступов и условий среды. " +
                            "Медицинские данные хранятся только на вашем устройстве и никуда не отправляются.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ограничение ответственности",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedButton(
                onClick = { openUrl(PRIVACY_POLICY_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Политика конфиденциальности")
            }

            OutlinedButton(
                onClick = { openUrl("mailto:$DEVELOPER_EMAIL") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Написать разработчику")
            }

            OutlinedButton(
                onClick = { openUrl(TELEGRAM_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Telegram: @Zamaa47")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Версия приложения из манифеста (без BuildConfig, который отключён по умолчанию). */
@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
}.getOrDefault("—")
