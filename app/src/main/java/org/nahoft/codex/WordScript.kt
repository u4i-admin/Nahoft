package org.nahoft.codex

import java.math.BigInteger

val wordList: Array<String> = arrayOf(
    "عنوان", "من", "خود", "که", "او", "بود", "برای", "در", "هستیم", "با",
    "آنها", "بودن", "در", "یک", "دارند", "این", "از", "توسط", "داغ", "کلمه",
    "اما", "چه", "است", "آن", "شما", "حال", "تر", "از", "به", "و", "دست", "در",
    "ما", "از", "دیگر", "بود", "که", "انجام", "شان", "زمان", "اگر", "چگونه",
    "گفت:", "پا", "هر", "بگو", "میکند", "مجموعه", "سه", "هوا", "همچنین", "بازی",
    "کوچک", "پایان", "خانه", "بهعنوان", "دست", "بندر", "بزرگ", "طلسم", "اضافه",
    "حتی", "زمین", "اینجا", "باید", "بزرگ", "بالا", "عمل", "بپرسید", "مردها",
    "تغییر", "رفت", "نور", "نوع", "خاموش", "نیاز", "خانه", "تصویر", "ما", "دوباره",
    "حیوانات", "نقطه", "مادر", "جهان", "ساخت", "خود", "زمین", "پدر", "هر", "جدید",
    "کار", "بخش", "را", "دریافت", "محل", "زنده", "کمی", "تنها", "دور", "مرد", "سال",
    "آمد", "نمایش", "هر", "خوب", "را", "ما", "در", "نامونام", "بسیار", "فقط", "فرم",
    "حکم", "بزرگ", "کمک", "کم", "خط", "متفاوت", "علت", "بسیار", "متوسط", "قبل",
    "حرکت", "راست", "پسر", "قدیمی", "هم", "همان", "او", "همه", "بالا", "استفاده",
    "راه", "نوشتن", "را", "مانند", "تا", "اینها", "او", "طولانی", "را", "ببینید",
    "او", "دو", "دارد", "نگاه", "تر", "روز", "به", "آمده", "انجام", "تعداد", "صدا",
    "هیچ", "بیشترین", "مردم", "من", "روی", "اب", "تماس", "اولین", "که", "پایین",
    "سمت", "بوده", "ساعت", "سر", "ایستادن", "خود", "صفحه", "باید", "کشور", "یافت",
    "پاسخ", "مدرسه", "رشد", "مطالعه", "هنوز", "یادگیری", "کارخانه", "پوشش", "آفتاب",
    "چهار", "بین", "دولت", "چشم", "هرگز", "آخرین", "اجازه", "فکر", "شهرستان", "درخت",
    "صلیب", "مزرعه", "سخت", "شروع", "زور", "داستان", "اره", "بسیار", "دریا", "اواخر",
    "اجرا", "نکن", "مطبوعات", "نزدیک", "شب", "واقعی", "زندگی", "کم", "شمال", "کتاب",
    "حمل", "علم", "خوردن", "اتاق", "دوستان", "ایده", "ماهی", "کوه", "توقف", "پایه",
    "گوش", "اسب", "برش", "مطمئن", "تماشای", "رنگ", "صورت", "چوب", "اصلی", "باز",
    "بعدی", "سفید", "کودکان", "شروع", "رو", "مثال", "آسان", "مقاله", "گروه", "همیشه",
    "موسیقی", "آن", "علامت", "غالبا", "نامه", "مایل", "رودخانه", "اتومبیل", "پا",
    "مراقبت", "دوم", "کافی", "ساده", "دختر", "معمول", "جوان", "اماده", "بالا", "همیشه",
    "قرمز", "لیست", "احساس", "بحث", "پرنده", "بزودی", "بدن", "سگ", "خانواده",
    "مستقیم", "مطرح", "ترک", "آهنگ", "درب", "محصول", "کوتاه", "کلاس", "باد",
    "سوال", "کامل", "کشتی", "منطقه", "نیم", "سنگ", "منظور", "آتش", "جنوب", "مشکل",
    "قطعه", "گفت", "عبور", "بالا", "تمام", "پادشاه", "خیابان", "اینچ", "ضرب", "هیچ",
    "البته", "اقامت", "چرخ", "کامل", "نیروی", "آبی", "شی", "سطح", "عمیق", "ماه",
    "جزیره", "پا", "سیستم", "مشغول", "آزمون", "رکورد", "قایق", "مشترک", "طلا", "ممکن",
    "هواپیما", "جا", "خشک", "خنده", "هزار", "پیش", "فرار", "بررسی", "بازی", "شکل",
    "برابر", "داغ", "دست", "آورده", "حرارت", "برف", "لاستیک", "را", "بله", "دور",
    "پر", "شرق", "رنگ", "زبان", "واحد", "قدرت", "شهر", "خوب", "معین", "پرواز",
    "سقوط", "شود", "فریاد", "تاریک", "ماشین", "یادداشت", "صبر", "برنامه", "شکل",
    "ستاره", "جعبه", "اسم", "حوزه", "بقیه", "درست", "قادر", "پوند", "انجام", "زیبایی",
    "درایو", "شامل", "جلو", "آموزش", "هفته", "نهایی", "به", "سبز", "آه", "سریع",
    "توسعه", "اقیانوس", "گرم", "رایگان", "دقیقه", "قوی", "ویژه", "ذهن", "روشن", "دم",
    "محصول", "واقع", "فضا", "شنیده", "بهترین", "ساعت", "بهتر", "در", "صد", "پنج",
    "گام", "اوایل", "غرب", "زمین", "علاقه", "سریع", "فعل", "شش", "جدول", "سفر", "کمتر",
    "صبح", "ده", "ساده", "چند", "واکه", "جنگ", "الگوی", "کند", "مرکز", "فرد", "پول",
    "خدمت", "جاده", "نقشه", "باران", "قانون", "حکومت", "کشیدن", "سرد", "اطلاع", "صدای",
    "انرژی", "شکار", "احتمالی", "تخت", "برادر", "سوار", "سلول", "باور", "شاید",
    "ناگهانی", "شمار", "مربع", "دلیل", "طول", "نمایندگی", "هنر", "موضوع", "منطقه",
    "اندازه", "کنند", "وزن", "عمومی", "یخ", "موضوع", "دایره", "جفت", "تقسیم", "هجا",
    "نمد", "بزرگ", "توپ", "هنوز", "موج", "قلب", "ساعت", "حاضر", "سنگین", "رقص",
    "موتور", "موقعیت", "دست", "گسترده", "بادبان", "ماده", "بخش", "جنگل", "نشستن",
    "مسابقه", "پنجره", "فروشگاه", "تابستان", "قطار", "خواب", "ثابت", "تنها", "پا",
    "ورزش", "دیوار", "گرفتن", "کوه", "آرزو", "آسمان", "لذت", "زمستان", "شنبه",
    "وحشی", "ابزار", "چمن", "گاو", "کار", "لبه", "علامت", "بازدید", "گذشته", "نرم",
    "سرگرم", "روشن", "گاز", "ماه", "میلیون", "تحمل", "پایان", "شاد", "امیدوارم",
    "گل", "پوشاندن", "رفته", "تجارت", "ملودی", "سفر", "دفتر", "دریافت", "ردیف",
    "دهان", "دقیق", "نماد", "مرگ", "کمترین", "مشکل", "فریاد", "جز", "نوشت", "دانه",
    "تن", "عضویت", "تمیز", "استراحت", "خانم", "حیاط", "افزایش", "بد", "ضربه", "نفت",
    "خون", "دستزدن", "رشد", "مخلوط", "تیم", "سیم", "هزینه", "لباس", "باغ", "برابر",
    "ارسال", "کنید", "سقوط", "مناسب", "جریان", "عادلانه", "بانک", "ذخیره", "کنترل",
    "اعشاری", "گوش", "دیگر", "کاملا", "شکست", "مورد", "متوسط", "کشتن", "پسر",
    "دریاچه", "مقیاس", "بهار", "مشاهده", "کودک", "مستقیم", "همخوان", "کشور", "شیر",
    "سرعت", "روش", "عضو", "پرداخت", "سن", "بخش", "لباس", "ابر", "تعجب", "آرام",
    "سنگ", "کوچک", "صعود", "سرد", "طراحی", "ضعیف", "زیادی", "تجربه", "پایین", "کلید",
    "اهن", "تک", "چوب", "تخت", "بیست", "پوست", "لبخند", "چین", "سوراخ", "کودک",
    "هشت", "روستای", "ملاقات", "ریشه", "خرید", "حل", "فلز", "چه", "فشار", "هفت",
    "بند", "سوم", "باید", "مو", "توصیف", "آشپز", "طبقه", "یا", "نتیجه", "رایت",
    "تپه", "امن", "گربه", "قرن", "نوع", "قانون", "بیت", "ساحل", "کپی", "عبارت",
    "خاموش", "بلند", "شن", "خاک", "رول", "انگشت", "صنعت", "ارزش", "مبارزه", "دروغ",
    "تحریک", "طبیعی", "نظر", "احساس", "سرمایه", "نه", "صندلی", "خطر", "میوه", "غنی",
    "ضخامت", "سرباز", "روند", "کار", "عمل", "جداگانه", "دشوار", "دکتر", "لطفا",
    "محافظت", "ظهر", "محصول", "مدرن", "عنصر", "ضربه", "گوشه", "حزب", "عرضه", "که",
    "قرار", "حلقه", "شخصیت", "حشرات", "گرفتار", "دوره", "رادیو", "صحبت", "اتم",
    "انسانی", "تاریخ", "اثر", "برق", "انتظار", "استخوان", "نرده", "ارائه", "توافق",
    "ملایم", "زن", "کاپیتان", "لازم", "تیز", "بال", "ایجاد", "همسایه", "شستشو", "خفاش",
    "نه", "جمعیت", "ذرت", "مقایسه", "شعر", "رشته", "زنگ", "گوشت", "مالیدن", "لوله",
    "معروف", "دلار", "جریان", "ترس", "نظر", "نازک", "مثلث", "سیاره", "رئیس", "مستعمره",
    "ساعت", "معدن", "کراوات", "اصلی", "تازه", "جستجو", "ارسال", "زرد", "اسلحه",
    "اجازه", "چاپ", "مرده", "نقطه", "بیابان", "جریان", "آسانسور", "افزایش", "رسیدن",
    "کارشناس", "آهنگ", "ساحل", "بخش", "ورق", "ماده", "اتصال", "پست", "وتر", "چربی",
    "خوشحالم", "اصلی", "سهم", "ایستگاه", "پدر", "نان", "شارژ", "مناسب", "بار",
    "پیشنهاد", "بخش", "برده", "اردک", "فوری", "بازار", "درجه", "جمعیت", "جوجه",
    "عزیز", "دشمن", "پاسخ", "نوشابه", "پشتیبانی", "سخنرانی", "طبیعت", "دامنه",
    "بخار", "حرکت", "راه", "مایع", "دندانها", "پوسته", "گردن", "اکسیژن", "قند",
    "مرگ", "خوب", "مهارت", "زنان", "فصل", "مغناطیس", "تشکر", "شاخه", "مسابقه",
    "پسوند", "ویژه", "انجیر", "ترس", "بزرگ", "خواهر", "فولاد", "بحث", "مشابه",
    "راهنمایی", "تجربه", "نمره", "سیب", "خریداری", "رهبری", "زمین", "کت", "جرم",
    "کارت", "گروه", "طناب", "لغزش", "برنده", "رویا", "شب", "شرایط", "خوراک",
    "ابزار", "کل", "اساسی", "بوی", "دره", "دو", "صندلی", "ادامه", "بلوک", "نمودار",
    "کلاه", "فروش", "موفقیت", "شرکت", "تفریق", "رویداد", "خاص", "معامله", "شنا",
    "مدت", "همسر", "کفش", "شانه", "گسترش", "ترتیب", "اردوگاه", "اختراع", "پنبه",
    "متولد", "تعیین", "کوارت", "نه", "کامیون", "سطح", "شانس", "فروشگاه", "کشش",
    "پرتاب", "درخشش", "خاصیت", "ستون", "مولکول", "اشتباه", "خاکستری", "تکرار",
    "نیاز", "پهن", "آماده", "نمک", "بینی", "جمع", "خشم", "ادعا", "قاره"
)
class WordScript {

