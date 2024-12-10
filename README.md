# RemoteAlarm

Android app that can set an alarm and control my ceiling light to wake me up.
The connection is made with BLE to an ESP32 that controls the light with two MOSFETs (for CW and WW).

Uses MVVC and Jetpack Compose.

The C++ code for the ESP32 is in the "light-peripheral" directory.

<div style="display: flex; justify-content: center; gap: 10px;">
  <img src="https://github.com/user-attachments/assets/be07582c-7616-44ba-a395-e2213361a645" alt="Control" width=300/>
  <img src="https://github.com/user-attachments/assets/298f5392-ceb6-4e69-b2b8-fbe392051154" alt="Alarms 1" width=300/>
  <img src="https://github.com/user-attachments/assets/668b2da3-ab08-4eac-9a9f-aba1c4c97c62" alt="Alarms 2" width=300/>
</div>
