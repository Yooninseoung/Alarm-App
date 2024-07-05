import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:alarm/alarm.dart';

import 'package:flutter_application_1/screens/main_screen.dart';
import 'package:flutter_application_1/screens/alarm_screen.dart';
import 'package:flutter_application_1/screens/shortcut.dart';
import 'package:flutter_application_1/service/platformservice.dart';

Map<String, MaterialPageRoute<dynamic>?> routes = {
  'ring': null,
};

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);

  await Alarm.init(showDebugLogs: true);
  runApp(MyApp(key: MyApp.globalKey)); // MyApp 위젯에 globalKey 적용

 // runApp(MyApp());
  PlatformService.onAppStart(); //처음 앱 실행 시 호출하는 함수
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);
  static final GlobalKey<_MyAppState> globalKey = GlobalKey<_MyAppState>();


  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int currentIndex = 0;

  final List<Widget> pageList = [MainPage(), AlarmPage()];


  void setTabIndex(int index) {
    setState(() {
      currentIndex = index;
    });
  }


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: false),
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: new Stack(
          children: <Widget>[
            new Offstage(
              offstage: currentIndex != 0,
              child: new TickerMode(enabled: currentIndex == 0, child: new MaterialApp(home: MainPage(), debugShowCheckedModeBanner: false,)),
            ),
            new Offstage(
              offstage: currentIndex != 1,
              child: new TickerMode(enabled: currentIndex == 1, child: new MaterialApp(home: AlarmPage(), debugShowCheckedModeBanner: false,)),
            )
          ]
        ),
        backgroundColor: Color(0xffe9edf3),
        appBar: AppBar(
          elevation: 0,
          centerTitle: true,
          backgroundColor: Colors.grey,
          title: Text(
            "수면 추적",
            style: TextStyle(
              color: Colors.black,
            ),
          ),
        ),
          bottomNavigationBar: BottomNavigationBar(
          backgroundColor: Colors.grey,
          selectedItemColor: Colors.white,
          unselectedItemColor: Colors.black,
          currentIndex: currentIndex,
          onTap: (index) => setState(() => currentIndex = index),
          items: [
            BottomNavigationBarItem(
              icon: Icon(Icons.home),
              label: "수면 기록",
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.alarm),
              label: "알람",
            ),
          ],
        ),
      ),
    );
  }
}
