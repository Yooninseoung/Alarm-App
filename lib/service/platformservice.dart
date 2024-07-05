import 'package:flutter/services.dart';

class PlatformService {
  static const MethodChannel _channel = MethodChannel('com.flutter_application_1.app/channel');

  static Future<void> onAppStart() async {
    try {
      print("success");
      await _channel.invokeMethod('createAsleepInstance');
    } on PlatformException catch (e) {
      print("Failed to execute method: '${e.message}'.");
    }
  }
}
