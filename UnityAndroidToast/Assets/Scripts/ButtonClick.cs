using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using TMPro;

public class ButtonClick : MonoBehaviour
{

    public GameObject Panel;
    public GameObject PP;
    public string Button_name;
    string ss;
    // Start is called before the first frame update
    public void OpenPanel()
    {
        Panel.SetActive(true);
        if (Button_name == "Eye")
        {
            ss = PlayerPrefs.GetString("CurrentText");
        }
        if (Button_name == "Hands")
        {
            ss = PlayerPrefs.GetString("CurrentText2");
        }
        if (Button_name == "Smile")
        {
            ss = PlayerPrefs.GetString("CurrentText3");
            Debug.Log("Add result:" + ss);
        }
        
        PP.GetComponent<TextMeshProUGUI>().text = ss;
    }
}
