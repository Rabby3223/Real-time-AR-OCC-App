# Real-time-AR-OCC-App
## Description
This project realized an artificial reality (AR) App assisted by optical camera communciation (OCC) system. The text information was embedded in the light from luminaire. Smartphone camera captured the reflected light and recorded signals on the image frame as black and white stripes by employing rolling shutter effect. After that, smartphone recognized Chinese dragon vase and showed virtual buttons on the vase. When clicking the button, text information will show on the smartphone display.  

https://user-images.githubusercontent.com/27682089/179368010-96d6c06a-8073-46fd-b0ed-6131ec615eda.mp4
## Getting Strated
Matlab R2021b Unity 2020.3.32f1  
Android Studio 2020.3.1  
Smartphone OnePlus 5T  
### Matlab (ControlLED for LED transmotter modualtion)
1.	Run the matlab code and generate csv file.
2.	Upload csv file to AWG or FPGA to control the On-Off state of luminaire.
### Android Studio (UnityPluginToast for OCC)
1.	Click File > New > New Module.
2.	Copy the aar file to unity project UnityAndroidToast/Assets/Plugins/Android
### Unity (UnityAndroidToast for AR)
1.	Click File > Build Settings > Player Settings. Enter password 123456.
2.	Click File > Build and Run.
## Authors
Liqiong Liu, Deaprtment of Information Engineering, The Chinese University of Hong Kong
## License
Distributed under the MIT License.
