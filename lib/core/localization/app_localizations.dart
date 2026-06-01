import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

class AppLocalizations {
  const AppLocalizations(this.locale);

  final Locale locale;

  static const supportedLocales = <Locale>[
    Locale('en'),
    Locale('ar'),
    Locale('es'),
    Locale('fr'),
  ];

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const _localizedValues = <String, Map<String, String>>{
    'en': {
      'appName': 'QR Master',
      'homeTitle': 'Create, scan, and share premium QR codes',
      'generator': 'Generator',
      'scanner': 'Scanner',
      'history': 'History',
      'settings': 'Settings',
      'content': 'QR content',
      'contentHint': 'Type text, URL, WiFi, phone, SMS, email, vCard, location, or social link',
      'design': 'Design Studio',
      'livePreview': 'Live preview',
      'savePng': 'Save PNG',
      'savePdf': 'Export PDF',
      'share': 'Share',
      'scanPrompt': 'Point your camera at a QR code',
      'gallery': 'Gallery',
      'flash': 'Flash',
      'autoOpen': 'Open URL after confirmation',
      'systemLanguage': 'Language follows your device automatically',
      'systemTheme': 'Light and Dark Mode follow the system theme',
      'emptyHistory': 'Generated and scanned codes will appear here.',
    },
    'ar': {
      'appName': 'QR Master',
      'homeTitle': 'أنشئ وامسح وشارك رموز QR باحتراف',
      'generator': 'إنشاء',
      'scanner': 'مسح',
      'history': 'السجل',
      'settings': 'الإعدادات',
      'content': 'محتوى QR',
      'contentHint': 'اكتب نصاً أو رابطاً أو واي فاي أو هاتفاً أو SMS أو بريداً أو vCard أو موقعاً أو رابطاً اجتماعياً',
      'design': 'استوديو التصميم',
      'livePreview': 'معاينة فورية',
      'savePng': 'حفظ PNG',
      'savePdf': 'تصدير PDF',
      'share': 'مشاركة',
      'scanPrompt': 'وجّه الكاميرا نحو رمز QR',
      'gallery': 'المعرض',
      'flash': 'الفلاش',
      'autoOpen': 'فتح الرابط بعد التأكيد',
      'systemLanguage': 'اللغة تتبع جهازك تلقائياً',
      'systemTheme': 'الوضع الفاتح والداكن يتبعان النظام',
      'emptyHistory': 'ستظهر الرموز المنشأة والممسوحة هنا.',
    },
    'es': {
      'appName': 'QR Master',
      'homeTitle': 'Crea, escanea y comparte códigos QR premium',
      'generator': 'Generador',
      'scanner': 'Escáner',
      'history': 'Historial',
      'settings': 'Ajustes',
      'content': 'Contenido QR',
      'contentHint': 'Escribe texto, URL, WiFi, teléfono, SMS, email, vCard, ubicación o enlace social',
      'design': 'Estudio de diseño',
      'livePreview': 'Vista previa',
      'savePng': 'Guardar PNG',
      'savePdf': 'Exportar PDF',
      'share': 'Compartir',
      'scanPrompt': 'Apunta la cámara a un QR',
      'gallery': 'Galería',
      'flash': 'Flash',
      'autoOpen': 'Abrir URLs tras confirmar',
      'systemLanguage': 'El idioma sigue automáticamente tu dispositivo',
      'systemTheme': 'Modo claro y oscuro siguen el sistema',
      'emptyHistory': 'Los códigos creados y escaneados aparecerán aquí.',
    },
    'fr': {
      'appName': 'QR Master',
      'homeTitle': 'Créez, scannez et partagez des QR premium',
      'generator': 'Générateur',
      'scanner': 'Scanner',
      'history': 'Historique',
      'settings': 'Réglages',
      'content': 'Contenu QR',
      'contentHint': 'Saisissez texte, URL, WiFi, téléphone, SMS, e-mail, vCard, position ou lien social',
      'design': 'Studio design',
      'livePreview': 'Aperçu direct',
      'savePng': 'Enregistrer PNG',
      'savePdf': 'Exporter PDF',
      'share': 'Partager',
      'scanPrompt': 'Pointez la caméra vers un QR',
      'gallery': 'Galerie',
      'flash': 'Flash',
      'autoOpen': 'Ouvrir les URL après confirmation',
      'systemLanguage': 'La langue suit automatiquement votre appareil',
      'systemTheme': 'Les modes clair et sombre suivent le système',
      'emptyHistory': 'Les codes créés et scannés apparaîtront ici.',
    },
  };

  String t(String key) =>
      _localizedValues[locale.languageCode]?[key] ??
      _localizedValues['en']![key] ??
      key;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) {
    return AppLocalizations.supportedLocales
        .map((supported) => supported.languageCode)
        .contains(locale.languageCode);
  }

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture(AppLocalizations(locale));
  }

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
