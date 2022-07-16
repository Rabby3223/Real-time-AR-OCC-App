using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;

public class test : MonoBehaviour
{
    AndroidJavaClass unityClass;
    AndroidJavaObject unityActivity;
    AndroidJavaObject _pluginInstance;
    string str1 = null;
    string str2 = null;
    string str3 = null;
    // Start is called before the first frame update
    void Start()
    {
        initializePlugin("com.example.android2unity.MainActivity");
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    void initializePlugin(string pluginName)
    {
        unityClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        unityActivity = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
        _pluginInstance = new AndroidJavaObject(pluginName);
        if (_pluginInstance == null)
        {
            Debug.Log("Plugin instance error");
        }
        //_pluginInstance.CallStatic("receiveUnityActivity", unityActivity);
        Debug.Log("Plugin");
    }
    public void Add()
    {
        if (_pluginInstance != null)
        {
            var result = _pluginInstance.Call<int>("Add", 5, 6);
            Debug.Log("Add result:"+result);
            PlayerPrefs.SetInt("CurrentLevel", 10);
            PlayerPrefs.SetString("CurrentText", str1);
            Debug.Log("Add result:"+str1);
            PlayerPrefs.Save();
            SceneManager.LoadScene("ARSession");
        }
    }
    public void Toast()
    {
        if (_pluginInstance != null)
        {
            var result = _pluginInstance.Call<int>("ToastToast", 5,unityActivity);
            //var result = _pluginInstance.Call<int>("ToastToast",5);

        }
    }
    public void ReceiveMessage(string kk)
    {
        if (str1 == null)
        {
            str1 = kk;
            Debug.Log("Add result:" + str1);
            PlayerPrefs.SetString("CurrentText", str1);
        }
        else if(str2 == null)
        {
            str2 = kk;
            Debug.Log("Add result:" + str2);
            PlayerPrefs.SetString("CurrentText2", str2);
        }
        else if (str3 == null)
        {
            str3 = kk;
            Debug.Log("Add result:" + str3);
            PlayerPrefs.SetString("CurrentText3", str3);
            PlayerPrefs.Save();
            SceneManager.LoadScene("ARSession");
        }
        

        
    }
}
