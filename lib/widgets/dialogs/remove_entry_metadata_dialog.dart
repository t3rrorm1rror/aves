import 'package:aves/model/entry.dart';
import 'package:aves/model/metadata/enums.dart';
import 'package:aves/ref/brand_colors.dart';
import 'package:aves/ref/mime_types.dart';
import 'package:aves/utils/color_utils.dart';
import 'package:aves/widgets/common/extensions/build_context.dart';
import 'package:aves/widgets/common/fx/highlight_decoration.dart';
import 'package:aves/widgets/common/identity/highlight_title.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';

import 'aves_dialog.dart';

class RemoveEntryMetadataDialog extends StatefulWidget {
  final AvesEntry entry;

  const RemoveEntryMetadataDialog({
    Key? key,
    required this.entry,
  }) : super(key: key);

  @override
  _RemoveEntryMetadataDialogState createState() => _RemoveEntryMetadataDialogState();
}

class _RemoveEntryMetadataDialogState extends State<RemoveEntryMetadataDialog> {
  late final List<MetadataType> _mainOptions, _moreOptions;
  final Set<MetadataType> _types = {};
  bool _showMore = false;
  final ValueNotifier<bool> _isValidNotifier = ValueNotifier(false);

  AvesEntry get entry => widget.entry;

  @override
  void initState() {
    super.initState();
    final byMain = groupBy([
      ...MetadataTypes.common,
      if (entry.mimeType == MimeTypes.jpeg) ...MetadataTypes.jpeg,
    ], MetadataTypes.main.contains);
    _mainOptions = (byMain[true] ?? [])..sort(_compareTypeText);
    _moreOptions = (byMain[false] ?? [])..sort(_compareTypeText);
    _validate();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AvesDialog(
      context: context,
      title: l10n.removeEntryMetadataDialogTitle,
      scrollableContent: [
        ..._mainOptions.map(_toTile),
        if (_moreOptions.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 1),
            child: ExpansionPanelList(
              expansionCallback: (index, isExpanded) {
                setState(() => _showMore = !isExpanded);
              },
              expandedHeaderPadding: EdgeInsets.zero,
              elevation: 0,
              children: [
                ExpansionPanel(
                  headerBuilder: (context, isExpanded) => ListTile(
                    title: Text(l10n.removeEntryMetadataDialogMore),
                  ),
                  body: Column(
                    children: _moreOptions.map(_toTile).toList(),
                  ),
                  isExpanded: _showMore,
                  canTapOnHeader: true,
                  backgroundColor: Theme.of(context).dialogBackgroundColor,
                ),
              ],
            ),
          ),
      ],
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(MaterialLocalizations.of(context).cancelButtonLabel),
        ),
        ValueListenableBuilder<bool>(
          valueListenable: _isValidNotifier,
          builder: (context, isValid, child) {
            return TextButton(
              onPressed: isValid ? () => _submit(context) : null,
              child: Text(context.l10n.applyButtonLabel),
            );
          },
        ),
      ],
    );
  }

  int _compareTypeText(MetadataType a, MetadataType b) => a.getText().compareTo(b.getText());

  Widget _toTile(MetadataType type) {
    final text = type.getText();
    return SwitchListTile(
      value: _types.contains(type),
      onChanged: (selected) {
        selected ? _types.add(type) : _types.remove(type);
        _validate();
        setState(() {});
      },
      title: Align(
        alignment: Alignment.centerLeft,
        child: DecoratedBox(
          decoration: HighlightDecoration(
            color: BrandColors.get(text) ?? stringToColor(text),
          ),
          child: Text(
            text,
            style: const TextStyle(
              shadows: HighlightTitle.shadows,
            ),
          ),
        ),
      ),
    );
  }

  void _validate() => _isValidNotifier.value = _types.isNotEmpty;

  void _submit(BuildContext context) => Navigator.pop(context, _types);
}