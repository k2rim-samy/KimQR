import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/glass_card.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 112),
      children: [
        Text(l10n.t('settings'), style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            children: [
              _SettingsTile(icon: Icons.brightness_auto, title: l10n.t('systemTheme')),
              const Divider(),
              _SettingsTile(icon: Icons.language, title: l10n.t('systemLanguage')),
              const Divider(),
              const _SettingsTile(icon: Icons.security, title: 'Local-first history and privacy-ready architecture'),
              const Divider(),
              const _SettingsTile(icon: Icons.high_quality, title: 'High-quality PNG/PDF export hooks'),
            ],
          ),
        ),
      ],
    );
  }
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: CircleAvatar(
        backgroundColor: Theme.of(context).colorScheme.primary.withOpacity(.12),
        child: Icon(icon, color: Theme.of(context).colorScheme.primary),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
    );
  }
}
