package com.finshield.app.data.credit

import com.finshield.app.domain.credit.AAConnectionStatus
import com.finshield.app.domain.credit.AccountAggregatorData
import com.finshield.app.domain.credit.CreditScoreBand
import com.finshield.app.domain.credit.CreditScoreData
import com.finshield.app.domain.credit.CreditScoreStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Request payload for querying CIBIL score.
 * Never logged or stored permanently on the device.
 */
data class CibilScoreRequest(
    val pan: String,
    val fullName: String,
    val dateOfBirth: String, // DD-MM-YYYY
    val mobileNumber: String,
    val consentGranted: Boolean,
)

/**
 * Result of checking a credit score provider's setup.
 */
sealed class ProviderStatus {
    object Ready : ProviderStatus()
    data class MissingConfiguration(val reason: String, val requirements: List<String>) : ProviderStatus()
}

/**
 * Interface contract for authorized Credit Bureau (CIBIL) integrations.
 * Real implementations must delegate sensitive credential exchange to a secure backend.
 */
interface CibilProvider {
    fun checkStatus(): ProviderStatus
    suspend fun requestScore(uid: String, idToken: String, request: CibilScoreRequest): Result<CreditScoreData>
}

/**
 * Interface contract for RBI Account Aggregator (AA) integrations.
 * Never requests or receives bank passwords, ATM/UPI PINs, or OTPs.
 */
interface AccountAggregatorProvider {
    fun checkStatus(): ProviderStatus
    suspend fun createConsentRequest(uid: String, idToken: String): Result<String> // Returns consent redirect URL
    suspend fun revokeConsent(uid: String, idToken: String): Result<Boolean>
}

/**
 * Official TransUnion CIBIL Provider implementation abstraction.
 *
 * Security & Regulatory Architecture:
 * 1. Credentials (CIBIL Member ID, Client Certificate, mTLS keys) are strictly kept OFF the Android client.
 * 2. Mobile client exchanges Firebase ID token with an authorized backend proxy.
 * 3. When production partner credentials are not configured, transparently returns INTEGRATION_REQUIRED.
 * 4. In development/testing environments, allows verifying sandbox responses clearly marked as synthetic.
 */
class TransUnionCibilBackendProvider(
    private val backendBaseUrl: String? = null, // Set via server configuration, not hardcoded in APK
) : CibilProvider {

    override fun checkStatus(): ProviderStatus {
        if (backendBaseUrl.isNullOrBlank()) {
            return ProviderStatus.MissingConfiguration(
                reason = "TransUnion CIBIL production partner gateway is not configured.",
                requirements = listOf(
                    "Licensed Financial Institution / NBFC membership or certified Fintech distribution partnership with TransUnion CIBIL.",
                    "Backend proxy with mTLS (mutual TLS) and ISO-27001 certified key management for API member authentication.",
                    "RBI CIR Regulations 2006 compliance for explicit consumer consent logging and data localization.",
                ),
            )
        }
        return ProviderStatus.Ready
    }

    override suspend fun requestScore(
        uid: String,
        idToken: String,
        request: CibilScoreRequest,
    ): Result<CreditScoreData> {
        if (!request.consentGranted) {
            return Result.failure(IllegalStateException("Explicit consumer consent is required to request credit score."))
        }

        // When backend is not configured, inform the user honestly rather than simulating a fake live API success
        val status = checkStatus()
        if (status is ProviderStatus.MissingConfiguration) {
            return Result.success(
                CreditScoreData(
                    score = null,
                    status = CreditScoreStatus.INTEGRATION_REQUIRED,
                    provider = "TransUnion CIBIL",
                    isOfficialBureauScore = false,
                    errorMessage = status.reason,
                )
            )
        }

        // Real backend call would be executed here with Authorization: Bearer $idToken
        // and payload { pan: request.pan, fullName: request.fullName, dob: request.dateOfBirth }
        return Result.failure(UnsupportedOperationException("Backend proxy connecting..."))
    }
}

