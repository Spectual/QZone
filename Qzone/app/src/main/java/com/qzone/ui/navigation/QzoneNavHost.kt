package com.qzone.ui.navigation

import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qzone.R
import com.qzone.feature.auth.AuthViewModel
import com.qzone.feature.auth.ui.RegisterScreen
import com.qzone.feature.auth.ui.SignInScreen
import com.qzone.feature.feed.FeedViewModel
import com.qzone.feature.feed.ui.FeedScreen
import com.qzone.feature.history.HistoryViewModel
import com.qzone.feature.history.HistoryDetailViewModel
import com.qzone.feature.history.ui.HistoryScreen
import com.qzone.feature.history.ui.HistoryDetailScreen
import com.qzone.feature.map.ui.NearbySurveyMapScreen
import com.qzone.feature.map.NearbyMapViewModel
import com.qzone.feature.profile.ProfileViewModel
import com.qzone.feature.profile.ui.EditProfileScreen
import com.qzone.feature.profile.ui.ProfileScreen
import com.qzone.feature.profile.ui.ProfileSettingsScreen
import com.qzone.feature.profile.ui.WalletScreen
import com.qzone.feature.rewards.RewardDetailViewModel
import com.qzone.feature.rewards.RewardsViewModel
import com.qzone.feature.rewards.ui.RewardDetailScreen
import com.qzone.feature.rewards.ui.RewardsScreen
import com.qzone.feature.survey.SurveyViewModel
import com.qzone.feature.survey.ui.SurveyScreen
import com.qzone.domain.repository.FirebaseLoginMode
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@Composable
fun QzoneNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
    appState: QzoneAppState
) {
    val startDestination = if (appState.userRepository.hasCachedSession()) {
        QzoneDestination.Feed.route
    } else {
        QzoneDestination.SignIn.route
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(QzoneDestination.SignIn.route) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val firebaseAuth = remember { FirebaseAuth.getInstance() }
            var showPhoneDialog by remember { mutableStateOf(false) }
            val defaultCountryCode = "+1"
            var countryCode by remember { mutableStateOf(defaultCountryCode) }
            var phoneNumber by remember { mutableStateOf("") }
            var smsCode by remember { mutableStateOf("") }
            var verificationId by remember { mutableStateOf<String?>(null) }
            var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
            var phoneAuthError by remember { mutableStateOf<String?>(null) }
            var isPhoneAuthLoading by remember { mutableStateOf(false) }
            val phoneFailureMessage = stringResource(id = R.string.phone_sign_in_failed)
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.factory(
                    appState.userRepository,
                    appState.surveyRepository
                )
            )
            fun resetPhoneAuthState() {
                countryCode = defaultCountryCode
                phoneNumber = ""
                smsCode = ""
                verificationId = null
                resendToken = null
                phoneAuthError = null
                isPhoneAuthLoading = false
            }
            fun handlePhoneCredential(credential: PhoneAuthCredential) {
                coroutineScope.launch {
                    try {
                        isPhoneAuthLoading = true
                        phoneAuthError = null
                        firebaseAuth.signInWithCredential(credential).await()
                        authViewModel.finalizeFirebaseLogin(
                            mode = FirebaseLoginMode.PHONE,
                            onSuccess = {
                                isPhoneAuthLoading = false
                                showPhoneDialog = false
                                resetPhoneAuthState()
                                Log.d("PhoneAuth", "Phone sign-in successful, navigating to Feed")
                                navController.navigate(QzoneDestination.Feed.route) {
                                    popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                                }
                            },
                            onFailure = { message ->
                                isPhoneAuthLoading = false
                                val error = message ?: phoneFailureMessage
                                phoneAuthError = error
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("PhoneAuth", "signInWithCredential failed", e)
                        isPhoneAuthLoading = false
                        val error = e.localizedMessage ?: phoneFailureMessage
                        phoneAuthError = error
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val phoneAuthCallbacks = remember(phoneFailureMessage, context) {
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d("PhoneAuth", "Verification completed automatically")
                        smsCode = credential.smsCode.orEmpty()
                        handlePhoneCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("PhoneAuth", "Verification failed", e)
                        isPhoneAuthLoading = false
                        val error = e.localizedMessage ?: phoneFailureMessage
                        phoneAuthError = error
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }

                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        Log.d("PhoneAuth", "Code sent, verificationId=$id")
                        verificationId = id
                        resendToken = token
                        smsCode = ""
                        isPhoneAuthLoading = false
                        phoneAuthError = null
                    }
                }
            }
            fun requestVerificationCode(useResendToken: Boolean = false) {
                val trimmedCode = countryCode.trim()
                val trimmedNumber = phoneNumber.trim()
                Log.d("PhoneAuth", "Requesting verification code. useResendToken=$useResendToken rawCode=$trimmedCode rawNumber=$trimmedNumber")
                if (trimmedCode.isBlank()) {
                    phoneAuthError = context.getString(R.string.country_code_required)
                    Log.w("PhoneAuth", "Country code empty, aborting verification request")
                    return
                }
                if (trimmedNumber.isBlank()) {
                    phoneAuthError = context.getString(R.string.phone_number_required)
                    Log.w("PhoneAuth", "Phone number empty, aborting verification request")
                    return
                }
                val normalizedCode = if (trimmedCode.startsWith("+")) trimmedCode else "+$trimmedCode"
                val normalizedNumber = trimmedNumber.filterNot { it.isWhitespace() }
                val fullNumber = normalizedCode + normalizedNumber
                Log.d("PhoneAuth", "Normalized phone=$fullNumber")
                val activity = context as? ComponentActivity
                if (activity == null) {
                    Toast.makeText(context, phoneFailureMessage, Toast.LENGTH_SHORT).show()
                    Log.e("PhoneAuth", "Context is not a ComponentActivity, cannot start verification")
                    return
                }
                isPhoneAuthLoading = true
                phoneAuthError = null
                val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(fullNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                if (useResendToken) {
                    resendToken?.let { optionsBuilder.setForceResendingToken(it) }
                        ?: Log.w("PhoneAuth", "Resend requested but token is null")
                }
                Log.d("PhoneAuth", "Calling verifyPhoneNumber for $fullNumber")
                PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
            }
            fun submitVerificationCode() {
                val code = smsCode.trim()
                Log.d("PhoneAuth", "Submitting verification code: $code")
                if (code.isEmpty()) {
                    phoneAuthError = context.getString(R.string.sms_code_required)
                    Log.w("PhoneAuth", "Verification code empty, aborting submit")
                    return
                }
                val id = verificationId
                if (id.isNullOrEmpty()) {
                    phoneAuthError = phoneFailureMessage
                    Log.e("PhoneAuth", "VerificationId is null, cannot submit code")
                    return
                }
                Log.d("PhoneAuth", "Creating credential with verificationId=$id")
                val credential = PhoneAuthProvider.getCredential(id, code)
                handlePhoneCredential(credential)
            }
            val googleSignInClient = remember(context) {
                val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, options)
            }
            val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d("GoogleSignIn", "ActivityResult received: resultCode=${result.resultCode}")
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val idToken = account?.idToken
                    Log.d("GoogleSignIn", "Google account retrieved. ID Token present: ${idToken != null}")
                    if (idToken != null) {
                        authViewModel.signInWithGoogle(
                            idToken,
                            onSuccess = {
                                Log.d("GoogleSignIn", "Navigating to Feed after success")
                                navController.navigate(QzoneDestination.Feed.route) {
                                    popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                                }
                            },
                            onFailure = { message ->
                                Log.e("GoogleSignIn", "Sign in failed: $message")
                                Toast.makeText(
                                    context,
                                    message ?: context.getString(R.string.google_sign_in_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        Log.e("GoogleSignIn", "ID Token is null")
                        Toast.makeText(
                            context,
                            context.getString(R.string.google_sign_in_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: com.google.android.gms.common.api.ApiException) {
                    Log.e("GoogleSignIn", "ApiException: code=${e.statusCode}, message=${e.message}", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.google_sign_in_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            if (showPhoneDialog) {
                PhoneAuthDialog(
                    phoneNumber = phoneNumber,
                    onPhoneNumberChanged = {
                        phoneNumber = it
                        phoneAuthError = null
                    },
                    countryCode = countryCode,
                    onCountryCodeChanged = {
                        countryCode = it
                        phoneAuthError = null
                    },
                    smsCode = smsCode,
                    onSmsCodeChanged = {
                        smsCode = it
                        phoneAuthError = null
                    },
                    isCodeSent = verificationId != null,
                    isLoading = isPhoneAuthLoading,
                    errorMessage = phoneAuthError,
                    onDismiss = {
                        if (!isPhoneAuthLoading) {
                            showPhoneDialog = false
                            resetPhoneAuthState()
                        }
                    },
                    onPrimaryAction = {
                        if (verificationId == null) {
                            requestVerificationCode()
                        } else {
                            submitVerificationCode()
                        }
                    },
                    onResend = { requestVerificationCode(useResendToken = true) }
                )
            }
            SignInScreen(
                state = authViewModel.uiState,
                onEmailChanged = authViewModel::onEmailChanged,
                onPasswordChanged = authViewModel::onPasswordChanged,
                onSignIn = {
                    authViewModel.signIn {
                        navController.navigate(QzoneDestination.Feed.route) {
                            popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onGoogleSignIn = {
                    Log.d("GoogleSignIn", "Launching Google Sign-In intent")
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onPhoneSignIn = {
                    phoneAuthError = null
                    showPhoneDialog = true
                },
                onNavigateToRegister = { navController.navigate(QzoneDestination.Register.route) }
            )
        }
        composable(QzoneDestination.Register.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.factory(
                    appState.userRepository,
                    appState.surveyRepository
                )
            )
            RegisterScreen(
                state = authViewModel.registerState,
                onUsernameChanged = authViewModel::onRegisterUsernameChanged,
                onEmailChanged = authViewModel::onRegisterEmailChanged,
                onPasswordChanged = authViewModel::onRegisterPasswordChanged,
                onRegister = {
                    authViewModel.register {
                        navController.navigate(QzoneDestination.Feed.route) {
                            popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.Feed.route) {
            val feedViewModel: FeedViewModel = viewModel(
                factory = FeedViewModel.factory(
                    appState.surveyRepository,
                    appState.locationRepository,
                    appState.localSurveyRepository
                )
            )
            FeedScreen(
                state = feedViewModel.uiState,
                onRefresh = feedViewModel::refresh,
                onSurveySelected = { surveyId ->
                    navController.navigate(QzoneDestination.SurveyDetail.createRoute(surveyId))
                },
                onLocationPermissionGranted = feedViewModel::onLocationPermissionGranted,
                locationRepository = appState.locationRepository
            )
        }
        composable(QzoneDestination.NearbyMap.route) {
            val mapViewModel: NearbyMapViewModel = viewModel(
                factory = NearbyMapViewModel.factory(
                    appState.locationRepository,
                    appState.localSurveyRepository,
                    appState.surveyRepository
                )
            )
            val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
            NearbySurveyMapScreen(
                state = mapState,
                onRefresh = mapViewModel::refresh,
                onLocationPermissionGranted = mapViewModel::onLocationPermissionGranted,
                onSurveySelected = { surveyId ->
                    navController.navigate(QzoneDestination.SurveyDetail.createRoute(surveyId))
                }
            )
        }
        composable(
            route = QzoneDestination.SurveyDetail.route,
            arguments = listOf(navArgument(QzoneDestination.SurveyDetail.surveyIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString(QzoneDestination.SurveyDetail.surveyIdArg).orEmpty()
            val surveyViewModel: SurveyViewModel = viewModel(
                factory = SurveyViewModel.factory(
                    appState.surveyRepository,
                    appState.userRepository,
                    surveyId,
                    appState.localSurveyRepository
                )
            )
            SurveyScreen(
                state = surveyViewModel.uiState,
                onPrevious = surveyViewModel::onPrevious,
                onNext = surveyViewModel::onNext,
                onClose = {
                    surveyViewModel.cacheProgress {
                        navController.popBackStack()
                    }
                },
                onSubmit = { surveyViewModel.submit() },
                onCompletionAcknowledged = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(QzoneDestination.Feed.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
                onAnswerChanged = surveyViewModel::onAnswerChanged
            )
        }
        composable(
            route = QzoneDestination.CompletedSurveyDetail.route,
            arguments = listOf(
                navArgument(QzoneDestination.CompletedSurveyDetail.surveyIdArg) { type = NavType.StringType },
                navArgument(QzoneDestination.CompletedSurveyDetail.responseIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val responseId = backStackEntry.arguments?.getString(QzoneDestination.CompletedSurveyDetail.responseIdArg).orEmpty()
            val historyDetailViewModel: HistoryDetailViewModel = viewModel(
                factory = HistoryDetailViewModel.factory(appState.surveyRepository, responseId)
            )
            HistoryDetailScreen(
                state = historyDetailViewModel.uiState,
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.Profile.route) {
            val context = LocalContext.current
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(
                    appState.userRepository,
                    appState.rewardRepository,
                    appState.localSurveyRepository,
                    appState.surveyRepository
                )
            )
            val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
                        val filename = "avatar_${System.currentTimeMillis()}.$extension"
                        profileViewModel.uploadAvatar(bytes, mime, filename) { success, message ->
                            Toast.makeText(
                                context,
                                message ?: if (success) "Avatar updated" else "Failed to update avatar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "Unable to read selected image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ProfileScreen(
                state = profileViewModel.uiState,
                onViewRewards = { appState.navigateTopLevel(QzoneDestination.Rewards) },
                onHistoryClick = { appState.navigateTopLevel(QzoneDestination.History) },
                onOpenSettings = { navController.navigate(QzoneDestination.ProfileSettings.route) },
                onWalletClick = { navController.navigate(QzoneDestination.Wallet.route) },
                onAvatarClick = { avatarPicker.launch("image/*") }
            )
        }
        composable(QzoneDestination.Wallet.route) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(QzoneDestination.Profile.route)
            }
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = ProfileViewModel.factory(
                    appState.userRepository,
                    appState.rewardRepository,
                    appState.localSurveyRepository,
                    appState.surveyRepository
                )
            )
            WalletScreen(
                state = profileViewModel.uiState,
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.EditProfile.route) {
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(
                    appState.userRepository,
                    appState.rewardRepository,
                    appState.localSurveyRepository,
                    appState.surveyRepository
                )
            )
            EditProfileScreen(
                state = profileViewModel.editState,
                onNameChanged = profileViewModel::onNameChanged,
                onEmailChanged = profileViewModel::onEmailChanged,
                onPasswordChanged = profileViewModel::onPasswordChanged,
                onCountryChanged = profileViewModel::onCountryChanged,
                onSave = {
                    profileViewModel.saveEdits()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.ProfileSettings.route) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val firebaseAuth = remember { FirebaseAuth.getInstance() }
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(
                    appState.userRepository,
                    appState.rewardRepository,
                    appState.localSurveyRepository,
                    appState.surveyRepository
                )
            )
            var showPhoneDialog by remember { mutableStateOf(false) }
            var phoneCountryCode by remember { mutableStateOf("+1") }
            var phoneInput by remember { mutableStateOf("") }
            var phoneVerificationCode by remember { mutableStateOf("") }
            var linkVerificationId by remember { mutableStateOf<String?>(null) }
            var linkResendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
            var isCodeSent by remember { mutableStateOf(false) }
            var phoneError by remember { mutableStateOf<String?>(null) }
            var isPhoneSubmitting by remember { mutableStateOf(false) }
            var isSendingCode by remember { mutableStateOf(false) }
            val phoneLinkCallbacks = remember(context) {
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        phoneVerificationCode = credential.smsCode.orEmpty()
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("PhoneLink", "Verification failed", e)
                        isSendingCode = false
                        phoneError = e.localizedMessage ?: context.getString(R.string.phone_sign_in_failed)
                    }

                    override fun onCodeSent(
                        id: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("PhoneLink", "Code sent for phone link")
                        linkVerificationId = id
                        linkResendToken = token
                        isCodeSent = true
                        isSendingCode = false
                        phoneError = null
                    }
                }
            }
            fun resetPhoneLinkState() {
                phoneCountryCode = "+1"
                phoneInput = ""
                phoneVerificationCode = ""
                linkVerificationId = null
                linkResendToken = null
                isCodeSent = false
                phoneError = null
                isPhoneSubmitting = false
                isSendingCode = false
            }
            val dismissPhoneDialog = {
                resetPhoneLinkState()
                showPhoneDialog = false
            }
            fun sendPhoneVerification(useResend: Boolean = false) {
                val trimmedCode = phoneCountryCode.trim()
                val trimmedNumber = phoneInput.trim()
                if (trimmedCode.isBlank()) {
                    phoneError = context.getString(R.string.country_code_required)
                    return
                }
                if (trimmedNumber.isBlank()) {
                    phoneError = context.getString(R.string.phone_number_required)
                    return
                }
                val normalizedCode = if (trimmedCode.startsWith("+")) trimmedCode else "+$trimmedCode"
                val normalizedNumber = trimmedNumber.filterNot { it.isWhitespace() }
                val fullNumber = normalizedCode + normalizedNumber
                val activity = context as? ComponentActivity
                if (activity == null) {
                    phoneError = context.getString(R.string.phone_sign_in_failed)
                    return
                }
                isSendingCode = true
                phoneError = null
                val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(fullNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneLinkCallbacks)
                if (useResend) {
                    linkResendToken?.let { optionsBuilder.setForceResendingToken(it) }
                }
                try {
                    PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Log.e("PhoneLink", "Failed to request code", t)
                    isSendingCode = false
                    phoneError = t.localizedMessage ?: context.getString(R.string.phone_sign_in_failed)
                }
            }
            ProfileSettingsScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(QzoneDestination.EditProfile.route) },
                isDarkTheme = appState.useDarkTheme,
                onToggleDarkMode = appState.onToggleDarkTheme,
                onLogout = {
                    profileViewModel.signOut {
                        navController.navigate(QzoneDestination.SignIn.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                        resetPhoneLinkState()
                    }
                },
                onAddPhoneNumber = { showPhoneDialog = true }
            )
            if (showPhoneDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isPhoneSubmitting) {
                            dismissPhoneDialog()
                        }
                    },
                    title = { Text(text = stringResource(id = R.string.phone_add_title)) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.phone_add_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = phoneCountryCode,
                                    onValueChange = {
                                        phoneCountryCode = it
                                        phoneError = null
                                    },
                                    label = { Text(text = stringResource(id = R.string.country_code_hint)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = {
                                        phoneInput = it
                                        phoneError = null
                                    },
                                    label = { Text(text = stringResource(id = R.string.phone_number_hint)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(2f)
                                )
                            }
                            OutlinedTextField(
                                value = phoneVerificationCode,
                                onValueChange = {
                                    phoneVerificationCode = it
                                    phoneError = null
                                },
                                label = { Text(text = stringResource(id = R.string.sms_code_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = isCodeSent,
                                supportingText = {
                                    if (!isCodeSent) {
                                        Text(text = stringResource(id = R.string.phone_send_code_first))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(
                                onClick = { sendPhoneVerification(isCodeSent) },
                                enabled = !isSendingCode
                            ) {
                                if (isSendingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(
                                            id = if (isCodeSent) R.string.resend_code else R.string.send_code
                                        )
                                    )
                                }
                            }
                            phoneError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmedCode = phoneCountryCode.trim()
                                val trimmedNumber = phoneInput.trim()
                                if (trimmedCode.isEmpty()) {
                                    phoneError = context.getString(R.string.country_code_required)
                                    return@TextButton
                                }
                                if (trimmedNumber.isEmpty()) {
                                    phoneError = context.getString(R.string.phone_number_required)
                                    return@TextButton
                                }
                                if (!isCodeSent) {
                                    phoneError = context.getString(R.string.phone_send_code_first)
                                    return@TextButton
                                }
                                val verificationId = linkVerificationId
                                if (verificationId.isNullOrEmpty()) {
                                    phoneError = context.getString(R.string.phone_sign_in_failed)
                                    return@TextButton
                                }
                                val codeValue = phoneVerificationCode.trim()
                                if (codeValue.isEmpty()) {
                                    phoneError = context.getString(R.string.sms_code_required)
                                    return@TextButton
                                }
                                val currentUser = firebaseAuth.currentUser
                                if (currentUser == null) {
                                    phoneError = context.getString(R.string.phone_sign_in_failed)
                                    return@TextButton
                                }
                                val normalizedCode = if (trimmedCode.startsWith("+")) trimmedCode else "+$trimmedCode"
                                val normalizedNumber = trimmedNumber.filterNot { it.isWhitespace() }
                                val fullNumber = normalizedCode + normalizedNumber
                                isPhoneSubmitting = true
                                phoneError = null
                                coroutineScope.launch {
                                    try {
                                        val credential = PhoneAuthProvider.getCredential(verificationId, codeValue)
                                        currentUser.linkWithCredential(credential).await()
                                        profileViewModel.linkPhoneNumber(fullNumber) { success, message ->
                                            isPhoneSubmitting = false
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.phone_add_success),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                dismissPhoneDialog()
                                            } else {
                                                phoneError = message ?: context.getString(R.string.phone_sign_in_failed)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (e is CancellationException) throw e
                                        Log.e("PhoneLink", "Failed to link phone", e)
                                        isPhoneSubmitting = false
                                        phoneError = e.localizedMessage ?: context.getString(R.string.phone_sign_in_failed)
                                    }
                                }
                            },
                            enabled = !isPhoneSubmitting
                        ) {
                            if (isPhoneSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = "Confirm")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                if (!isPhoneSubmitting) {
                                    dismissPhoneDialog()
                                }
                            },
                            enabled = !isPhoneSubmitting
                        ) {
                            Text(text = "Cancel")
                        }
                    }
                )
            }
        }
        composable(QzoneDestination.History.route) {
            val context = LocalContext.current
            val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(appState.surveyRepository))
            HistoryScreen(
                state = historyViewModel.uiState,
                onQueryChanged = historyViewModel::onQueryChange,
                onInProgressSurveyClick = { surveyId ->
                    navController.navigate(QzoneDestination.SurveyDetail.createRoute(surveyId))
                },
                onCompletedSurveyClick = { survey ->
                    val responseId = survey.responseId
                    if (responseId.isNullOrBlank()) {
                        Toast.makeText(context, "No response history available for this survey.", Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate(
                            QzoneDestination.CompletedSurveyDetail.createRoute(survey.id, responseId)
                        )
                    }
                },
                locationRepository = appState.locationRepository
            )
        }
        composable(QzoneDestination.Rewards.route) {
            val context = LocalContext.current
            val rewardsViewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.factory(appState.rewardRepository))
            RewardsScreen(
                state = rewardsViewModel.uiState,
                onRewardSelected = { id -> navController.navigate(QzoneDestination.RewardDetail.createRoute(id)) },
                onRedeem = { reward ->
                    rewardsViewModel.redeemReward(reward) { success, message ->
                        Toast.makeText(
                            context,
                            message ?: if (success) "Redemption successful" else "Redemption failed, please try again later",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        composable(
            route = QzoneDestination.RewardDetail.route,
            arguments = listOf(navArgument(QzoneDestination.RewardDetail.rewardIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val rewardId = backStackEntry.arguments?.getString(QzoneDestination.RewardDetail.rewardIdArg).orEmpty()
            val rewardDetailViewModel: RewardDetailViewModel = viewModel(factory = RewardDetailViewModel.factory(appState.rewardRepository, rewardId))
            RewardDetailScreen(
                state = rewardDetailViewModel.uiState,
                onBack = { navController.popBackStack() },
                onRedeem = { /* Placeholder for backend integration */ }
            )
        }
    }
}

@Composable
private fun PhoneAuthDialog(
    phoneNumber: String,
    onPhoneNumberChanged: (String) -> Unit,
    countryCode: String,
    onCountryCodeChanged: (String) -> Unit,
    smsCode: String,
    onSmsCodeChanged: (String) -> Unit,
    isCodeSent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onResend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onPrimaryAction,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(
                            id = if (isCodeSent) R.string.verify_code else R.string.send_code
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = { Text(text = stringResource(id = R.string.phone_sign_in_title)) },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = onCountryCodeChanged,
                        label = { Text(text = stringResource(id = R.string.country_code_hint)) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.width(96.dp)
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChanged,
                        label = { Text(text = stringResource(id = R.string.phone_number_hint)) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isCodeSent) {
                    OutlinedTextField(
                        value = smsCode,
                        onValueChange = onSmsCodeChanged,
                        label = { Text(text = stringResource(id = R.string.sms_code_hint)) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isCodeSent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onResend,
                        enabled = !isLoading
                    ) {
                        Text(text = stringResource(id = R.string.resend_code))
                    }
                }
            }
        }
    )
}
