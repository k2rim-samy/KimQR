import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/glass_card.dart';

class ScannerScreen extends StatefulWidget {
  const ScannerScreen({super.key});

  @override
  State<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen> {
  final MobileScannerController _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    facing: CameraFacing.back,
    torchEnabled: false,
  );
  String? _lastValue;
  bool _autoOpen = true;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 112),
      children: [
        Text(
          l10n.t('scanner'),
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 18),
        AspectRatio(
          aspectRatio: 1,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(32),
            child: Stack(
              fit: StackFit.expand,
              children: [
                MobileScanner(
                  controller: _controller,
                  onDetect: _handleDetection,
                ),
                const _ScanFrame(),
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Container(
                    margin: const EdgeInsets.all(16),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(.44),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(l10n.t('scanPrompt'), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800)),
                  ),
                ),
              ],
            ),
          ),
        ).animate().fadeIn(duration: 400.ms).scale(begin: const Offset(.96, .96)),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            children: [
              SwitchListTile.adaptive(
                value: _autoOpen,
                onChanged: (value) => setState(() => _autoOpen = value),
                title: Text(l10n.t('autoOpen')),
                contentPadding: EdgeInsets.zero,
              ),
              const Divider(),
              Row(
                children: [
                  Expanded(
                    child: FilledButton.tonalIcon(
                      onPressed: () => _controller.toggleTorch(),
                      icon: const Icon(Icons.flashlight_on),
                      label: Text(l10n.t('flash')),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton.tonalIcon(
                      onPressed: _scanFromGallery,
                      icon: const Icon(Icons.photo_library),
                      label: Text(l10n.t('gallery')),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        if (_lastValue != null) ...[
          const SizedBox(height: 18),
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Result', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
                const SizedBox(height: 8),
                SelectableText(_lastValue!),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Future<void> _handleDetection(BarcodeCapture capture) async {
    final value = capture.barcodes.isEmpty ? null : capture.barcodes.first.rawValue;
    if (value == null || value == _lastValue) return;
    setState(() => _lastValue = value);
    final uri = Uri.tryParse(value);
    final isUrl = uri != null && (uri.scheme == 'http' || uri.scheme == 'https');
    if (!mounted || !_autoOpen || !isUrl) return;
    final open = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Open URL?'),
        content: Text(value),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Open')),
        ],
      ),
    );
    if (open == true) await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _scanFromGallery() async {
    final image = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (image == null) return;
    final capture = await _controller.analyzeImage(image.path);
    final value = capture == null || capture.barcodes.isEmpty ? null : capture.barcodes.first.rawValue;
    if (!mounted) return;
    setState(() => _lastValue = value ?? 'No QR code detected in this image.');
  }
}

class _ScanFrame extends StatelessWidget {
  const _ScanFrame();

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Center(
        child: Container(
          width: 250,
          height: 250,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(32),
            border: Border.all(color: Theme.of(context).colorScheme.secondary, width: 3),
          ),
        ).animate(onPlay: (controller) => controller.repeat(reverse: true)).scaleXY(begin: .96, end: 1, duration: 900.ms),
      ),
    );
  }
}
