package com.example.settled.domain.model

object SupportedCardsRegistry {
    val banks = listOf(
        "HDFC Bank",
        "ICICI Bank",
        "Axis Bank",
        "SBI Card",
        "Kotak Mahindra",
        "American Express"
    )

    fun getCardsForBank(bankName: String): List<String> {
        return when (bankName) {
            "HDFC Bank" -> listOf("Regalia", "Millennia", "Infinia", "Diners Club Black", "MoneyBack")
            "ICICI Bank" -> listOf("Amazon Pay", "Coral", "Sapphiro", "Rubyx")
            "Axis Bank" -> listOf("Magnus", "Ace", "Flipkart", "Vistara")
            "SBI Card" -> listOf("SimplyCLICK", "SimplySAVE", "Prime", "Elite")
            "Kotak Mahindra" -> listOf("League Platinum", "Mojo", "Zen")
            "American Express" -> listOf("Platinum Charge", "Gold", "SmartEarn")
            else -> emptyList()
        }
    }
}
