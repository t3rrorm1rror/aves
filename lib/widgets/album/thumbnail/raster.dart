import 'package:aves/model/image_entry.dart';
import 'package:aves/utils/constants.dart';
import 'package:aves/widgets/common/image_providers/thumbnail_provider.dart';
import 'package:aves/widgets/common/image_providers/uri_image_provider.dart';
import 'package:aves/widgets/common/transition_image.dart';
import 'package:flutter/material.dart';

class ThumbnailRasterImage extends StatefulWidget {
  final ImageEntry entry;
  final double extent;
  final ValueNotifier<bool> isScrollingNotifier;
  final Object heroTag;

  const ThumbnailRasterImage({
    Key key,
    @required this.entry,
    @required this.extent,
    this.isScrollingNotifier,
    this.heroTag,
  }) : super(key: key);

  @override
  _ThumbnailRasterImageState createState() => _ThumbnailRasterImageState();
}

class _ThumbnailRasterImageState extends State<ThumbnailRasterImage> {
  ThumbnailProvider _imageProvider;

  ImageEntry get entry => widget.entry;

  double get extent => widget.extent;

  Object get heroTag => widget.heroTag;

  @override
  void initState() {
    super.initState();
    _initProvider();
  }

  @override
  void didUpdateWidget(ThumbnailRasterImage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.entry != entry) {
      _pauseProvider();
      _initProvider();
    }
  }

  @override
  void dispose() {
    _pauseProvider();
    super.dispose();
  }

  void _initProvider() => _imageProvider = ThumbnailProvider(entry: entry, extent: Constants.thumbnailCacheExtent);

  void _pauseProvider() {
    final isScrolling = widget.isScrollingNotifier?.value ?? false;
    // when the user is scrolling faster than we can retrieve the thumbnails,
    // the retrieval task queue can pile up for thumbnails that got disposed
    // in this case we pause the image retrieval task to get it out of the queue
    if (isScrolling) {
      _imageProvider?.pause();
    }
  }

  @override
  Widget build(BuildContext context) {
    final image = Image(
      image: _imageProvider,
      width: extent,
      height: extent,
      fit: BoxFit.cover,
    );
    return heroTag == null
        ? image
        : Hero(
            tag: heroTag,
            flightShuttleBuilder: (flight, animation, direction, fromHero, toHero) {
              ImageProvider heroImageProvider = _imageProvider;
              if (!entry.isVideo && !entry.isSvg) {
                final imageProvider = UriImage(
                  uri: entry.uri,
                  mimeType: entry.mimeType,
                );
                if (imageCache.statusForKey(imageProvider).keepAlive) {
                  heroImageProvider = imageProvider;
                }
              }
              return TransitionImage(
                image: heroImageProvider,
                animation: animation,
              );
            },
            child: image,
          );
  }
}