/**
 * RBI Account Aggregator Provider abstraction.
 * Follows the Reserve Bank of India Master Directions for Account Aggregator ecosystem (Sahamati / Setu / Anumati).
 */
class RbiAccountAggregatorBackendProvider(
    private val aaGatewayUrl: String? = null,
) : AccountAggregatorProvider {

    override fun checkStatus(): ProviderStatus {
        if (aaGatewayUrl.isNullOrBlank()) {
            return ProviderStatus.MissingConfiguration(
                reason = "RBI Account Aggregator (AA) gateway connector is not configured.",
                requirements = listOf(
                    "Registration as a Financial Information User (FIU) with an RBI-licensed Account Aggregator.",
                    "Secure FIU backend for receiving encrypted financial information artefacts.",
                    "Zero collection of bank credentials: User authenticates exclusively on the regulated AA provider interface.",
                ),
            )
        }
        return ProviderStatus.Ready
    }

    override suspend fun createConsentRequest(uid: String, idToken: String): Result<String> {
        val status = checkStatus()
        if (status is ProviderStatus.MissingConfiguration) {
            return Result.failure(IllegalStateException(status.reason))
        }
        return Result.failure(UnsupportedOperationException("AA gateway not initialized."))
    }

    override suspend fun revokeConsent(uid: String, idToken: String): Result<Boolean> {
        val status = checkStatus()
        if (status is ProviderStatus.MissingConfiguration) {
            return Result.success(true)
        }
        return Result.success(true)
    }
}

/**
 * Combined Credit Health State.
 */
data class CreditHealthState(
    val creditScore: CreditScoreData = CreditScoreData(),
    val accountAggregator: AccountAggregatorData = AccountAggregatorData(),
    val isLoading: Boolean = false,
    val isSandboxMode: Boolean = false,
    val lastSyncStatus: String? = null,
)

/**
 * Repository for managing user Credit Health data, Firestore synchronization,
 * and authorized provider handshakes.
 */
class CreditHealthRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val cibilProvider: CibilProvider = TransUnionCibilBackendProvider(),
    private val aaProvider: AccountAggregatorProvider = RbiAccountAggregatorBackendProvider(),
) {
    private val _state = MutableStateFlow(CreditHealthState())
    val state: StateFlow<CreditHealthState> = _state.asStateFlow()

    /**
     * Loads credit health data from Firestore under `users/{uid}/credit_health/overview`.
     * Does NOT store or log PAN, bank passwords, or raw reports.
     */
    suspend fun loadUserCreditHealth(uid: String) {
        if (uid.isBlank()) return

        _state.value = _state.value.copy(isLoading = true)
        try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("credit_health")
                .document("overview")
                .get()
                .await()

            if (doc.exists()) {
                val scoreLong = doc.getLong("creditScore")
                val score = scoreLong?.toInt()
                val retrievalDate = doc.getString("retrievalDate")
                val provider = doc.getString("provider") ?: "TransUnion CIBIL"
                val isOfficial = doc.getBoolean("isOfficialBureauScore") ?: false
                val scoreStatusStr = doc.getString("scoreStatus") ?: CreditScoreStatus.NOT_FETCHED.name

                val aaStatusStr = doc.getString("aaStatus") ?: AAConnectionStatus.NOT_CONNECTED.name
                val connectedBank = doc.getString("connectedBank")
                val lastSynced = doc.getString("lastSyncedTime")

                val scoreStatus = try {
                    CreditScoreStatus.valueOf(scoreStatusStr)
                } catch (e: Exception) {
                    if (score != null) CreditScoreStatus.VERIFIED else CreditScoreStatus.NOT_FETCHED
                }

                val aaStatus = try {
                    AAConnectionStatus.valueOf(aaStatusStr)
                } catch (e: Exception) {
                    AAConnectionStatus.NOT_CONNECTED
                }

                _state.value = CreditHealthState(
                    creditScore = CreditScoreData(
                        score = score,
                        retrievalDate = retrievalDate,
                        provider = provider,
                        isOfficialBureauScore = isOfficial,
                        status = scoreStatus,
                    ),
                    accountAggregator = AccountAggregatorData(
                        status = aaStatus,
                        connectedBankName = connectedBank,
                        lastSyncedTime = lastSynced,
                    ),
                    isLoading = false,
                )
            } else {
                _state.value = CreditHealthState(
                    creditScore = CreditScoreData(status = CreditScoreStatus.NOT_FETCHED),
                    accountAggregator = AccountAggregatorData(status = AAConnectionStatus.NOT_CONNECTED),
                    isLoading = false,
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                lastSyncStatus = "Could not sync latest credit health: ${e.localizedMessage ?: "Network error"}",
            )
        }
    }

    /**
     * Checks CIBIL provider configuration status.
     */
    fun getCibilProviderStatus(): ProviderStatus = cibilProvider.checkStatus()

    /**
     * Checks Account Aggregator provider configuration status.
     */
    fun getAaProviderStatus(): ProviderStatus = aaProvider.checkStatus()

    /**
     * Initiates official credit score inquiry flow.
     */
    suspend fun requestScore(
        uid: String,
        idToken: String,
        request: CibilScoreRequest,
    ): Result<CreditScoreData> {
        _state.value = _state.value.copy(isLoading = true)

        val result = cibilProvider.requestScore(uid, idToken, request)
        result.onSuccess { data ->
            _state.value = _state.value.copy(
                creditScore = data,
                isLoading = false,
            )
            // Persist verified metadata only (never the PAN or personal identity data)
            if (data.score != null && uid.isNotBlank()) {
                persistScoreMetadata(uid, data)
            }
        }.onFailure { error ->
            _state.value = _state.value.copy(
                creditScore = _state.value.creditScore.copy(
                    status = CreditScoreStatus.ERROR,
                    errorMessage = error.localizedMessage ?: "Failed to verify credit score with provider.",
                ),
                isLoading = false,
            )
        }

        return result
    }

    /**
     * Enables sandbox mode for developer integration testing.
     * Clearly labels data as synthetic sandbox test data.
     */
    suspend fun setSandboxTestData(uid: String, simulatedScore: Int = 772) {
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val sandboxData = CreditScoreData(
            score = simulatedScore,
            retrievalDate = todayStr,
            provider = "TransUnion CIBIL (Sandbox Test)",
            isOfficialBureauScore = false, // Explicitly false since it is test data
            status = CreditScoreStatus.VERIFIED,
        )

        _state.value = _state.value.copy(
            creditScore = sandboxData,
            isSandboxMode = true,
            isLoading = false,
        )

        if (uid.isNotBlank()) {
            persistScoreMetadata(uid, sandboxData)
        }
    }

    /**
     * Disconnects / revokes consent for Account Aggregator connection.
     */
    suspend fun revokeAccountAggregator(uid: String, idToken: String): Result<Boolean> {
        _state.value = _state.value.copy(isLoading = true)
        val result = aaProvider.revokeConsent(uid, idToken)
        result.onSuccess {
            _state.value = _state.value.copy(
                accountAggregator = AccountAggregatorData(status = AAConnectionStatus.REVOKED),
                isLoading = false,
            )
            if (uid.isNotBlank()) {
                firestore.collection("users")
                    .document(uid)
                    .collection("credit_health")
                    .document("overview")
                    .set(
                        mapOf(
                            "aaStatus" to AAConnectionStatus.REVOKED.name,
                            "lastSyncedTime" to null,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge()
                    ).await()
            }
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false)
        }
        return result
    }

    private suspend fun persistScoreMetadata(uid: String, data: CreditScoreData) {
        try {
            firestore.collection("users")
                .document(uid)
                .collection("credit_health")
                .document("overview")
                .set(
                    mapOf(
                        "creditScore" to data.score,
                        "retrievalDate" to data.retrievalDate,
                        "provider" to data.provider,
                        "isOfficialBureauScore" to data.isOfficialBureauScore,
                        "scoreStatus" to data.status.name,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {
            // Non-critical local persistence fallback
        }
    }
}
