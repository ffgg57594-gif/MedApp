# مساعد التشخيص الطبي (MedApp)

تطبيق أندرويد يعمل بالكامل **دون اتصال بالإنترنت (offline)** لتحليل الصور
الطبية باستخدام نماذج ذكاء اصطناعي محولة من MONAI/PyTorch إلى ONNX. مصمم
كمكتبة نماذج قابلة للتوسع: كل نموذج مستقل بملف ONNX + إعدادات JSON خاصة به.

⚠️ **هذا التطبيق أداة مساعدة تعليمية/بحثية وليس جهازًا طبيًا معتمدًا.** أي
استخدام فعلي في بيئة سريرية حقيقية يتطلب اعتمادًا تنظيميًا (مثل FDA أو ما
يعادلها محليًا) واختبارًا سريريًا لا يوفره هذا المشروع.

## البنية

```
app/
  src/main/
    java/com/medapp/
      ai/
        core/        # الواجهات الأساسية (MedicalModel, InferenceResult...)
        models/       # تطبيقات كل نوع مهمة (classification, ...)
        registry/     # ModelRegistry - نقطة الإضافة المركزية
      ui/
        screens/      # الشاشات (Main, ModelList, Inference)
        components/    # عناصر UI قابلة لإعادة الاستخدام
    assets/models/     # كل نموذج في مجلده: model.onnx + config.json
scripts/
  convert_to_onnx.py   # تحويل نماذج PyTorch/MONAI إلى ONNX
docs/
  ADDING_A_MODEL.md    # شرح خطوة بخطوة لإضافة نموذج جديد
.github/workflows/
  build-apk.yml         # بناء APK تلقائيًا عبر GitHub Actions
```

## الحالة الحالية

- ✅ الهيكل الكامل جاهز وقابل للتوسع (plug-and-play للنماذج)
- ✅ نموذج واحد مُعرَّف: `densenet121_chest_xray` (تصنيف أشعة الصدر)
- ⚠️ **ملف `model.onnx` الفعلي غير موجود بعد** — لازم تضيفه بنفسك (راجع
  القسم التالي)
- ⚠️ segmentation / anomaly detection: البنية جاهزة لاستقبالهم لكن لسه مفيش
  implementation فعلي (راجع `docs/ADDING_A_MODEL.md`)

## خطوات التشغيل

### 1. أضف ملف النموذج

النموذج الأول (`densenet121_chest_xray`) محتاج ملف `model.onnx` حقيقي.
احصل عليه من [MONAI Model Zoo](https://monai.io/model-zoo.html) أو درّبه/
حمّله بنفسك، وحوّله لـ ONNX باستخدام:

```bash
pip install monai torch onnx onnxruntime
python scripts/convert_to_onnx.py --checkpoint your_model.pt --output model.onnx
```

انسخ الملف الناتج إلى:
```
app/src/main/assets/models/densenet121_chest_xray/model.onnx
```

### 2. ابنِ المشروع

**عبر GitHub Actions (موصى به لأنك بتبني APK):**
1. ادفع (push) المشروع إلى مستودع GitHub جديد
2. الـ workflow في `.github/workflows/build-apk.yml` هيشتغل تلقائيًا
3. حمّل الـ APK من تبويب "Actions" -> "Artifacts"

**محليًا (لو عندك Android Studio):**
```bash
./gradlew assembleDebug
```
ملف الـ APK هيكون في `app/build/outputs/apk/debug/app-debug.apk`

### 3. أضف نماذج جديدة

راجع `docs/ADDING_A_MODEL.md` للشرح الكامل. باختصار: مجلد جديد في
`assets/models/` + سطر واحد في `ModelRegistry.kt`.

## ملاحظة تقنية عن ملف gradle-wrapper.jar

هذا المشروع يحتوي على `gradlew`/`gradlew.bat` لكن **بدون** ملف
`gradle/wrapper/gradle-wrapper.jar` الثنائي (binary) — تعذّر إنشاؤه في
بيئة إنشاء هذا المشروع لعدم توفر اتصال إنترنت لحظتها. الـ workflow
(`build-apk.yml`) يتعامل مع هذا تلقائيًا بتوليد الملف عند البناء على
GitHub. لو حبيت تبني محليًا بدل GitHub Actions، شغّل مرة واحدة (يتطلب
تثبيت Gradle مسبقًا):

```bash
gradle wrapper --gradle-version 8.7
```

ده هيولّد الملف الناقص محليًا وبعدها `./gradlew` هيشتغل عادي.
