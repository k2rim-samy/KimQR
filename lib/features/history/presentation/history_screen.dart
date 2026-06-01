import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/glass_card.dart';

class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 112),
      children: [
        Text(l10n.t('history'), style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 18),
        GlassCard(
          child: Column(
            children: [
              Icon(Icons.history_rounded, size: 54, color: Theme.of(context).colorScheme.primary),
              const SizedBox(height: 12),
              Text(l10n.t('emptyHistory'), textAlign: TextAlign.center),
            ],
          ),
        ),
      ],
    );
  }
}
