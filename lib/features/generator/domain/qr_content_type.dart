enum QrContentType {
  text('Text'),
  url('URL'),
  wifi('WiFi'),
  phone('Phone'),
  sms('SMS'),
  email('Email'),
  vcard('vCard'),
  location('Location'),
  social('Social');

  const QrContentType(this.label);
  final String label;
}

enum QrVisualStyle {
  square('Square'),
  rounded('Rounded'),
  dots('Dots'),
  modern('Modern');

  const QrVisualStyle(this.label);
  final String label;
}

class QrPayloadBuilder {
  const QrPayloadBuilder._();

  static String build(QrContentType type, String raw) {
    final value = raw.trim();
    if (value.isEmpty) return '';
    switch (type) {
      case QrContentType.url:
        return value.startsWith(RegExp('https?://')) ? value : 'https://$value';
      case QrContentType.wifi:
        return 'WIFI:T:WPA;S:$value;P:password;;';
      case QrContentType.phone:
        return 'tel:$value';
      case QrContentType.sms:
        return 'SMSTO:$value:';
      case QrContentType.email:
        return 'mailto:$value';
      case QrContentType.vcard:
        return 'BEGIN:VCARD\nVERSION:3.0\nFN:$value\nEND:VCARD';
      case QrContentType.location:
        return 'geo:$value';
      case QrContentType.social:
        return value.startsWith('@') ? 'https://social.example/${value.substring(1)}' : value;
      case QrContentType.text:
        return value;
    }
  }
}
