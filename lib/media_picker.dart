import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

class MediasPicker {
  static const MethodChannel _channel =
      const MethodChannel('media_picker');

  static Future<List<String>> pickImages({int quantity, int maxWidth, int maxHeight, int quality, bool withVideo}) async {

    if (maxWidth != null && maxWidth < 0) {
      throw new ArgumentError.value(maxWidth, 'maxWidth cannot be negative');
    }

    if (maxHeight != null && maxHeight < 0) {
      throw new ArgumentError.value(maxHeight, 'maxHeight cannot be negative');
    }

    if (quality != null && (quality < 0 || quality > 100)) {
      throw new ArgumentError.value(maxWidth, 'quality cannot be negative and cannot be bigger then 100');
    }

    if (Platform.isAndroid)
      if (!await checkPermission())
        if (!await requestPermission())
          return [];

    var arguments = <String, dynamic>{
      'maxWidth': maxWidth ?? 0,
      'maxHeight': maxHeight ?? 0,
      'quality': quality ?? 100,
    };
    if (quantity != null) {
      arguments['quantity'] = quantity;
    }
    if (withVideo != null) {
      arguments['withVideo'] = withVideo;
    }
    final List<String> docsPaths = await _channel.invokeListMethod<String>('pickImages', arguments);
    return docsPaths;
  }

  static Future<List<String>> pickVideos({int quantity}) async {

    if (Platform.isAndroid)
      if (!await checkPermission())
        if (!await requestPermission())
          return [];

    var arguments = <String, dynamic>{ };
    if (quantity != null) {
      arguments['quantity'] = quantity;
    }

    final List<String> docsPaths = await _channel.invokeListMethod<String>('pickVideos', arguments);
    return docsPaths;
  }

  static Future<List<String>> compressImages({@required List<String> imgPaths, int maxWidth, int maxHeight, int quality}) async {

    if (imgPaths != null && imgPaths.length <= 0) {
      throw new ArgumentError.value(imgPaths, 'imgPaths needs to have 1 or more itens');
    }

    if (maxWidth != null && maxWidth < 0) {
      throw new ArgumentError.value(maxWidth, 'maxWidth cannot be negative');
    }

    if (maxHeight != null && maxHeight < 0) {
      throw new ArgumentError.value(maxHeight, 'maxHeight cannot be negative');
    }

    if (quality != null && (quality < 0 || quality > 100)) {
      throw new ArgumentError.value(quality, 'quality cannot be negative and cannot be bigger then 100');
    }

    if (Platform.isAndroid)
      if (!await checkPermission())
        if (!await requestPermission())
          return [];

    final List<String> docsPaths = await _channel.invokeListMethod<String>('compressImages', <String, dynamic>{
      'imgPaths': imgPaths,
      'maxWidth': maxWidth ?? 0,
      'maxHeight': maxHeight ?? 0,
      'quality': quality ?? 100
    });
    return docsPaths;
  }

  //Just android (storage permission)
  static Future<bool> checkPermission() async {
    return await _channel.invokeMethod("checkPermission");
  }

  //Just android (storage permission)
  static Future<bool> requestPermission() async {
    return await _channel.invokeMethod("requestPermission");
  }

  static Future<bool> deleteAllTempFiles() async {
    final bool deleted = await _channel.invokeMethod('deleteAllTempFiles');
    return deleted;
  }
}
