package ir.amir.triedgame.model

enum class LifeEventKind { BONUS, EXPENSE }

data class LifeEvent(
    val id: String,
    val title: String,
    val description: String,
    val kind: LifeEventKind,
    val amountToman: Double
)

object LifeEventCatalog {
    val all: List<LifeEvent> = listOf(
        LifeEvent("job_offer", "پیشنهاد کاری", "یه کار جانبی پیدا کردی و بهت پول رسید!", LifeEventKind.BONUS, 200_000.0),
        LifeEvent("lucky_bonus", "شانس بلند", "توی خیابون یه بلیط بخت‌آزمایی کوچیک بردی!", LifeEventKind.BONUS, 120_000.0),
        LifeEvent("gift", "هدیه‌ی غیرمنتظره", "یکی از آشناها بهت هدیه داد.", LifeEventKind.BONUS, 150_000.0),
        LifeEvent("car_trouble", "خرابی ماشین", "ماشینت یهو خراب شد و باید تعمیرش کنی.", LifeEventKind.EXPENSE, 300_000.0),
        LifeEvent("unexpected_bill", "قبض غیرمنتظره", "یه قبض یادت رفته بود که باید پرداخت بشه.", LifeEventKind.EXPENSE, 180_000.0),
        LifeEvent("phone_repair", "تعمیر گوشی", "صفحه‌ی گوشیت ترک خورد.", LifeEventKind.EXPENSE, 250_000.0)
    )

    fun random(): LifeEvent = all.random()
}
