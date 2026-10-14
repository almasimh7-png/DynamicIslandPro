# Dynamic Island Pro

اپلیکیشن اندروید «Dynamic Island Pro» با پکیج `com.ziba.dynamicislandpro`،
نوشته‌شده با **Kotlin + Jetpack Compose (Material3)**، `minSdk 26` و
`targetSdk/compileSdk 35`، AGP 8.5+ و Kotlin 2.0+.

## امکانات
- **MainActivity** با دکمه‌های مجوزها (نمایش روی صفحه، شنیدن اعلان‌ها، باتری، دسترس‌پذیری) و شروع/توقف سرویس اوورلی
- **IslandOverlayService**: سرویس Foreground که با WindowManager + ComposeView اوورلی می‌سازد؛ حالت جمع‌شده/باز با انیمیشن
- **IslandNotificationListener**: گوش‌دادن به اعلان‌ها و انتشار آن‌ها از طریق singleton مشترک (AppBus) به UI اوورلی
- **MediaSessionWatcher**: اتصال به MediaSessionManager و Callback‌های MediaController (عنوان، هنرمند، وضعیت پخش)
- **TimerForegroundService**: تایمر/کرنومتر Foreground با Kotlin Coroutines (اجرا/توقف/بازنشانی + نمایش پیشرفت در اوورلی)
- **انیمیشن تماس ورودی**: تشخیص اعلان‌های CallStyle (و fallback روی extras) + اسکلت AccessibilityService برای رویدادهای تماس
  با مستندسازی محدودیت‌ها (عدم امکان پاسخ‌دادن، عدم دسترسی به dialer در پروفایل کاربری جدا)
- **لوکالیزیشن fa/en** و پشتیبانی RTL (`android:supportsRtl="true"` و `values-fa`)
- **تم Material3**

## ساخت در Android Studio
1. فایل زیپ را استخراج کنید و پوشه `DynamicIslandPro` را با **File > Open** باز کنید.
2. صبر کنید تا Gradle Sync تمام شود (اگر `gradle-wrapper.jar` وجود ندارد، استودیو خودش می‌سازد؛
   در غیر این صورت یک‌بار در ترمینال: `gradle wrapper --gradle-version 8.7`).
3. از منوی **Build > Build Bundle(s)/APK(s) > Build APK(s)** خروجی debug بگیرید.
   مسیر خروجی: `app/build/outputs/apk/debug/app-debug.apk`

## ساخت با خط فرمان (CLI)
```bash
cd DynamicIslandPro
./gradlew assembleDebug          # یا: gradle assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions (ساخت debug APK)
فایل `.github/workflows/build.yml` آماده است؛ با هر push روی `main` یک APK دیباگ به‌صورت artifact ساخته می‌شود.

## امضای نسخه Release
1. ساخت keystore:
```bash
keytool -genkey -v -keystore release.keystore -alias dynamicisland \
  -keyalg RSA -keysize 2048 -validity 10000
```
2. فایل `keystore.properties` کنار ریشه پروژه:
```
storeFile=../release.keystore
storePassword=****
keyAlias=dynamicisland
keyPassword=****
```
3. در `app/build.gradle.kts` بلوک `signingConfigs` را اضافه کنید و در `buildTypes.release` تنظیم `signingConfig` را فعال کنید
   (نمونه کامنت‌شده داخل فایل موجود است).
4. ساخت:
```bash
./gradlew assembleRelease
# خروجی: app/build/outputs/apk/release/app-release.apk
```

## نکات مهم
- `gradle-wrapper.jar` باینری است و در این تولید خودکار وجود ندارد؛ یک‌بار با `gradle wrapper` بسازید.
- برای نمایش اوورلی حتماً مجوز SYSTEM_ALERT_WINDOW را از تنظیمات بدهید.
