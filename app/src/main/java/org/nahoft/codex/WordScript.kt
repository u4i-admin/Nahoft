package org.nahoft.codex

import java.math.BigInteger

class WordScript: BaseScript() {
    companion object {
        public val wordList: Array<String> = arrayOf(
            "عنوان",
            "برای",
            "هستیم",
            "با",
            "آنها",
            "بودن",
            "یک",
            "دارند",
            "این",
            "توسط",
            "کلمه",
            "اما",
            "است",
            "شما",
            "حال",
            "و",
            "از",
            "بود",
            "شان",
            "زمان",
            "اگر",
            "چگونه",
            "گفت:",
            "بگو",
            "میکند",
            "مجموعه",
            "سه",
            "هوا",
            "همچنین",
            "بهعنوان",
            "بندر",
            "طلسم",
            "اضافه",
            "حتی",
            "اینجا",
            "بپرسید",
            "مردها",
            "تغییر",
            "رفت",
            "نور",
            "خانه",
            "تصویر",
            "دوباره",
            "حیوانات",
            "مادر",
            "جهان",
            "ساخت",
            "جدید",
            "محل",
            "زنده",
            "کمی",
            "مرد",
            "سال",
            "آمد",
            "نمایش",
            "هر",
            "ما",
            "نامونام",
            "فقط",
            "فرم",
            "حکم",
            "کمک",
            "خط",
            "متفاوت",
            "علت",
            "قبل",
            "راست",
            "قدیمی",
            "هم",
            "همان",
            "همه",
            "استفاده",
            "نوشتن",
            "مانند",
            "تا",
            "اینها",
            "طولانی",
            "ببینید",
            "او",
            "دارد",
            "نگاه",
            "تر",
            "روز",
            "آمده",
            "تعداد",
            "صدا",
            "بیشترین",
            "مردم",
            "من",
            "روی",
            "اب",
            "تماس",
            "اولین",
            "سمت",
            "بوده",
            "سر",
            "ایستادن",
            "خود",
            "صفحه",
            "یافت",
            "مدرسه",
            "مطالعه",
            "یادگیری",
            "کارخانه",
            "پوشش",
            "آفتاب",
            "چهار",
            "بین",
            "دولت",
            "چشم",
            "هرگز",
            "آخرین",
            "فکر",
            "شهرستان",
            "درخت",
            "صلیب",
            "مزرعه",
            "سخت",
            "زور",
            "داستان",
            "اره",
            "بسیار",
            "دریا",
            "اواخر",
            "اجرا",
            "نکن",
            "مطبوعات",
            "نزدیک",
            "واقعی",
            "زندگی",
            "کم",
            "شمال",
            "کتاب",
            "حمل",
            "علم",
            "خوردن",
            "اتاق",
            "دوستان",
            "ایده",
            "ماهی",
            "توقف",
            "پایه",
            "اسب",
            "برش",
            "مطمئن",
            "تماشای",
            "صورت",
            "باز",
            "بعدی",
            "سفید",
            "کودکان",
            "شروع",
            "رو",
            "مثال",
            "آسان",
            "مقاله",
            "موسیقی",
            "آن",
            "غالبا",
            "نامه",
            "مایل",
            "رودخانه",
            "اتومبیل",
            "مراقبت",
            "دوم",
            "کافی",
            "دختر",
            "معمول",
            "جوان",
            "اماده",
            "همیشه",
            "قرمز",
            "لیست",
            "پرنده",
            "بزودی",
            "بدن",
            "سگ",
            "خانواده",
            "مطرح",
            "ترک",
            "درب",
            "کوتاه",
            "کلاس",
            "باد",
            "سوال",
            "کشتی",
            "نیم",
            "منظور",
            "آتش",
            "جنوب",
            "قطعه",
            "گفت",
            "عبور",
            "بالا",
            "تمام",
            "پادشاه",
            "خیابان",
            "اینچ",
            "ضرب",
            "هیچ",
            "البته",
            "اقامت",
            "چرخ",
            "کامل",
            "نیروی",
            "آبی",
            "شی",
            "عمیق",
            "جزیره",
            "سیستم",
            "مشغول",
            "آزمون",
            "رکورد",
            "قایق",
            "مشترک",
            "طلا",
            "ممکن",
            "هواپیما",
            "جا",
            "خشک",
            "خنده",
            "هزار",
            "پیش",
            "فرار",
            "بررسی",
            "بازی",
            "داغ",
            "آورده",
            "حرارت",
            "برف",
            "لاستیک",
            "را",
            "بله",
            "دور",
            "پر",
            "شرق",
            "رنگ",
            "زبان",
            "واحد",
            "قدرت",
            "شهر",
            "معین",
            "پرواز",
            "شود",
            "تاریک",
            "ماشین",
            "یادداشت",
            "صبر",
            "برنامه",
            "شکل",
            "ستاره",
            "جعبه",
            "اسم",
            "حوزه",
            "بقیه",
            "درست",
            "قادر",
            "پوند",
            "انجام",
            "زیبایی",
            "درایو",
            "شامل",
            "جلو",
            "آموزش",
            "هفته",
            "نهایی",
            "به",
            "سبز",
            "آه",
            "توسعه",
            "اقیانوس",
            "گرم",
            "رایگان",
            "دقیقه",
            "قوی",
            "ذهن",
            "دم",
            "واقع",
            "فضا",
            "شنیده",
            "بهترین",
            "بهتر",
            "در",
            "صد",
            "پنج",
            "گام",
            "اوایل",
            "غرب",
            "علاقه",
            "سریع",
            "فعل",
            "شش",
            "جدول",
            "کمتر",
            "صبح",
            "ده",
            "ساده",
            "چند",
            "واکه",
            "جنگ",
            "الگوی",
            "کند",
            "مرکز",
            "فرد",
            "پول",
            "خدمت",
            "جاده",
            "نقشه",
            "باران",
            "حکومت",
            "کشیدن",
            "اطلاع",
            "صدای",
            "انرژی",
            "شکار",
            "احتمالی",
            "برادر",
            "سوار",
            "سلول",
            "باور",
            "شاید",
            "ناگهانی",
            "شمار",
            "مربع",
            "دلیل",
            "طول",
            "نمایندگی",
            "هنر",
            "منطقه",
            "اندازه",
            "کنند",
            "وزن",
            "عمومی",
            "یخ",
            "موضوع",
            "دایره",
            "جفت",
            "تقسیم",
            "هجا",
            "نمد",
            "توپ",
            "هنوز",
            "موج",
            "قلب",
            "حاضر",
            "سنگین",
            "رقص",
            "موتور",
            "موقعیت",
            "دست",
            "گسترده",
            "بادبان",
            "جنگل",
            "نشستن",
            "پنجره",
            "تابستان",
            "قطار",
            "خواب",
            "ثابت",
            "تنها",
            "پا",
            "ورزش",
            "دیوار",
            "گرفتن",
            "کوه",
            "آرزو",
            "آسمان",
            "لذت",
            "زمستان",
            "شنبه",
            "وحشی",
            "چمن",
            "گاو",
            "لبه",
            "علامت",
            "بازدید",
            "گذشته",
            "نرم",
            "سرگرم",
            "روشن",
            "گاز",
            "ماه",
            "میلیون",
            "تحمل",
            "پایان",
            "شاد",
            "امیدوارم",
            "گل",
            "پوشاندن",
            "رفته",
            "تجارت",
            "ملودی",
            "سفر",
            "دفتر",
            "دریافت",
            "ردیف",
            "دهان",
            "دقیق",
            "نماد",
            "کمترین",
            "مشکل",
            "فریاد",
            "جز",
            "نوشت",
            "دانه",
            "تن",
            "عضویت",
            "تمیز",
            "استراحت",
            "خانم",
            "حیاط",
            "بد",
            "نفت",
            "خون",
            "دستزدن",
            "رشد",
            "مخلوط",
            "تیم",
            "سیم",
            "هزینه",
            "باغ",
            "برابر",
            "کنید",
            "سقوط",
            "عادلانه",
            "بانک",
            "ذخیره",
            "کنترل",
            "اعشاری",
            "گوش",
            "دیگر",
            "کاملا",
            "شکست",
            "مورد",
            "متوسط",
            "کشتن",
            "پسر",
            "دریاچه",
            "مقیاس",
            "بهار",
            "مشاهده",
            "مستقیم",
            "همخوان",
            "کشور",
            "شیر",
            "سرعت",
            "روش",
            "عضو",
            "پرداخت",
            "سن",
            "لباس",
            "ابر",
            "تعجب",
            "آرام",
            "سنگ",
            "کوچک",
            "صعود",
            "سرد",
            "طراحی",
            "ضعیف",
            "زیادی",
            "پایین",
            "کلید",
            "اهن",
            "تک",
            "چوب",
            "تخت",
            "بیست",
            "پوست",
            "لبخند",
            "چین",
            "سوراخ",
            "کودک",
            "هشت",
            "روستای",
            "ملاقات",
            "ریشه",
            "خرید",
            "حل",
            "فلز",
            "چه",
            "فشار",
            "هفت",
            "بند",
            "سوم",
            "باید",
            "مو",
            "توصیف",
            "آشپز",
            "طبقه",
            "یا",
            "نتیجه",
            "رایت",
            "تپه",
            "امن",
            "گربه",
            "قرن",
            "نوع",
            "قانون",
            "بیت",
            "کپی",
            "عبارت",
            "خاموش",
            "بلند",
            "شن",
            "خاک",
            "رول",
            "انگشت",
            "صنعت",
            "ارزش",
            "مبارزه",
            "دروغ",
            "تحریک",
            "طبیعی",
            "احساس",
            "سرمایه",
            "خطر",
            "میوه",
            "غنی",
            "ضخامت",
            "سرباز",
            "روند",
            "کار",
            "عمل",
            "جداگانه",
            "دشوار",
            "دکتر",
            "لطفا",
            "محافظت",
            "ظهر",
            "محصول",
            "مدرن",
            "عنصر",
            "ضربه",
            "گوشه",
            "حزب",
            "عرضه",
            "که",
            "قرار",
            "حلقه",
            "شخصیت",
            "حشرات",
            "گرفتار",
            "دوره",
            "رادیو",
            "صحبت",
            "اتم",
            "انسانی",
            "تاریخ",
            "اثر",
            "برق",
            "انتظار",
            "استخوان",
            "نرده",
            "ارائه",
            "توافق",
            "ملایم",
            "زن",
            "کاپیتان",
            "لازم",
            "تیز",
            "بال",
            "ایجاد",
            "همسایه",
            "شستشو",
            "خفاش",
            "ذرت",
            "مقایسه",
            "شعر",
            "رشته",
            "زنگ",
            "گوشت",
            "مالیدن",
            "لوله",
            "معروف",
            "دلار",
            "نظر",
            "نازک",
            "مثلث",
            "سیاره",
            "رئیس",
            "مستعمره",
            "ساعت",
            "معدن",
            "کراوات",
            "تازه",
            "جستجو",
            "ارسال",
            "زرد",
            "اسلحه",
            "اجازه",
            "چاپ",
            "مرده",
            "نقطه",
            "بیابان",
            "جریان",
            "آسانسور",
            "افزایش",
            "رسیدن",
            "کارشناس",
            "آهنگ",
            "ساحل",
            "ورق",
            "ماده",
            "اتصال",
            "پست",
            "وتر",
            "چربی",
            "خوشحالم",
            "اصلی",
            "سهم",
            "ایستگاه",
            "پدر",
            "نان",
            "شارژ",
            "مناسب",
            "بار",
            "پیشنهاد",
            "بخش",
            "برده",
            "اردک",
            "فوری",
            "بازار",
            "درجه",
            "جمعیت",
            "جوجه",
            "عزیز",
            "دشمن",
            "پاسخ",
            "نوشابه",
            "پشتیبانی",
            "سخنرانی",
            "طبیعت",
            "دامنه",
            "بخار",
            "حرکت",
            "راه",
            "مایع",
            "دندانها",
            "پوسته",
            "گردن",
            "اکسیژن",
            "قند",
            "مرگ",
            "خوب",
            "مهارت",
            "زنان",
            "فصل",
            "مغناطیس",
            "تشکر",
            "شاخه",
            "مسابقه",
            "پسوند",
            "ویژه",
            "انجیر",
            "ترس",
            "بزرگ",
            "خواهر",
            "فولاد",
            "بحث",
            "مشابه",
            "راهنمایی",
            "تجربه",
            "نمره",
            "سیب",
            "خریداری",
            "رهبری",
            "زمین",
            "کت",
            "جرم",
            "کارت",
            "گروه",
            "طناب",
            "لغزش",
            "برنده",
            "رویا",
            "شب",
            "شرایط",
            "خوراک",
            "ابزار",
            "کل",
            "اساسی",
            "بوی",
            "دره",
            "دو",
            "صندلی",
            "ادامه",
            "بلوک",
            "نمودار",
            "کلاه",
            "فروش",
            "موفقیت",
            "شرکت",
            "تفریق",
            "رویداد",
            "خاص",
            "معامله",
            "شنا",
            "مدت",
            "همسر",
            "کفش",
            "شانه",
            "گسترش",
            "ترتیب",
            "اردوگاه",
            "اختراع",
            "پنبه",
            "متولد",
            "تعیین",
            "کوارت",
            "نه",
            "کامیون",
            "سطح",
            "شانس",
            "فروشگاه",
            "کشش",
            "پرتاب",
            "درخشش",
            "خاصیت",
            "ستون",
            "مولکول",
            "اشتباه",
            "خاکستری",
            "تکرار",
            "نیاز",
            "پهن",
            "آماده",
            "نمک",
            "بینی",
            "جمع",
            "خشم",
            "ادعا",
            "قاره"
        )
    }

    fun encode(bytes: ByteArray): String {
        val byteDigits = bytesToDigits(bytes)
//        println("bytes: " + byteDigits)

        val integer = digitsToBigInteger(byteDigits, 256)
//        println("integer: " + integer)

        val digits = bigIntegerToDigits(integer, wordList.size)
//        println("digits: " + digits)

        // takes a series of digits, looks it up in the word list and gives back a string of words
        return digitsToSymbols(digits)
    }

    fun decode(ciphertext: String): ByteArray {
        var result = byteArrayOf()

        val base = wordList.size

        val digits = symbolsToDigits(ciphertext)
//        println("digits: " + digits)

        val integer = digitsToBigInteger(digits, base)
//        println("integer: " + integer)

        val byteDigits = bigIntegerToDigits(integer, 256)
//        println("bytes: " + byteDigits)

        val bytes = digitsToBytes(byteDigits)

        return bytes
    }
}