    fun encode(bytes: ByteArray): String {
        val byteDigits = bytesToDigits(bytes)
        println("bytes: " + byteDigits)

        val integer = digitsToBigInteger(byteDigits, 256)
        println("integer: " + integer)

        val digits = bigIntegerToDigits(integer, alphabet.size)
        println("digits: " + digits)

        return digitsToSymbols(digits)
    }

    fun decode(ciphertext: String): ByteArray {
        var result = byteArrayOf()

        val base = alphabet.size

        val digits = symbolsToDigits(ciphertext)
        println("digits: " + digits)

        val integer = digitsToBigInteger(digits, base)
        println("integer: " + integer)

        val byteDigits = bigIntegerToDigits(integer, 256)
        println("bytes: " + byteDigits)

        val bytes = digitsToBytes(byteDigits)

        return bytes
    }

    fun digitsToSymbols(digits: List<Int>): String {
        var result: String = String()

        for (digit in digits) {
            val symbol = alphabet[digit.toInt()]
            result = result + symbol
        }
        println("symbol: " + result)

        return result
    }

    fun symbolsToDigits(ciphertext: String): List<Int> {
        var digits: List<Int> = listOf()
        for (offset in 0..ciphertext.lastIndex) {
            val symbol = Character.toString(ciphertext[offset])

            var foundIndex = -1
            for (index in 0..alphabet.lastIndex) {
                if (alphabet[index] == symbol) {
                    foundIndex = index
                    break
                }
            }

            if (foundIndex == -1) {
                println("Symbol not found")
                return emptyList()
            }

            val digit = foundIndex
            digits += digit
        }

        return digits
    }

