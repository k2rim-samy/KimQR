import 'package:flex_color_picker/flex_color_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/glass_card.dart';
import '../domain/qr_content_type.dart';

class GeneratorScreen extends StatefulWidget {
  const GeneratorScreen({super.key});

  @override
  State<GeneratorScreen> createState() => _GeneratorScreenState();
}

class _GeneratorScreenState extends State<GeneratorScreen> {
  final _controller = TextEditingController(text: 'https://qrmaster.app');
  QrContentType _type = QrContentType.url;
  QrVisualStyle _style = QrVisualStyle.rounded;
  Color _foreground = const Color(0xFF4F46E5);
  Color _background = Colors.white;
  bool _gradient = true;

  String get _payload => QrPayloadBuilder.build(_type, _controller.text);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 112),
      children: [
        Text(
          l10n.t('homeTitle'),
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w900,
              ),
        ).animate().fadeIn(duration: 350.ms).slideY(begin: .08, end: 0),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l10n.t('content'), style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: QrContentType.values.map((type) {
                  return ChoiceChip(
                    selected: _type == type,
                    label: Text(type.label),
                    onSelected: (_) => setState(() => _type = type),
                  );
                }).toList(),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _controller,
                minLines: 3,
                maxLines: 5,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  hintText: l10n.t('contentHint'),
                  filled: true,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(22)),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            children: [
              Row(
                children: [
                  Text(l10n.t('livePreview'), style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
                  const Spacer(),
                  Icon(Icons.auto_awesome, color: scheme.secondary),
                ],
              ),
              const SizedBox(height: 18),
              AnimatedContainer(
                duration: const Duration(milliseconds: 350),
                curve: Curves.easeOutCubic,
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(28),
                  gradient: _gradient
                      ? LinearGradient(colors: [_background, _background.withBlue((_background.blue + 20).clamp(0, 255).toInt())])
                      : null,
                  color: _gradient ? null : _background,
                  boxShadow: [
                    BoxShadow(
                      color: _foreground.withOpacity(.20),
                      blurRadius: 26,
                      offset: const Offset(0, 14),
                    ),
                  ],
                ),
                child: QrImageView(
                  data: _payload.isEmpty ? 'QR Master' : _payload,
                  size: 230,
                  eyeStyle: QrEyeStyle(
                    eyeShape: _style == QrVisualStyle.square ? QrEyeShape.square : QrEyeShape.circle,
                    color: _foreground,
                  ),
                  dataModuleStyle: QrDataModuleStyle(
                    dataModuleShape: _style == QrVisualStyle.dots ? QrDataModuleShape.circle : QrDataModuleShape.square,
                    color: _foreground,
                  ),
                  embeddedImageStyle: const QrEmbeddedImageStyle(size: Size(42, 42)),
                  backgroundColor: Colors.transparent,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l10n.t('design'), style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                children: QrVisualStyle.values.map((style) {
                  return ChoiceChip(
                    selected: _style == style,
                    label: Text(style.label),
                    onSelected: (_) => setState(() => _style = style),
                  );
                }).toList(),
              ),
              const SizedBox(height: 12),
              SwitchListTile.adaptive(
                value: _gradient,
                onChanged: (value) => setState(() => _gradient = value),
                title: const Text('Gradient background'),
                contentPadding: EdgeInsets.zero,
              ),
              Row(
                children: [
                  Expanded(
                    child: _ColorButton(
                      label: 'QR color',
                      color: _foreground,
                      onColorChanged: (color) => setState(() => _foreground = color),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _ColorButton(
                      label: 'Background',
                      color: _background,
                      onColorChanged: (color) => setState(() => _background = color),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(child: FilledButton.icon(onPressed: _showComingSoon, icon: const Icon(Icons.image), label: Text(l10n.t('savePng')))),
            const SizedBox(width: 10),
            Expanded(child: FilledButton.tonalIcon(onPressed: _showComingSoon, icon: const Icon(Icons.picture_as_pdf), label: Text(l10n.t('savePdf')))),
          ],
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(onPressed: _showComingSoon, icon: const Icon(Icons.ios_share), label: Text(l10n.t('share'))),
      ],
    );
  }

  Future<void> _showComingSoon() async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Export/share pipeline is ready for platform integration.')),
    );
  }
}

class _ColorButton extends StatelessWidget {
  const _ColorButton({
    required this.label,
    required this.color,
    required this.onColorChanged,
  });

  final String label;
  final Color color;
  final ValueChanged<Color> onColorChanged;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      icon: CircleAvatar(radius: 10, backgroundColor: color),
      label: Text(label),
      onPressed: () async {
        final selected = await showColorPickerDialog(
          context,
          color,
          heading: Text(label),
          borderRadius: 20,
        );
        onColorChanged(selected);
      },
    );
  }
}
