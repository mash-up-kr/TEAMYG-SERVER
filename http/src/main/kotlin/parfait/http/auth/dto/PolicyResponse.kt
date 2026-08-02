package parfait.http.auth.dto

data class PolicyResponse(
    val policies: List<PolicyItemResponse>,
)

data class PolicyItemResponse(
    val termsId: Long,
    val type: String,
    val title: String,
    val url: String,
    val required: Boolean,
)
