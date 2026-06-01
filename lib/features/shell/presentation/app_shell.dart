import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../../core/localization/app_localizations.dart';
import '../../generator/presentation/generator_screen.dart';
import '../../history/presentation/history_screen.dart';
import '../../scanner/presentation/scanner_screen.dart';
import '../../settings/presentation/settings_screen.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _index = 0;

  static const _pages = [
    GeneratorScreen(),
    ScannerScreen(),
    HistoryScreen(),
    SettingsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final labels = [
      l10n.t('generator'),
      l10n.t('scanner'),
      l10n.t('history'),
      l10n.t('settings'),
    ];
    return Scaffold(
      extendBody: true,
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(14),
                gradient: LinearGradient(
                  colors: [
                    Theme.of(context).colorScheme.primary,
                    Theme.of(context).colorScheme.secondary,
                  ],
                ),
              ),
              child: const Icon(Icons.qr_code_2_rounded, color: Colors.white),
            ),
            const SizedBox(width: 12),
            Text(l10n.t('appName')),
          ],
        ),
      ),
      body: Stack(
        children: [
          const _AmbientBackground(),
          SafeArea(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 260),
              switchInCurve: Curves.easeOutCubic,
              child: KeyedSubtree(key: ValueKey(_index), child: _pages[_index]),
            ),
          ),
        ],
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 18),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(28),
          child: NavigationBar(
            selectedIndex: _index,
            onDestinationSelected: (value) => setState(() => _index = value),
            destinations: [
              NavigationDestination(icon: const Icon(Icons.auto_awesome), selectedIcon: const Icon(Icons.auto_awesome), label: labels[0]),
              NavigationDestination(icon: const Icon(Icons.qr_code_scanner), selectedIcon: const Icon(Icons.qr_code_scanner), label: labels[1]),
              NavigationDestination(icon: const Icon(Icons.history), selectedIcon: const Icon(Icons.history), label: labels[2]),
              NavigationDestination(icon: const Icon(Icons.settings), selectedIcon: const Icon(Icons.settings), label: labels[3]),
            ],
          ),
        ),
      ).animate().fadeIn(duration: 450.ms).slideY(begin: .24, end: 0),
    );
  }
}

class _AmbientBackground extends StatelessWidget {
  const _AmbientBackground();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            scheme.primary.withOpacity(.14),
            Theme.of(context).scaffoldBackgroundColor,
            scheme.secondary.withOpacity(.10),
          ],
        ),
      ),
      child: const SizedBox.expand(),
    );
  }
}
