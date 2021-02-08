package technology.nine.cred.model

data class EmiDetails(
    val amount: Double,
    val months: Int,
    var isChecked: Boolean,
    var bg_color: Int,
    var checked_color: Int
)