    fun bigIntegerToDigits(integer: BigInteger, base: Int): List<Int> {
        if (integer == BigInteger.ZERO) {
            return listOf(0)
        }

        var working = integer
        val bigBase = base.toBigInteger()

        var result: List<Int> = listOf()

        val numDigits = computeNumDigits(integer, base)

        val placeValues = generatePlaceValues(numDigits, base)
        // println("placeValues: " + placeValues)

        working = integer
        for (placeValue in placeValues) {
            val digit = working / placeValue
            working %= placeValue

            print(" " + placeValue + " * " + digit + " + ")

            result += digit.toInt()
        }

        println()

        while (result[0] == 0) {
            result = result.slice(1..result.size - 1)
        }

        return result
    }

    fun digitsToBigInteger(digits: List<Int>, base: Int): BigInteger {
        val bigBase = base.toBigInteger()

        val numDigits = digits.size

        val placeValues = generatePlaceValues(numDigits, base)
        // println("placeValues: " + placeValues)

        return computeInteger(placeValues, digits)

    }

    fun bytesToDigits(bytes: ByteArray): List<Int>
    {
        var result: List<Int> = listOf()

        for (byte in bytes)
        {
            if (byte >= 0)
            {
                result += byte.toInt()
            }
            else
            {
                result += (-byte.toInt()) + 128
            }
        }

        return result
    }

    fun digitsToBytes(digits: List<Int>): ByteArray
    {
        var result = byteArrayOf()

        for (digit in digits)
        {
            if (digit > 128)
            {
                result += (-(digit-128)).toByte()
            }
            else
            {
                result += digit.toByte()
            }
        }

        return result
    }
}