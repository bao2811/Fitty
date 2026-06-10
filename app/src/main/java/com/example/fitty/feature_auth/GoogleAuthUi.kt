package com.example.fitty.feature_auth

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

@Composable
internal fun GoogleAuthButton(
    loading: Boolean,
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val googleClient = remember(context) {
        val webClientId = context.resolveGoogleWebClientId()
        if (webClientId.isBlank()) {
            null
        } else {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, options)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                onError(context.getString(R.string.auth_google_failed))
            } else {
                onIdToken(idToken)
            }
        } catch (error: ApiException) {
            onError(context.resolveGoogleSignInError(error))
        }
    }

    OutlinedButton(
        onClick = {
            val client = googleClient
            if (client == null) {
                onError(context.getString(R.string.auth_google_not_configured))
            } else {
                launcher.launch(client.signInIntent)
            }
        },
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    Text("G", fontWeight = FontWeight.Black)
                }
            }
            Text(
                text = stringResource(R.string.auth_continue_with_google),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun Context.resolveGoogleWebClientId(): String {
    val resourcePackages = listOf(
        packageName,
        runCatching { resources.getResourcePackageName(R.string.app_name) }.getOrDefault(packageName)
    ).distinct()
    resourcePackages.forEach { resourcePackage ->
        val generatedId = resources.getIdentifier("default_web_client_id", "string", resourcePackage)
        if (generatedId != 0) {
            return getString(generatedId).trim()
        }
    }
    return getString(R.string.google_web_client_id).trim()
}

private fun Context.resolveGoogleSignInError(error: ApiException): String {
    return when (error.statusCode) {
        CommonStatusCodes.CANCELED -> getString(R.string.auth_google_cancelled)
        CommonStatusCodes.NETWORK_ERROR -> getString(R.string.auth_google_network_error)
        CommonStatusCodes.DEVELOPER_ERROR -> getString(R.string.auth_google_developer_error)
        else -> getString(R.string.auth_google_failed_with_code, error.statusCode)
    }
}
