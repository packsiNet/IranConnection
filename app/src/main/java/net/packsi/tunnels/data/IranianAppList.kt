package net.packsi.tunnels.data

import net.packsi.tunnels.data.subscription.CatalogApp

object IranianAppList {

    /** Package names in the free tier — shown at top, no paywall. */
    val FREE_PACKAGES = setOf(
        "com.samanpr.blu",
    )

    /** Ordered category labels for premium section grouping. */
    val CATEGORY_ORDER = listOf("Banks", "Finance", "Telecom", "Shopping", "Insurance", "Social", "Services")

    val apps = listOf(
        // ─── Free ────────────────────────────────────────────────────────────
        IranianApp("com.samanpr.blu",                            "Blu Bank",       "بلوبانک"),
        IranianApp("com.samanpr.blujr",                          "Blu Bank Junior", "جونیور بلوبانک"),

        // ─── Banks ───────────────────────────────────────────────────────────
        IranianApp("ir.bmi.bam.nativeweb",                       "Bank Melli",     "بانک ملی",       "Banks"),
        IranianApp("com.saman.singlewindow",                     "Saman Bank",     "بانک سامان",     "Banks"),
        IranianApp("ir.mobillet.app",                            "Mobilet Saman",  "موبایلت سامان",  "Banks"),
        IranianApp("ir.tejaratbank.tata.mobile.android.tejarat", "Bank Tejarat",   "بانک تجارت",     "Banks"),
        IranianApp("com.refahbank.dpi.android",                  "Bank Refah",     "بانک رفاه",      "Banks"),
        IranianApp("com.isc.bsinew",                             "Bank Saderat",   "بانک صادرات",    "Banks"),
        IranianApp("com.ada.mbank.mehr",                         "Bank Mehr Iran", "بانک مهر ایران", "Banks"),
        IranianApp("ir.omidbank",                                "Bank Omid",      "بانک امید",      "Banks"),
        IranianApp("com.pmb.mobile",                             "Bank Maskan",    "بانک مسکن",      "Banks"),
        IranianApp("com.tosan.dara.postbank",                    "Post Bank",      "پست بانک",       "Banks"),
        IranianApp("digital.neobank",                            "Neo Bank",       "نئوبانک",        "Banks"),
        IranianApp("com.bki.mobilebanking.android",              "Bank Karafarin", "بانک کارآفرین",  "Banks"),
        IranianApp("mob.banking.android.taavon",                 "Bank Taavon",    "بانک توسعه تعاون","Banks"),
        IranianApp("ir.izbank.omnichannel",                      "IZ Bank",        "ایران زمین",     "Banks"),
        IranianApp("mob.banking.android.resalat",                "Bank Resalat",   "بانک رسالت",     "Banks"),
        IranianApp("co.redbank.app",                             "Red Bank",       "رد بانک",        "Banks"),
        IranianApp("com.tosan.dara.day",                         "Bank Day",       "بانک دی",        "Banks"),
        IranianApp("ir.stts.bjt",                                "Bank Jodat",     "بانک جودت",      "Banks"),
        IranianApp("com.tosan.dara.sina",                        "Bank Sina",      "بانک سینا",      "Banks"),
        IranianApp("mob.banking.android.pasargad",               "Bank Pasargad",  "بانک پاسارگاد",  "Banks"),
        IranianApp("com.tosan.dara.mehriran",                    "Bank Mehr Iran", "بانک مهر ایران", "Banks"),
        IranianApp("ir.tes.sarmayeh",                            "Bank Sarmayeh",  "بانک سرمایه",    "Banks"),
        IranianApp("com.farazpardazan.enbank",                   "Hamrah Novin",   "همراه نوین",     "Banks"),
        IranianApp("mob.banking.android.gardesh",                 "Tourism Bank",   "بانک گردشگری",  "Banks"),

        // ─── Finance ─────────────────────────────────────────────────────────
        IranianApp("market.nobitex",                             "Nobitex",        "نوبیتکس",        "Finance"),
        IranianApp("com.emofid.rnmofid",                         "Mofid Mobile",   "موبایل مفید",    "Finance"),
        IranianApp("com.mofidonline.mobile",                     "Mofid Online",   "مفید آنلاین",    "Finance"),
        IranianApp("ir.easytrader.orbis.m.twa",                  "EasyTrader",     "ایزی‌تریدر",     "Finance"),
        IranianApp("com.hamidrezabashiri.ezcard",                 "EzCard",         "ای‌زی کارت",     "Finance"),
        IranianApp("com.mydigipay.app.android",                  "DigiPay",        "دیجی‌پی",        "Finance"),
        IranianApp("co.nilin.faraznative",                       "Faraz",          "فراز",           "Finance"),
        IranianApp("ir.hafhashtad.android780",                   "Haf Hashtad",    "هفت‌هشتاد",      "Finance"),
        IranianApp("com.dotin.wepod",                            "Wepod",          "وپد",            "Finance"),
        IranianApp("ir.sep.sesoot",                              "Sesoot",         "سه‌سوت",         "Finance"),
        IranianApp("com.fam.fam",                                "Fam",            "فام",            "Finance"),

        // ─── Telecom ─────────────────────────────────────────────────────────
        IranianApp("ir.mci.ecareapp",                            "Hamrahe Aval",   "همراه اول",      "Telecom"),
        IranianApp("com.myirancell",                             "Irancell",       "ایرانسل",        "Telecom"),
        IranianApp("ir.rightel.myrightel",                       "Rightel",        "رایتل",          "Telecom"),

        // ─── Shopping ────────────────────────────────────────────────────────
        IranianApp("com.digikala",                               "Digikala",       "دیجی‌کالا",      "Shopping"),
        IranianApp("ir.divar",                                   "Divar",          "دیوار",          "Shopping"),
        IranianApp("com.sheypoor.mobile",                        "Sheypoor",       "شیپور",          "Shopping"),
        IranianApp("ir.basalam.app",                             "Basalam",        "باسلام",         "Shopping"),
        IranianApp("com.okala",                                  "Okala",          "اوکالا",         "Shopping"),
        IranianApp("ir.torob",                                   "Torob",          "ترب",            "Shopping"),
        IranianApp("com.farsitel.bazaar",                        "Bazaar",         "بازار",          "Shopping"),

        // ─── Insurance ───────────────────────────────────────────────────────
        IranianApp("com.nar.bimito",                             "Bimito",         "بیمیتو",         "Insurance"),
        IranianApp("com.bimebazar.bimebazar",                    "Bime Bazar",     "بیمه بازار",     "Insurance"),

        // ─── Social ──────────────────────────────────────────────────────────
        IranianApp("ir.eitaa.messenger",                         "Eitaa",          "ایتا",           "Social"),
        IranianApp("app.rbmain.a",                               "Rubika",         "روبیکا",         "Social"),
        IranianApp("mobi.mmdt.ottplus",                          "OTT Plus",       "اوتی‌تی پلاس",   "Social"),

        // ─── Services ────────────────────────────────────────────────────────
        IranianApp("com.citydi.hplus",                           "City Plus",      "سیتی پلاس",      "Services"),
        IranianApp("com.parsmobapp",                             "Pars Mobile",    "پارس موبایل",    "Services"),
        IranianApp("ir.ayantech.ghabzino",                       "Ghabzino",       "قبضینو",         "Services"),
        IranianApp("com.sibche.aspardproject.app",               "Aspard",         "اسپرد",          "Services"),
        IranianApp("ir.zypod.app",                               "Zypod",          "زیپاد",          "Services"),
        IranianApp("app.sepino",                                 "Sepino",         "سپینو",          "Services"),
        IranianApp("com.melal",                                  "Melal",          "ملل",           "Services"),
    )

    val packageNames: List<String> get() = apps.map { it.packageName }

    /** Offline fallback for GET /api/subscription/apps when the catalog can't be fetched. */
    fun asCatalog(): List<CatalogApp> = apps.map {
        CatalogApp(
            packageName = it.packageName,
            nameEn = it.nameEn,
            nameFa = it.nameFa,
            isFree = it.packageName in FREE_PACKAGES,
        )
    }
}
