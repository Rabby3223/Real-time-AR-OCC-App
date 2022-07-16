using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;

public class ExitClick : MonoBehaviour
{

    public GameObject Panel;
    // Start is called before the first frame update
    public void ClosePanel()
    {
        Panel.SetActive(false);
    }
    public void CloseApplication()
    {
        //UnityEditor.EditorApplication.isPlaying = false;
        //Application.Quit();
        SceneManager.LoadScene("Test");
    }
}
