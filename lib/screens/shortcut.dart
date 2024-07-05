import 'package:alarm/alarm.dart';
import 'package:alarm/model/alarm_settings.dart';
import 'package:flutter/material.dart';

class AlarmHomeShortcutButton extends StatefulWidget {
  final void Function() refreshAlarms;


  const AlarmHomeShortcutButton({Key? key, required this.refreshAlarms})
      : super(key: key);



  @override
  State<AlarmHomeShortcutButton> createState() =>
      _AlarmHomeShortcutButtonState();
}

class _AlarmHomeShortcutButtonState
    extends State<AlarmHomeShortcutButton> {
  bool showMenu = false;

  Future<void> onPressButton(int delayInHours) async {
    DateTime dateTime = DateTime.now().add(Duration(hours: delayInHours));
    double? volume;

    if (delayInHours != 0) {
      dateTime = dateTime.copyWith(second: 0, millisecond: 0);
      volume = 0.5;
    }

    setState(() => showMenu = false);


    DateTime dateTime1 = DateTime.now();

    final alarmSettings = AlarmSettings(
      id: DateTime.now().millisecondsSinceEpoch % 10000,
      dateTime: dateTime1,
      assetAudioPath: '../../assets/marimba.mp3',
      volume: volume,
      notificationTitle: '알람 테스트',
      notificationBody: '테스트',
    );

    await Alarm.set(alarmSettings: alarmSettings);
    widget.refreshAlarms();

  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        GestureDetector(
          onLongPress: () {
            setState(() => showMenu = true);
          },
          child: FloatingActionButton(
            onPressed: () => onPressButton(0),
            backgroundColor: Colors.red,
            heroTag: null,
            child: const Text("테스트", textAlign: TextAlign.center),
          ),
        ),
        if (showMenu)
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextButton(
                onPressed: () => onPressButton(24),
                child: const Text("+24h"),
              ),
              TextButton(
                onPressed: () => onPressButton(36),
                child: const Text("+36h"),
              ),
              TextButton(
                onPressed: () => onPressButton(48),
                child: const Text("+48h"),
              ),
            ],
          ),
      ],
    );
  }
}